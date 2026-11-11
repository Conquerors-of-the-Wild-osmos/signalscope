package ru.itmo.ipkn.signalscope.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import ru.itmo.ipkn.signalscope.Signalscope;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
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
        var signals = new ClientSignals();
        var radio = new RadioController();
        var radioSounds = new RadioSounds(signals);
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
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            signals.clear();
            RadioState.setClientGains(Map.of());
            radio.reset();
            radioSounds.clear(client);
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
        private boolean transmitting;
        private boolean useWasDown;
        private InteractionHand hand;
        private ItemStack stack;
        private int tick;
        private int nextHeartbeat;

        boolean isTransmitting(ItemStack candidate) {
            if (!RadioState.isClientTransmitting()) return false;
            if (transmitting) return candidate == stack;
            var player = Minecraft.getInstance().player;
            return player != null && player.isUsingItem() && player.getUseItem() == candidate;
        }

        void tick(Minecraft client) {
            tick++;
            boolean useDown = client.options.keyUse.isDown();
            var player = client.player;
            if (!SignalscopeConfig.get().toggleRadio) {
                stop(client);
                RadioState.setClientTransmitting(player != null && client.screen == null && !client.isPaused()
                        && useDown && SignalscopeItems.isTransmitting(player));
                useWasDown = useDown;
                return;
            }

            if (player == null || !player.isAlive() || hand == null || player.getItemInHand(hand) != stack
                    || !SignalscopeItems.holdsRadio(player, hand)) {
                stop(client);
            }
            if (player != null && client.screen == null && !client.isPaused() && useDown && !useWasDown
                    && SignalscopeItems.isTransmitting(player)) {
                if (transmitting) {
                    stop(client);
                } else {
                    hand = player.getUsedItemHand();
                    stack = player.getItemInHand(hand);
                    transmitting = true;
                    send(client);
                }
            }
            useWasDown = useDown;
            if (transmitting && tick >= nextHeartbeat) {
                send(client);
            }
            RadioState.setClientTransmitting(transmitting && !client.isPaused());
        }

        void reset() {
            transmitting = false;
            hand = null;
            stack = null;
            nextHeartbeat = 0;
            RadioState.setClientTransmitting(false);
        }

        private void stop(Minecraft client) {
            if (transmitting) {
                transmitting = false;
                send(client);
            }
            hand = null;
            stack = null;
            nextHeartbeat = 0;
        }

        private void send(Minecraft client) {
            if (hand != null && ClientPlayNetworking.canSend(RadioControlPayload.TYPE)) {
                ClientPlayNetworking.send(new RadioControlPayload(transmitting, hand));
                nextHeartbeat = tick + HEARTBEAT_TICKS;
            }
        }
    }
}
