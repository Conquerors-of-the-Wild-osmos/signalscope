package ru.itmo.ipkn.signalscope.signal;

public final class SignalScreenGeometry {

    private SignalScreenGeometry() {
    }

    public static boolean blocks(double x1, double y1, double z1, double x2, double y2, double z2,
                                 int blockX, int blockY, int blockZ, int power) {
        if (power <= 0) return false;
        double radius = power - 0.5;
        double[] center = {blockX + 0.5, blockY + 0.5, blockZ + 0.5};
        double[] from = {x1, y1, z1};
        double[] to = {x2, y2, z2};
        boolean fromInside = true;
        boolean toInside = true;
        double enter = 0;
        double exit = 1;
        for (int i = 0; i < 3; i++) {
            double min = center[i] - radius;
            double max = center[i] + radius;
            fromInside &= from[i] >= min && from[i] <= max;
            toInside &= to[i] >= min && to[i] <= max;
            double delta = to[i] - from[i];
            if (delta == 0) {
                if (from[i] < min || from[i] > max) return false;
            } else {
                double a = (min - from[i]) / delta;
                double b = (max - from[i]) / delta;
                enter = Math.max(enter, Math.min(a, b));
                exit = Math.min(exit, Math.max(a, b));
                if (enter > exit) return false;
            }
        }
        return !(fromInside && toInside);
    }
}
