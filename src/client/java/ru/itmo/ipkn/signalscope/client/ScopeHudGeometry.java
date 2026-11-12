package ru.itmo.ipkn.signalscope.client;

final class ScopeHudGeometry {
    static final int WIDTH = 16;
    static final int HEIGHT = 30;
    private static final double[] RADII = {13.5, 12, 9, 6, 3};
    private static final int[][][] PIXELS = new int[5][HEIGHT][WIDTH];

    // Невероятное нейрохрючево с 5-ю вложенными циклами
    static {
        for (int state = 0; state < RADII.length; state++) {
            for (int row = 0; row < HEIGHT; row++) {
                for (int col = 0; col < WIDTH; col++) {
                    int body = 0;
                    int outline = 0;
                    for (int sy = 0; sy < 4; sy++) {
                        for (int sx = 0; sx < 4; sx++) {
                            double x = col + (sx + 0.5) / 4;
                            double y = row - HEIGHT / 2.0 + (sy + 0.5) / 4;
                            double radius = RADII[state];
                            boolean outer = insideEllipse(x, y, radius, 13.5);
                            boolean inner = insideEllipse(x, y, radius - 3, 10.5);
                            if (outer && !inner) {
                                body++;
                            } else if (insideEllipse(x, y, radius + 1, 14.5)
                                    && !insideEllipse(x, y, radius - 4, 9.5)) {
                                outline++;
                            }
                        }
                    }
                    PIXELS[state][row][col] = body | (outline << 5);
                }
            }
        }
    }

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

    private static boolean insideEllipse(double x, double y, double radiusX, double radiusY) {
        return radiusX > 0 && x * x / (radiusX * radiusX) + y * y / (radiusY * radiusY) <= 1;
    }

    static int pixelColor(int state, int row, int col, int color) {
        int coverage = PIXELS[state][row][col];
        int body = coverage & 31;
        int outline = coverage >>> 5;
        int total = body + outline;
        if (total == 0) return 0;
        int alpha = (color >>> 24) * total / 16;
        int weight = 2 * body + outline;
        int red = ((color >> 16) & 255) * weight / (2 * total);
        int green = ((color >> 8) & 255) * weight / (2 * total);
        int blue = (color & 255) * weight / (2 * total);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
