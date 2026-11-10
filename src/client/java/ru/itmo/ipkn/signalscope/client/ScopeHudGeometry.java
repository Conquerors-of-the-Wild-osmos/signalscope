package ru.itmo.ipkn.signalscope.client;

final class ScopeHudGeometry {
    static final int ARC_SEGMENTS = 32;
    static final float HALF_STROKE = 0.875F;

    private ScopeHudGeometry() {
    }

    static float hudScale(double guiScale, int percent) {
        // At 100%, one HUD unit occupies five framebuffer pixels, regardless of vanilla GUI scale.
        return (float) (5.0 * percent / 100.0 / guiScale);
    }

    static float crosshairCenter(int extent) {
        // Vanilla places its 15px sprite at (extent - 15) / 2 using integer division.
        return (extent - 15) / 2 + 7.5F;
    }

    static boolean isVisible(double angle, double receptionAngle) {
        return angle >= 0 && angle <= receptionAngle;
    }

    static Arc arc(double angle, float screenWidth, double stableAngle, double receptionAngle) {
        double progress = Math.clamp((angle - stableAngle) / (receptionAngle - stableAngle), 0.0, 1.0);
        return new Arc((float) (progress * Math.min(108, screenWidth * 0.3)),
                (float) (9 * (1 - 0.75 * progress)),
                (float) (9 * (1 - 0.35 * progress)));
    }

    record Arc(float separation, float radiusX, float radiusY) {
        Point point(boolean left, int step, boolean outer) {
            double angle = -Math.PI / 2 + Math.PI * step / ARC_SEGMENTS;
            float stroke = outer ? HALF_STROKE : -HALF_STROKE;
            return new Point((float) (Math.cos(angle) * (radiusX + stroke)) * (left ? -1 : 1),
                    (float) (Math.sin(angle) * (radiusY + stroke)));
        }
    }

    record Point(float x, float y) {
    }
}
