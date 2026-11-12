package ru.itmo.ipkn.signalscope.client;

final class ScopeHudGeometry {
    private static final int[] RADII = {9, 8, 6, 4, 2};

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

    static int state(double angle, double stableAngle, double receptionAngle) {
        if (angle <= stableAngle) return 0;
        return Math.clamp((int) Math.ceil(4 * (angle - stableAngle) / (receptionAngle - stableAngle)), 1, 4);
    }

    static float separation(double angle, float screenWidth, double stableAngle, double receptionAngle) {
        double progress = Math.clamp((angle - stableAngle) / (receptionAngle - stableAngle), 0.0, 1.0);
        return (float) (progress * Math.min(108, screenWidth * 0.3F));
    }

    static int outerOffset(int state, int row) {
        double y = (row - 8) / 9.0;
        return (int) Math.round(RADII[state] * Math.sqrt(1 - y * y));
    }

    static int innerOffset(int state, int row) {
        double y = (row - 8) / 7.0;
        return Math.abs(y) >= 1 ? 0 : (int) Math.round((RADII[state] - 2) * Math.sqrt(1 - y * y));
    }
}
