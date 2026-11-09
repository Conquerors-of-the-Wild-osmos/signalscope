package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import ru.itmo.ipkn.signalscope.item.SignalscopeItems;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;

import java.util.List;

/** Accessed only on the client thread, including Fabric's payload callback. */
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
        players = payload.players();
        level = client.level;
        receivedAt = System.nanoTime();
    }

    public void tick(Minecraft client) {
        if (client.level != level || client.player == null || !client.player.isAlive()
                || !SignalscopeItems.holdsSignalscope(client.player)
                || System.nanoTime() - receivedAt > 2_000_000_000L) {
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
