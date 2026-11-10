package ru.itmo.ipkn.signalscope.client;

import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-only presentation state; the server still sends one snapshot every five ticks. */
final class SignalInterpolator {
    private static final long TRANSITION_NANOS = 250_000_000L;
    private static final double SNAP_DISTANCE_SQUARED = 64 * 64;

    private Map<UUID, Track> tracks = new LinkedHashMap<>();

    void accept(List<Signal> signals, long now) {
        Map<UUID, Track> next = new LinkedHashMap<>();
        for (Signal signal : signals) {
            Track previous = tracks.get(signal.id());
            Vec3 from = signal.position();
            if (previous != null
                    && previous.target().position().distanceToSqr(signal.position()) <= SNAP_DISTANCE_SQUARED) {
                // Retarget from the currently displayed position, even if the previous transition
                // has not finished. Early/jittery packets must not introduce a position jump.
                from = previous.positionAt(now);
            }
            next.put(signal.id(), new Track(signal, from, now));
        }
        // Full snapshots also remove players that left; never animate a departed target.
        tracks = next;
    }

    List<Signal> sample(long now) {
        return tracks.values().stream()
                .map(track -> new Signal(track.target().id(), track.target().name(), track.positionAt(now)))
                .toList();
    }

    void clear() {
        tracks.clear();
    }

    private record Track(Signal target, Vec3 from, long startedAt) {
        Vec3 positionAt(long now) {
            double progress = Math.clamp((double) (now - startedAt) / TRANSITION_NANOS, 0.0, 1.0);
            // Stop at the last known position if packets are delayed; do not predict movement.
            return from.lerp(target.position(), progress);
        }
    }
}
