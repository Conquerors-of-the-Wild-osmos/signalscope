package ru.itmo.ipkn.signalscope.client.receiver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReceiverRendererTest {
    @Test
    void compassPixelsAndSmoothMarker() {
        double threshold = Math.pow(Math.cos(Math.toRadians(45)), 2);
        int north = ReceiverRenderer.compassPixel(7, 3, 0, 1, threshold);
        int south = ReceiverRenderer.compassPixel(7, 12, 0, 1, threshold);
        assertEquals(0xFF52D9AF, north);
        assertEquals(0xFF183F3D, south);
        int edge = ReceiverRenderer.compassPixel(7, 1, 0, 1, threshold) >>> 24;
        assertTrue(edge > 0 && edge < 255);
        assertEquals(0, ReceiverRenderer.compassPixel(0, 0, 0, 1, threshold));
        assertEquals(2f / 16, ReceiverRenderer.markerY(-90));
        assertEquals(13f / 16, ReceiverRenderer.markerY(90));
        assertTrue(ReceiverRenderer.markerY(1) > ReceiverRenderer.markerY(0));
        assertTrue(ReceiverRenderer.markerY(1) - ReceiverRenderer.markerY(0) < 1f / 16);
    }
}
