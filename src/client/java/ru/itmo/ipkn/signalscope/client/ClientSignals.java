package ru.itmo.ipkn.signalscope.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Accessed only on the client thread, including Fabric's payload callback.
 */
public final class ClientSignals {

    private static final long STALE_NANOS = 15_000_000_000L;

    private final SignalInterpolator interpolator = new SignalInterpolator();
    private final EmptySignalNotice emptyNotice = new EmptySignalNotice();

    private final Map<UUID, PlayerSignalsPayload.Signal> signals =
            new LinkedHashMap<>();

    private ClientLevel level;
    private long receivedAt;
    private boolean hasSnapshot;

    public void accept(PlayerSignalsPayload payload, Minecraft client) {
        if (client.level == null || client.player == null
                || !payload.dimension().equals(client.level.dimension().location())) {
            clear();
            return;
        }

        long now = System.nanoTime();

        if (client.level != level
                || (receivedAt != 0 && now - receivedAt > STALE_NANOS)) {
            clear();
        }

        // A delta cannot initialize client state.
        if (!payload.fullSnapshot() && !hasSnapshot) {
            return;
        }

        if (payload.fullSnapshot()) {
            signals.clear();

            for (var signal : payload.players()) {
                signals.put(signal.id(), signal);
            }

            interpolator.accept(List.copyOf(signals.values()), now);
        } else {
            for (UUID id : payload.removed()) {
                signals.remove(id);
            }

            for (var signal : payload.players()) {
                signals.put(signal.id(), signal);
            }

            interpolator.acceptDelta(
                    payload.players(),
                    payload.removed(),
                    now
            );
        }

        emptyNotice.accept(signals.isEmpty(), now);

        level = client.level;
        receivedAt = now;
        hasSnapshot = true;
    }

    public void tick(Minecraft client) {
        if (client.level != level || client.player == null || !client.player.isAlive()
                || (receivedAt != 0 && System.nanoTime() - receivedAt > STALE_NANOS)) {
            clear();
        }
    }

    public List<PlayerSignalsPayload.Signal> players() {
        return interpolator.sample(System.nanoTime());
    }

    public boolean shouldShowEmptyNotice() {
        return emptyNotice.isVisible(System.nanoTime());
    }

    public void clear() {
        signals.clear();
        interpolator.clear();
        emptyNotice.clear();

        level = null;
        receivedAt = 0;
        hasSnapshot = false;
    }
}