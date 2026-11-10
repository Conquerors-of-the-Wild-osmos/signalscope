package ru.itmo.ipkn.signalscope.client;

import net.minecraft.world.phys.Vec3;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-only presentation state with interpolated signal positions.
 */
final class SignalInterpolator {

    private static final long TRANSITION_NANOS = 250_000_000L;
    private static final double SNAP_DISTANCE_SQUARED = 64 * 64;

    private Map<UUID, Track> tracks = new LinkedHashMap<>();

    // Full snapshot: replace the entire set of tracks.
    void accept(List<Signal> signals, long now) {
        Map<UUID, Track> next = new LinkedHashMap<>();
        for (Signal signal : signals) {
            next.put(signal.id(), createTrack(signal, now));
        }

        tracks = next;
    }

    // Delta: update only changed signals and remove departed players.
    void acceptDelta(List<Signal> updated, List<UUID> removed, long now) {
        for (UUID id : removed) {
            tracks.remove(id);
        }

        for (Signal signal : updated) {
            tracks.put(signal.id(), createTrack(signal, now));
        }
    }

    private Track createTrack(Signal signal, long now) {
        Track previous = tracks.get(signal.id());
        Vec3 from = signal.position();
        if (previous != null
                && previous.target().position().distanceToSqr(signal.position())
                <= SNAP_DISTANCE_SQUARED) {
            // Continue from the currently displayed position.
            from = previous.positionAt(now);
        }

        return new Track(signal, from, now);
    }

    List<Signal> sample(long now) {
        return tracks.values().stream()
                .map(track -> new Signal(track.target().id(), track.target().name(), track.positionAt(now),
                        track.target().transmitting()))
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