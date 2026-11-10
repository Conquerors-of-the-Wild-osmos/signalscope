package ru.itmo.ipkn.signalscope.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.network.RadioControlPayload;
import ru.itmo.ipkn.signalscope.voice.RadioState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SignalscopeServer {

    private static final int TOGGLE_TIMEOUT_TICKS = 30;
    private static final Map<UUID, ToggleRequest> toggleRequests = new HashMap<>();

    private SignalscopeServer() {
    }

    public static void initialize() {
        ServerPlayNetworking.registerGlobalReceiver(RadioControlPayload.TYPE,
                (payload, context) -> setToggle(context.player().getUUID(), payload, context.player(), context.server()));
        ServerTickEvents.END_SERVER_TICK.register(SignalscopeServer::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            toggleRequests.remove(handler.player.getUUID());
            var players = new HashMap<>(RadioState.serverPlayers());
            players.remove(handler.player.getUUID());
            RadioState.publish(players);
        });
    }

    private static void setToggle(UUID id, RadioControlPayload payload,
                                  net.minecraft.server.level.ServerPlayer player, MinecraftServer server) {
        if (!payload.transmitting()) {
            toggleRequests.remove(id);
        } else if (!player.isSpectator() && player.isAlive() && SignalscopeItems.holdsRadio(player, payload.hand())) {
            toggleRequests.put(id, new ToggleRequest(payload.hand(), player.getItemInHand(payload.hand()),
                    server.getTickCount() + TOGGLE_TIMEOUT_TICKS));
        }
    }

    private static void tick(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        var radioPlayers = new HashMap<UUID, RadioState.Player>();
        for (var player : players) {
            ToggleRequest toggle = toggleRequests.get(player.getUUID());
            boolean toggleTransmitting = toggle != null && toggle.expiresAt() >= server.getTickCount()
                    && !player.isSpectator() && player.isAlive()
                    && player.getItemInHand(toggle.hand()) == toggle.stack()
                    && SignalscopeItems.holdsRadio(player, toggle.hand());
            if (!toggleTransmitting) {
                toggleRequests.remove(player.getUUID());
            }
            radioPlayers.put(player.getUUID(), new RadioState.Player(player.level().dimension().location().toString(),
                    !player.isSpectator() && (SignalscopeItems.isTransmitting(player) || toggleTransmitting)));
        }
        RadioState.publish(radioPlayers);

        // Coordinates are independent of entity tracking distance and of the audio transport.
        if (server.getTickCount() % 5 != 0) {
            return;
        }
        for (var receiver : players) {
            if (!SignalscopeItems.holdsSignalscope(receiver)
                    || !ServerPlayNetworking.canSend(receiver, PlayerSignalsPayload.TYPE)) {
                continue;
            }
            var signals = players.stream()
                    .filter(player -> player != receiver && player.level() == receiver.level())
                    .filter(player -> player.isAlive() && !player.isSpectator())
                    .map(player -> new PlayerSignalsPayload.Signal(player.getUUID(), player.getGameProfile().getName(),
                            player.getBoundingBox().getCenter()))
                    .toList();
            ServerPlayNetworking.send(receiver,
                    new PlayerSignalsPayload(receiver.level().dimension().location(), signals));
        }
    }

    private static void clear() {
        toggleRequests.clear();
        RadioState.clearServer();
    }

    private record ToggleRequest(net.minecraft.world.InteractionHand hand,
                                 net.minecraft.world.item.ItemStack stack, int expiresAt) {
    }
}
