package ru.itmo.ipkn.signalscope.signal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalGeometryTest {
    @Test
    void gainFallsWithinFirstThirdOfReceptionRange() {
        assertEquals(1, SignalGeometry.gain(0, 10, 100));
        assertEquals(1, SignalGeometry.gain(10, 10, 100));
        assertEquals(0.25, SignalGeometry.gain(25, 10, 100));
        assertEquals(0, SignalGeometry.gain(40, 10, 100));
        assertEquals(0, SignalGeometry.gain(70, 10, 100));
        assertEquals(0, SignalGeometry.gain(100, 10, 100));
    }

    @Test
    void distinguishesAheadBehindAndAbove() {
        assertEquals(0, SignalGeometry.angleDegrees(0, 0, 1, 0, 0, 10), 1.0e-6);
        assertEquals(180, SignalGeometry.angleDegrees(0, 0, 1, 0, 0, -10), 1.0e-6);
        assertEquals(90, SignalGeometry.angleDegrees(0, 0, 1, 0, 10, 0), 1.0e-6);
        assertEquals(45, SignalGeometry.angleDegrees(0, 0, 1, 0, 10, 10), 1.0e-6);
    }

    @Test
    void directionDoesNotDependOnDistanceOrVectorMagnitude() {
        double near = SignalGeometry.angleDegrees(0, 0, 1, 3, 4, 5);
        double far = SignalGeometry.angleDegrees(0, 0, 2, 30_000, 40_000, 50_000);
        assertEquals(near, far, 1.0e-6);
    }

    @Test
    void handlesOverlappingPositionsAndRoundingWithoutNaN() {
        assertEquals(0, SignalGeometry.angleDegrees(0, 0, 1, 0, 0, 0));
        double angle = SignalGeometry.angleDegrees(1, 1, 1, 1, 1, 1);
        assertTrue(Double.isFinite(angle));
        assertEquals(0, angle, 1.0e-5);
    }
}
