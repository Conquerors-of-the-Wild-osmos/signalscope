package ru.itmo.ipkn.signalscope.signal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalScreenGeometryTest {

    private boolean blocks(double x1, double y1, double z1, double x2, double y2, double z2, int power) {
        return SignalScreenGeometry.blocks(x1, y1, z1, x2, y2, z2, 0, 0, 0, power);
    }

    @Test
    void screenBoundaryBlocksInBothDirectionsAndAcrossTheCube() {
        assertTrue(blocks(0.5, 0.5, 0.5, 5, 0.5, 0.5, 2));
        assertTrue(blocks(5, 0.5, 0.5, 0.5, 0.5, 0.5, 2));
        assertTrue(blocks(-5, 0.5, 0.5, 5, 0.5, 0.5, 2));
        assertFalse(blocks(0, 0, 0, 1, 1, 1, 2));
        assertFalse(blocks(-5, 3, 0.5, 5, 3, 0.5, 2));
        assertFalse(blocks(-5, 0.5, 0.5, 5, 0.5, 0.5, 0));
    }

    @Test
    void powerDeterminesOddSideLength() {
        assertFalse(blocks(-2, 1.5, 0.5, 2, 1.5, 0.5, 1));
        assertTrue(blocks(-2, 1.5, 0.5, 3, 1.5, 0.5, 2));
        assertFalse(blocks(-3, 2.1, 0.5, 3, 2.1, 0.5, 2));
        assertTrue(blocks(-3, 2.1, 0.5, 4, 2.1, 0.5, 3));
    }
}
