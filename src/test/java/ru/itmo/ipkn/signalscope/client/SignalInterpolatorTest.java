package ru.itmo.ipkn.signalscope.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import ru.itmo.ipkn.signalscope.network.PlayerSignalsPayload.Signal;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalInterpolatorTest {
    private static final long MS = 1_000_000L;
    private final UUID player = UUID.randomUUID();
    private final SignalInterpolator interpolator = new SignalInterpolator();

    @Test
    void firstSightingUsesActualCoordinatesWithoutFlyingFromOrigin() {
        Signal signal = signal(new Vec3(30_000, 80, -20_000));
        interpolator.accept(List.of(signal), 0);
        assertEquals(List.of(signal), interpolator.sample(0));
    }

    @Test
    void interpolatesAllCoordinatesAndStopsAtTheLatestSnapshot() {
        interpolator.accept(List.of(signal(new Vec3(0, 64, 0))), 0);
        interpolator.accept(List.of(signal(new Vec3(10, 70, -4))), 250 * MS);
        assertPosition(new Vec3(0, 64, 0), 250);
        assertPosition(new Vec3(5, 67, -2), 375);
        assertPosition(new Vec3(10, 70, -4), 500);
        assertPosition(new Vec3(10, 70, -4), 1_500);
    }

    @Test
    void earlyPacketRetargetsWithoutJumpingToThePreviousEndpoint() {
        interpolator.accept(List.of(signal(Vec3.ZERO)), 0);
        interpolator.accept(List.of(signal(new Vec3(10, 0, 0))), 250 * MS);
        Vec3 before = interpolator.sample(350 * MS).getFirst().position();
        interpolator.accept(List.of(signal(new Vec3(20, 0, 0))), 350 * MS);
        assertPosition(before, 350);
        assertPosition(new Vec3(12, 0, 0), 475);
        assertPosition(new Vec3(20, 0, 0), 600);
    }

    @Test
    void renderingMoreFramesDoesNotChangeTheResult() {
        interpolator.accept(List.of(signal(Vec3.ZERO)), 0);
        interpolator.accept(List.of(signal(new Vec3(10, 0, 0))), 250 * MS);
        var expected = interpolator.sample(400 * MS);
        for (int ms = 250; ms < 400; ms++) {
            interpolator.sample(ms * MS);
        }
        assertEquals(expected, interpolator.sample(400 * MS));
    }

    @Test
    void tracksPlayersByIdWhenSnapshotOrderChanges() {
        UUID other = UUID.randomUUID();
        interpolator.accept(List.of(signal(Vec3.ZERO), new Signal(other, "Other", new Vec3(100, 0, 0))), 0);
        interpolator.accept(List.of(new Signal(other, "Other", new Vec3(110, 0, 0)),
                signal(new Vec3(10, 0, 0))), 250 * MS);
        var sampled = interpolator.sample(375 * MS);
        assertEquals(other, sampled.get(0).id());
        assertEquals(new Vec3(105, 0, 0), sampled.get(0).position());
        assertEquals(player, sampled.get(1).id());
        assertEquals(new Vec3(5, 0, 0), sampled.get(1).position());
    }

    @Test
    void removesDepartedPlayersAndResetsTheirHistoryOnReturn() {
        interpolator.accept(List.of(signal(Vec3.ZERO)), 0);
        interpolator.accept(List.of(), 250 * MS);
        assertTrue(interpolator.sample(250 * MS).isEmpty());
        interpolator.accept(List.of(signal(new Vec3(10, 0, 0))), 500 * MS);
        assertPosition(new Vec3(10, 0, 0), 500);
    }

    @Test
    void clearDiscardsHistoryBetweenWorldsOrSessions() {
        interpolator.accept(List.of(signal(Vec3.ZERO)), 0);
        interpolator.clear();
        assertTrue(interpolator.sample(0).isEmpty());
        interpolator.accept(List.of(signal(new Vec3(10, 0, 0))), 250 * MS);
        assertPosition(new Vec3(10, 0, 0), 250);
    }

    @Test
    void snapsLargeDiscontinuitiesInsteadOfAnimatingThroughTheWorld() {
        interpolator.accept(List.of(signal(Vec3.ZERO)), 0);
        interpolator.accept(List.of(signal(new Vec3(10_000, 80, 0))), 250 * MS);
        assertPosition(new Vec3(10_000, 80, 0), 250);
    }

    private Signal signal(Vec3 position) {
        return new Signal(player, "Explorer", position);
    }

    private void assertPosition(Vec3 expected, long millis) {
        Vec3 actual = interpolator.sample(millis * MS).getFirst().position();
        assertEquals(expected.x, actual.x, 1.0e-9);
        assertEquals(expected.y, actual.y, 1.0e-9);
        assertEquals(expected.z, actual.z, 1.0e-9);
    }
}
