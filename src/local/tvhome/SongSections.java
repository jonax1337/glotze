package local.tvhome;

/**
 * Follows the shape of a song through its bass. A surge is the moment the bass of the last third
 * of a second leaps well above the seconds before it: a drop after a breakdown, the beat kicking
 * in, a chorus coming in harder. It is caught within a few captures, while the music still lands.
 * The breakdowns in between show as the bass falling below the song's usual.
 */
final class SongSections {
    private static final float NOW_WEIGHT = 0.15f;
    private static final float BEFORE_WEIGHT = 0.0125f;
    private static final float SONG_WEIGHT = 0.004f;
    /** The bass now must be this many times the bass before, and near the song's usual at least. */
    private static final float SURGE_JUMP = 1.4f;
    private static final float SURGE_SHARE_OF_SONG = 0.9f;
    /** Four seconds at twenty captures a second: the start of a song is no surge. */
    private static final int SETTLING_CAPTURES = 80;
    /** Eight seconds between surges, so each one stays a moment. */
    private static final int SURGE_GAP_CAPTURES = 160;
    private static final float QUIETEST = 1f;

    private float now;
    private float before;
    private float song;
    private int captures;
    private int sinceSurge = SURGE_GAP_CAPTURES;
    private volatile float lift = 1f;
    private volatile int surges;

    int surges() {
        return surges;
    }

    float lift() {
        return lift;
    }

    void add(float bass) {
        if (captures == 0) {
            now = bass;
            before = bass;
            song = bass;
        }
        captures++;
        sinceSurge++;
        now += (bass - now) * NOW_WEIGHT;
        before += (bass - before) * BEFORE_WEIGHT;
        song += (bass - song) * SONG_WEIGHT;
        if (song < QUIETEST) {
            return;
        }
        lift = now / song;
        if (isSurging()) {
            surges++;
            sinceSurge = 0;
        }
    }

    void reset() {
        captures = 0;
        sinceSurge = SURGE_GAP_CAPTURES;
        lift = 1f;
    }

    private boolean isSurging() {
        return captures > SETTLING_CAPTURES && sinceSurge > SURGE_GAP_CAPTURES
                && now > SURGE_JUMP * before && now > SURGE_SHARE_OF_SONG * song;
    }
}
