package local.tvhome;

/**
 * The slow floating of the idle mascot: a lift, a slight tilt and a faint breath, each a sine of
 * its own length so the three never line up and the motion does not look mechanical. All three
 * start at rest and have no seam, so the motion may begin and pause at any time.
 */
final class IdleDrift {
    // Artwork units; at the usual size on the home screen one unit is about 0.6 px.
    private static final float LIFT_AMPLITUDE = 17f;
    private static final float TILT_AMPLITUDE_DEGREES = 1.2f;
    private static final float BREATH_AMPLITUDE = 0.01f;
    private static final float LIFT_PERIOD_SECONDS = 3.4f;
    private static final float TILT_PERIOD_SECONDS = 4.1f;
    private static final float BREATH_PERIOD_SECONDS = 5.3f;
    private static final double FULL_TURN = 2 * Math.PI;

    private IdleDrift() {
    }

    /** Lays the drift of this moment over the pose. */
    static void addTo(GlotzeFigure.Pose pose, float seconds) {
        pose.dy += LIFT_AMPLITUDE * wave(seconds, LIFT_PERIOD_SECONDS);
        pose.rotation += TILT_AMPLITUDE_DEGREES * wave(seconds, TILT_PERIOD_SECONDS);
        pose.scale += BREATH_AMPLITUDE * wave(seconds, BREATH_PERIOD_SECONDS);
    }

    private static float wave(float seconds, float periodSeconds) {
        return (float) Math.sin(FULL_TURN * seconds / periodSeconds);
    }
}
