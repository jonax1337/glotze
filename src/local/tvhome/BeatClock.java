package local.tvhome;

/**
 * A metronome that follows the song. Between measurements it ticks on by itself, so its beats
 * come evenly; each measurement pulls it gently towards where the beats were heard, so it stays
 * on them without jumping. It only ever counts forward, even when it is pulled back.
 */
final class BeatClock {
    /** A measured offset is made up over this many beats. */
    private static final float CORRECTION_BEATS = 4f;
    /** Share of a newly measured beat length taken over at once; the rest smooths out jitter. */
    private static final float LENGTH_WEIGHT = 0.3f;
    /** The sound is heard a little before the onsets show up in the captures; measured on the TV. */
    private static final long LEAD_MS = 40;

    private volatile Ticking ticking = new Ticking(0L, 0f, 0f);
    private float beatMs;

    /** Beats counted at {@code now}; whole numbers fall on the beat. */
    float beat(long now) {
        return ticking.beat(now + LEAD_MS);
    }

    /** {@code beatAt} is when the latest beat was heard, {@code measuredBeatMs} the measured beat length. */
    void follow(long beatAt, float measuredBeatMs, long now) {
        Ticking current = ticking;
        if (!current.isTicking()) {
            beatMs = measuredBeatMs;
            ticking = new Ticking(beatAt, (float) Math.ceil(current.beat(now)), 1f / beatMs);
            return;
        }
        beatMs += (measuredBeatMs - beatMs) * LENGTH_WEIGHT;
        float heard = current.beat(beatAt);
        float offset = Math.round(heard) - heard;
        ticking = new Ticking(now, current.beat(now), (1f + offset / CORRECTION_BEATS) / beatMs);
    }

    /** Stops counting where it is, so a later beat carries on from here. */
    void pause(long now) {
        ticking = new Ticking(now, ticking.beat(now), 0f);
    }

    /** A straight count from one moment on; replaced whole, so readers on other threads see it consistently. */
    private static final class Ticking {
        private final long since;
        private final float beatThen;
        private final float beatsPerMs;

        Ticking(long since, float beatThen, float beatsPerMs) {
            this.since = since;
            this.beatThen = beatThen;
            this.beatsPerMs = beatsPerMs;
        }

        boolean isTicking() {
            return beatsPerMs > 0f;
        }

        float beat(long now) {
            return beatThen + (now - since) * beatsPerMs;
        }
    }
}
