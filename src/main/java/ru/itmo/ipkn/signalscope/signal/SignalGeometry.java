package ru.itmo.ipkn.signalscope.signal;

/**
 * Direction calculations shared by the HUD and tests; independent of rendering and audio.
 */
public final class SignalGeometry {

    private SignalGeometry() {
    }

    public static double gain(double angle, double stableAngle, double receptionAngle) {
        // The outer two thirds remain available for visual tracking, without audio.
        double linear = Math.clamp(1.0 - 3.0 * (angle - stableAngle) / (receptionAngle - stableAngle), 0.0, 1.0);
        return linear * linear;
    }

    public static double angleDegrees(double lookX, double lookY, double lookZ,
                                      double deltaX, double deltaY, double deltaZ) {
        double lookLength = Math.sqrt(lookX * lookX + lookY * lookY + lookZ * lookZ);
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance < 1.0e-9) {
            return 0;
        }
        if (lookLength < 1.0e-9) {
            return 180;
        }
        double cosine = (lookX * deltaX + lookY * deltaY + lookZ * deltaZ) / (lookLength * distance);
        return Math.toDegrees(Math.acos(Math.clamp(cosine, -1.0, 1.0)));
    }
}
