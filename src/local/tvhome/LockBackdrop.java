package local.tvhome;

import java.util.Calendar;

/**
 * The colours of each lock screen background, for the drifting light of {@link AuroraView}:
 * a dark base and three glows. Aurora and "time of day" follow the hour.
 */
final class LockBackdrop {
    // Each palette: base, then the three glows.
    private static final int[] AURORA_NIGHT = {0xFF060714, 0xFF1C2A6B, 0xFF0F5C6E, 0xFF3B1F66};
    private static final int[] AURORA_MORNING = {0xFF0E0A1A, 0xFFE0785F, 0xFFB43F7A, 0xFF3A3F9E};
    private static final int[] AURORA_DAY = {0xFF050A18, 0xFF2F72E0, 0xFF12A99E, 0xFF7E55E8};
    private static final int[] AURORA_EVENING = {0xFF0A0614, 0xFFE8693A, 0xFFC42E66, 0xFF2B308C};
    private static final int[] SUNSET = {0xFF120812, 0xFFE0603C, 0xFFB83280, 0xFFE8963A};
    private static final int[] OCEAN = {0xFF030C18, 0xFF0F6E9E, 0xFF1AA6A0, 0xFF2A4FB0};
    private static final int[] FOREST = {0xFF040E08, 0xFF2F7A4A, 0xFF5E8A2E, 0xFF1F6B6B};
    private static final int[] LAVENDER = {0xFF0C0818, 0xFF7E5BD6, 0xFFD27FB8, 0xFF4A5BD0};
    private static final int[] GRAPHITE = {0xFF08090C, 0xFF3A4250, 0xFF4A4F5C, 0xFF2A3140};
    private static final int[] NIGHT = {0xFF02040A, 0xFF1D2D6B, 0xFF0F1B44, 0xFF2B1F5E};

    private static final int MORNING_FROM_HOUR = 6;
    private static final int DAY_FROM_HOUR = 10;
    private static final int EVENING_FROM_HOUR = 17;
    private static final int NIGHT_FROM_HOUR = 21;

    private LockBackdrop() {
    }

    /** Black is the only background without light. */
    static boolean isLit(LockStyle.Background background) {
        return background != LockStyle.Background.BLACK;
    }

    /** Only for lit backgrounds, see {@link #isLit}. */
    static int[] paletteOf(LockStyle.Background background) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        switch (background) {
            case AURORA:
                return byHour(hour, AURORA_MORNING, AURORA_DAY, AURORA_EVENING, AURORA_NIGHT);
            case TIME_OF_DAY:
                return byHour(hour, LAVENDER, OCEAN, SUNSET, NIGHT);
            case SUNSET:
                return SUNSET;
            case OCEAN:
                return OCEAN;
            case FOREST:
                return FOREST;
            case LAVENDER:
                return LAVENDER;
            default:
                return GRAPHITE;
        }
    }

    private static int[] byHour(int hour, int[] morning, int[] day, int[] evening, int[] night) {
        if (hour >= MORNING_FROM_HOUR && hour < DAY_FROM_HOUR) {
            return morning;
        }
        if (hour >= DAY_FROM_HOUR && hour < EVENING_FROM_HOUR) {
            return day;
        }
        return hour >= EVENING_FROM_HOUR && hour < NIGHT_FROM_HOUR ? evening : night;
    }
}
