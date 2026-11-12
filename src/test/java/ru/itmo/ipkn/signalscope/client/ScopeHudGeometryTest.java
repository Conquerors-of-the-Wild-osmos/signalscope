package ru.itmo.ipkn.signalscope.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeHudGeometryTest {
    @Test
    void shapesChangeInFiveStepsWhileSeparationMovesContinuously() {
        assertEquals(0, ScopeHudGeometry.state(20, 20, 60));
        for (int state = 1; state <= 4; state++) {
            double start = 20 + (state - 1) * 10;
            assertEquals(state, ScopeHudGeometry.state(start + 0.001, 20, 60));
            assertEquals(state, ScopeHudGeometry.state(start + 10, 20, 60));
            float before = ScopeHudGeometry.separation(start + 5, 640, 20, 60);
            float after = ScopeHudGeometry.separation(start + 5.1, 640, 20, 60);
            assertTrue(after > before);
            assertEquals(0.27F, after - before, 1.0e-4);
            if (state > 1) {
                float left = ScopeHudGeometry.separation(start - 0.001, 640, 20, 60);
                float right = ScopeHudGeometry.separation(start + 0.001, 640, 20, 60);
                assertTrue(right - left < 0.01F);
            }
        }
        for (int state = 0; state <= 4; state++) {
            boolean body = false;
            boolean outline = false;
            boolean antialiased = false;
            for (int row = 0; row < ScopeHudGeometry.HEIGHT; row++) {
                for (int col = 0; col < ScopeHudGeometry.WIDTH; col++) {
                    int pixel = ScopeHudGeometry.pixelColor(state, row, col, 0xFFE85858);
                    assertEquals(pixel, ScopeHudGeometry.pixelColor(state, ScopeHudGeometry.HEIGHT - 1 - row, col, 0xFFE85858));
                    body |= pixel == 0xFFE85858;
                    if (pixel != 0 && (pixel & 0xFFFFFF) == 0x742C2C) {
                        outline = true;
                        int greenPixel = ScopeHudGeometry.pixelColor(state, row, col, 0xFF60E860);
                        assertEquals(0x307430, greenPixel & 0xFFFFFF);
                    }
                    antialiased |= pixel != 0 && pixel >>> 24 < 255;
                    int translucent = ScopeHudGeometry.pixelColor(state, row, col, 0x80E85858);
                    assertEquals((pixel >>> 24) * 128 / 255, translucent >>> 24, 1);
                }
            }
            assertTrue(body && outline && antialiased, "state " + state);
        }
        assertTrue(ScopeHudGeometry.isVisible(60, 60));
        assertFalse(ScopeHudGeometry.isVisible(60.001, 60));
    }

    @Test
    void hudSizeAndSpreadAreIndependentOfVanillaGuiScale() {
        for (int width : new int[]{854, 1920, 3840}) {
            for (int percent : new int[]{50, 100, 115, 200}) {
                float baseScale = ScopeHudGeometry.hudScale(1, percent);
                float reference = ScopeHudGeometry.separation(60, width / baseScale, 20, 60);
                for (int guiScale : new int[]{1, 2, 3, 4, 6}) {
                    float scale = ScopeHudGeometry.hudScale(guiScale, percent);
                    assertEquals(reference, ScopeHudGeometry.separation(60, (float) width / guiScale / scale, 20, 60), 1.0e-4);
                    assertEquals(baseScale, scale * guiScale, 1.0e-6);
                }
            }
        }
        assertEquals(1.0F, ScopeHudGeometry.hudScale(5, 100), 1.0e-6);
        assertEquals(319.5F, ScopeHudGeometry.crosshairCenter(640));
        assertEquals(320.5F, ScopeHudGeometry.crosshairCenter(641));
    }
}
