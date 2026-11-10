package ru.itmo.ipkn.signalscope.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScopeHudGeometryTest {
    @Test
    void hudSizeAndArcSpreadInPixelsAreIndependentOfVanillaGuiScale() {
        for (int width : new int[]{854, 1920, 3840}) {
            for (int percent : new int[]{50, 100, 115, 200}) {
                float baseScale = ScopeHudGeometry.hudScale(1, percent);
                var reference = ScopeHudGeometry.arc(90, width / baseScale, 5, 90);
                for (int guiScale : new int[]{1, 2, 3, 4, 6}) {
                    float scale = ScopeHudGeometry.hudScale(guiScale, percent);
                    var arc = ScopeHudGeometry.arc(90, (float) width / guiScale / scale, 5, 90);
                    assertEquals(reference.separation() * baseScale, arc.separation() * scale * guiScale, 1.0e-3);
                    assertEquals(reference.radiusY() * baseScale, arc.radiusY() * scale * guiScale, 1.0e-4);
                    assertEquals(baseScale, scale * guiScale, 1.0e-6);
                    int guiWidth = (int) Math.ceil((double) width / guiScale);
                    float center = ScopeHudGeometry.crosshairCenter(guiWidth);
                    float hudCenter = center / scale;
                    assertEquals(center * guiScale, hudCenter * scale * guiScale, 1.0e-3);
                }
            }
        }
    }

    @Test
    void defaultHudScaleMatchesVanillaGuiScaleFive() {
        float scale = ScopeHudGeometry.hudScale(5, 100);
        assertEquals(1.0F, scale, 1.0e-6);
        var arc = ScopeHudGeometry.arc(0, 640, 5, 90);
        assertEquals(9 * 5, arc.radiusY() * scale * 5, 1.0e-5);
    }

    @Test
    void matchesTheVanillaSpriteCenterForEvenAndOddGuiDimensions() {
        assertEquals(319.5F, ScopeHudGeometry.crosshairCenter(640));
        assertEquals(320.5F, ScopeHudGeometry.crosshairCenter(641));
        assertEquals(179.5F, ScopeHudGeometry.crosshairCenter(360));
        assertEquals(180.5F, ScopeHudGeometry.crosshairCenter(361));
    }

    @Test
    void halvesAreMirroredHorizontallyAndSymmetricVerticallyIncludingBothEndpoints() {
        for (double angle : new double[]{0, 45, 90}) {
            var arc = ScopeHudGeometry.arc(angle, 640, 5, 90);
            for (boolean outer : new boolean[]{false, true}) {
                for (int step = 0; step <= ScopeHudGeometry.ARC_SEGMENTS; step++) {
                    var left = arc.point(true, step, outer);
                    var right = arc.point(false, step, outer);
                    var opposite = arc.point(true, ScopeHudGeometry.ARC_SEGMENTS - step, outer);
                    assertEquals(-left.x(), right.x(), 1.0e-6);
                    assertEquals(left.y(), right.y(), 1.0e-6);
                    assertEquals(left.x(), opposite.x(), 1.0e-6);
                    assertEquals(-left.y(), opposite.y(), 1.0e-6);
                }
            }
        }
    }

    @Test
    void lockedHalvesFormACircleAtTheCrosshair() {
        var arc = ScopeHudGeometry.arc(5, 640, 5, 90);
        assertEquals(0, arc.separation());
        for (int step = 0; step <= ScopeHudGeometry.ARC_SEGMENTS; step++) {
            var point = arc.point(false, step, true);
            assertEquals(arc.radiusY() + ScopeHudGeometry.HALF_STROKE,
                    Math.hypot(point.x(), point.y()), 1.0e-5);
        }
    }

    @Test
    void arcsShrinkAndFlattenAsTheyMoveAwayFromTheCrosshair() {
        var near = ScopeHudGeometry.arc(5, 640, 5, 90);
        var middle = ScopeHudGeometry.arc(45, 640, 5, 90);
        var far = ScopeHudGeometry.arc(90, 640, 5, 90);
        assertTrue(near.separation() < middle.separation() && middle.separation() < far.separation());
        assertTrue(near.radiusY() > middle.radiusY() && middle.radiusY() > far.radiusY());
        assertTrue(near.radiusX() > middle.radiusX() && middle.radiusX() > far.radiusX());
        assertTrue(near.radiusX() / near.radiusY() > middle.radiusX() / middle.radiusY());
        assertTrue(middle.radiusX() / middle.radiusY() > far.radiusX() / far.radiusY());
        assertTrue(far.radiusX() > ScopeHudGeometry.HALF_STROKE);
    }

    @Test
    void targetsAtTheBoundaryRemainVisibleButTargetsBehindItAreHidden() {
        assertTrue(ScopeHudGeometry.isVisible(0, 90));
        assertTrue(ScopeHudGeometry.isVisible(90, 90));
        assertFalse(ScopeHudGeometry.isVisible(90.001, 90));
        assertFalse(ScopeHudGeometry.isVisible(180, 90));
    }

    @Test
    void configuredAnglesControlLockShapeAndVisibilityBoundary() {
        var locked = ScopeHudGeometry.arc(20, 640, 20, 60);
        assertEquals(0, locked.separation());
        assertEquals(locked.radiusX(), locked.radiusY());
        assertEquals(ScopeHudGeometry.arc(90, 640, 5, 90),
                ScopeHudGeometry.arc(60, 640, 20, 60));
        assertTrue(ScopeHudGeometry.isVisible(60, 60));
        assertFalse(ScopeHudGeometry.isVisible(60.001, 60));
    }
}
