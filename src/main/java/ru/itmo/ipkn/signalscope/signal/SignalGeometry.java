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

    /** Tests a position against a cone whose azimuth is clockwise from north. */
    public static boolean inCone(double azimuth, double elevation, double fullAngle,
                                 double deltaX, double deltaY, double deltaZ) {
        double yaw = Math.toRadians(azimuth);
        double pitch = Math.toRadians(elevation);
        double horizontal = Math.cos(pitch);
        return angleDegrees(Math.sin(yaw) * horizontal, Math.sin(pitch), -Math.cos(yaw) * horizontal,
                deltaX, deltaY, deltaZ) <= fullAngle / 2.0 + 1.0e-9;
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
