package ru.itmo.ipkn.signalscope.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import ru.itmo.ipkn.signalscope.Signalscope;
import ru.itmo.ipkn.signalscope.block.ReceiverBlocks;
import ru.itmo.ipkn.signalscope.client.receiver.ReceiverRenderer;
import ru.itmo.ipkn.signalscope.client.receiver.ReceiverScreen;
import ru.itmo.ipkn.signalscope.client.receiver.TransmitterScreen;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.menu.ReceiverMenus;
import ru.itmo.ipkn.signalscope.menu.TransmitterMenu;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.network.RadioControlPayload;
import ru.itmo.ipkn.signalscope.voice.RadioState;
import ru.itmo.ipkn.signalscope.voice.RadioVoicechatPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignalscopeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(ReceiverBlocks.RECEIVER_ENTITY, ReceiverRenderer::new);
        MenuScreens.register(ReceiverMenus.RECEIVER, ReceiverScreen::new);
        MenuScreens.register(TransmitterMenu.TYPE, TransmitterScreen::new);
        var signals = new ClientSignals();
        var radio = new RadioController();
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var client = Minecraft.getInstance();
            if (world.isClientSide && player == client.player && !player.isSpectator()
                    && SignalscopeConfig.get().toggleRadio && SignalscopeItems.holdsRadio(player, hand)) {
                radio.input.used(hand);
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        var radioSounds = new RadioSounds(signals);
        var signalBlockerRenderer = new SignalBlockerRenderer();
        signalBlockerRenderer.initialize();
        ItemProperties.register(SignalscopeItems.RADIO, Signalscope.id("transmitting"),
                (stack, level, entity, seed) -> {
                    // Only held radios animate; dropped items and spare inventory stacks stay still.
                    if (entity == null || !entity.isAlive()
                            || (entity.getMainHandItem() != stack && entity.getOffhandItem() != stack)) {
                        return 0F;
                    }
                    if (entity == Minecraft.getInstance().player) {
                        return radio.isTransmitting(stack) ? 1F : 0F;
                    }
                    // Server snapshots include transmission state for both hold and toggle modes.
                    return signals.isTransmitting(entity.getUUID()) ? 1F : 0F;
                });
        ClientPlayNetworking.registerGlobalReceiver(PlayerSignalsPayload.TYPE,
                (payload, context) -> signals.accept(payload, context.client()));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            signals.clear();
            RadioState.setClientGains(Map.of());
            radio.reset();
            radioSounds.clear(client);
            signalBlockerRenderer.clear();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            signals.clear();
            RadioState.setClientGains(Map.of());
            radio.reset();
            radioSounds.clear(client);
            signalBlockerRenderer.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            radio.tick(client);
            signals.tick(client);
            var gains = signals.gains(client);
            var channelGains = new HashMap<UUID, Float>();
            gains.forEach((id, gain) -> channelGains.put(RadioVoicechatPlugin.channelId(id), gain));
            RadioState.setClientGains(channelGains);
            radioSounds.tick(client, gains);
        });
        HudRenderCallback.EVENT.register(new SignalscopeHud(signals)::render);
    }

    private static final class RadioController {
        private static final int HEARTBEAT_TICKS = 10;
        private final RadioToggleInput input = new RadioToggleInput();
        private boolean transmitting;
        private InteractionHand hand;
        private ItemStack stack;
        private int selectedSlot;
        private int tick;
        private int nextHeartbeat;

        boolean isTransmitting(ItemStack candidate) {
            if (!RadioState.isClientTransmitting()) return false;
            if (SignalscopeConfig.get().toggleRadio) {
                var player = Minecraft.getInstance().player;
                return transmitting && player != null && hand != null && candidate == player.getItemInHand(hand);
            }
            var player = Minecraft.getInstance().player;
            return player != null && player.isUsingItem() && player.getUseItem() == candidate;
        }

        void tick(Minecraft client) {
            tick++;
            boolean useDown = client.options.keyUse.isDown();
            InteractionHand usedHand = input.tick(useDown);
            var player = client.player;
            if (!SignalscopeConfig.get().toggleRadio) {
                stop(client);
                RadioState.setClientTransmitting(player != null && client.screen == null && !client.isPaused()
                        && useDown && SignalscopeItems.isTransmitting(player));
                return;
            }

            if (player == null || !player.isAlive() || player.isSpectator()
                    || (hand != null && ((hand == InteractionHand.MAIN_HAND && selectedSlot != player.getInventory().selected)
                    || !ItemStack.isSameItemSameComponents(player.getItemInHand(hand), stack)
                    || !SignalscopeItems.holdsRadio(player, hand)))) {
                stop(client);
            }
            if (player != null && player.isAlive() && !player.isSpectator() && hand == null) {
                for (InteractionHand candidate : InteractionHand.values()) {
                    if (SignalscopeItems.holdsRadio(player, candidate)) {
                        hand = candidate;
                        stack = player.getItemInHand(hand).copy();
                        selectedSlot = player.getInventory().selected;
                        send(client, true);
                        break;
                    }
                }
            }
            if (player != null && client.screen == null && !client.isPaused() && usedHand != null
                    && SignalscopeItems.holdsRadio(player, usedHand) && SignalscopeItems.isTransmitting(player)) {
                if (hand != usedHand) {
                    stop(client);
                    hand = usedHand;
                    stack = player.getItemInHand(hand).copy();
                    selectedSlot = player.getInventory().selected;
                }
                transmitting = !transmitting;
                send(client, true);
            }
            if (hand != null && tick >= nextHeartbeat) {
                send(client, true);
            }
            RadioState.setClientTransmitting(transmitting && !client.isPaused());
        }

        void reset() {
            transmitting = false;
            hand = null;
            stack = null;
            nextHeartbeat = 0;
            input.reset();
            RadioState.setClientTransmitting(false);
        }

        private void stop(Minecraft client) {
            if (hand != null) send(client, false);
            transmitting = false;
            hand = null;
            stack = null;
            nextHeartbeat = 0;
        }

        private void send(Minecraft client, boolean toggleMode) {
            if (hand != null && ClientPlayNetworking.canSend(RadioControlPayload.TYPE)) {
                ClientPlayNetworking.send(new RadioControlPayload(toggleMode, transmitting, hand));
                nextHeartbeat = tick + HEARTBEAT_TICKS;
            }
        }
    }
}
