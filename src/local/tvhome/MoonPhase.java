package local.tvhome;

/** The moon's phase from the mean length of its cycle; accurate to well within a day. */
final class MoonPhase {
    private static final double SYNODIC_MONTH_DAYS = 29.530588853;
    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private static final long KNOWN_NEW_MOON_MS = 947_182_440_000L;
    private static final double DAY_MS = 86_400_000d;
    private static final double HALF = 0.5;
    // Where each named phase ends, as a fraction of the cycle.
    private static final double[] PHASE_ENDS = {0.03, 0.22, 0.28, 0.47, 0.53, 0.72, 0.78, 0.97};
    private static final int[] PHASE_NAMES = {
            R.string.moon_new, R.string.moon_waxing_crescent, R.string.moon_first_quarter,
            R.string.moon_waxing_gibbous, R.string.moon_full, R.string.moon_waning_gibbous,
            R.string.moon_last_quarter, R.string.moon_waning_crescent};

    /** 0 at new moon, 0.5 at full moon, approaching 1 just before the next new moon. */
    final double age;

    private MoonPhase(double age) {
        this.age = age;
    }

    static MoonPhase at(long millis) {
        double cycles = (millis - KNOWN_NEW_MOON_MS) / DAY_MS / SYNODIC_MONTH_DAYS;
        return new MoonPhase(cycles - Math.floor(cycles));
    }

    boolean isWaxing() {
        return age < HALF;
    }

    /** The lit share of the disc, 0 to 1. */
    double illumination() {
        return (1 - Math.cos(2 * Math.PI * age)) / 2;
    }

    int nameRes() {
        for (int i = 0; i < PHASE_ENDS.length; i++) {
            if (age < PHASE_ENDS[i]) {
                return PHASE_NAMES[i];
            }
        }
        return R.string.moon_new;
    }
}
