package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * Accessed only on the client thread, including Fabric's payload callback.
 */
public final class ClientSignals {

    private List<PlayerSignalsPayload.Signal> players = List.of();
    private ClientLevel level;
    private long receivedAt;

    public void accept(PlayerSignalsPayload payload, Minecraft client) {
        if (client.level == null || client.player == null
                || !SignalscopeItems.holdsSignalscope(client.player)
                || !payload.dimension().equals(client.level.dimension().location())) {
            clear();
            return;
        }

        // Ignore delta packets until a full snapshot has been received.
        if (!payload.fullSnapshot() && level != client.level) {
            return;
        }

        if (payload.fullSnapshot()) {
            // A full snapshot replaces everything.
            players = payload.players();
        } else {
            // Apply only changed and removed players.
            var updated = new LinkedHashMap<UUID, PlayerSignalsPayload.Signal>();

            for (var player : players) {
                updated.put(player.id(), player);
            }

            for (UUID id : payload.removed()) {
                updated.remove(id);
            }

            for (var player : payload.players()) {
                updated.put(player.id(), player);
            }

            players = List.copyOf(updated.values());
        }

        level = client.level;
        receivedAt = System.nanoTime();
    }

    public void tick(Minecraft client) {
        if (client.level != level || client.player == null || !client.player.isAlive()
                || !SignalscopeItems.holdsSignalscope(client.player)
                || System.nanoTime() - receivedAt > 15_000_000_000L) {
            clear();
        }
    }

    public List<PlayerSignalsPayload.Signal> players() {
        return players;
    }

    public void clear() {
        players = List.of();
        level = null;
        receivedAt = 0;
    }
}
