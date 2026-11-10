package ru.itmo.ipkn.signalscope.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;
import ru.itmo.ipkn.signalscope.voice.RadioState;

import java.util.HashMap;
import java.util.UUID;

public final class SignalscopeServer {

    private SignalscopeServer() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(SignalscopeServer::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RadioState.clearServer());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var players = new HashMap<>(RadioState.serverPlayers());
            players.remove(handler.player.getUUID());
            RadioState.publish(players);
        });
    }

    private static void tick(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        var radioPlayers = new HashMap<UUID, RadioState.Player>();
        for (var player : players) {
            radioPlayers.put(player.getUUID(), new RadioState.Player(player.level().dimension().location().toString(),
                    !player.isSpectator() && SignalscopeItems.isTransmitting(player)));
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
}
