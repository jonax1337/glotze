package local.tvhome;

/**
 * Finds a song's tempo and where its beats fall. The onsets of the last seconds are compared
 * with themselves shifted by one possible beat length after another (autocorrelation); the
 * shift at which they line up best is the beat. Tempos near 120 are preferred when two fit about
 * equally, since a beat and its double or half always line up too. Where the onsets pile up
 * within one beat length tells when the beats fall, and a {@link BeatClock} ticks along them.
 * A song whose bass does not repeat clearly enough has no beat for the clock to follow; through
 * a short breakdown the clock keeps ticking, since the song keeps its tempo.
 */
final class TempoTracker {
    /** About eight seconds of onsets at the usual twenty captures a second. */
    private static final int HISTORY = 160;
    /** The first estimate comes after three seconds; the window grows from there. */
    private static final int FIRST_ESTIMATE_AT = 60;
    /** How long the clock ticks on through a stretch without a clear beat. */
    private static final long KEEP_BEAT_MS = 8000;
    private static final float SLOWEST_BPM = 60f;
    private static final float FASTEST_BPM = 180f;
    private static final float PREFERRED_BPM = 120f;
    private static final float PREFERENCE_OCTAVES = 1f;
    // How well the onsets must line up to name a tempo, and to keep naming it once named.
    private static final float SURE = 0.3f;
    private static final float STILL_SURE = 0.18f;
    private static final int ESTIMATE_EVERY = 4;
    private static final float INTERVAL_WEIGHT = 0.05f;

    private final float[] onsets = new float[HISTORY];
    private final BeatClock clock = new BeatClock();
    private int next;
    private int filled;
    private int sinceEstimate;
    private long lastAt;
    private float captureSeconds;
    private boolean sure;
    private long lastSureAt;
    private volatile float bpm;

    /** Beats per minute, or 0 while the song has no clear beat. */
    float bpm() {
        return bpm;
    }

    float beat(long now) {
        return clock.beat(now);
    }

    void add(float onset, long now) {
        measureCaptureInterval(now);
        onsets[next] = onset;
        next = (next + 1) % HISTORY;
        filled = Math.min(filled + 1, HISTORY);
        sinceEstimate++;
        if (filled >= FIRST_ESTIMATE_AT && sinceEstimate >= ESTIMATE_EVERY) {
            sinceEstimate = 0;
            estimate(now);
        }
    }

    void reset(long now) {
        next = 0;
        filled = 0;
        sinceEstimate = 0;
        lastAt = 0;
        captureSeconds = 0f;
        loseBeat(now);
    }

    private void measureCaptureInterval(long now) {
        if (lastAt > 0) {
            float interval = (now - lastAt) / 1000f;
            captureSeconds = captureSeconds == 0f
                    ? interval : captureSeconds + (interval - captureSeconds) * INTERVAL_WEIGHT;
        }
        lastAt = now;
    }

    private void estimate(long now) {
        float mean = mean();
        float variance = correlation(0, mean);
        if (variance <= 0f || captureSeconds <= 0f) {
            loseBeat(now);
            return;
        }
        int shortest = Math.max(2, Math.round(60f / (FASTEST_BPM * captureSeconds)));
        int longest = Math.min(filled / 2, Math.round(60f / (SLOWEST_BPM * captureSeconds)));
        float[] match = new float[longest + 2];
        for (int lag = shortest - 1; lag <= longest + 1; lag++) {
            match[lag] = correlation(lag, mean) / variance;
        }
        int best = bestLag(match, shortest, longest);
        sure = match[best] >= (sure ? STILL_SURE : SURE);
        if (sure) {
            lastSureAt = now;
            float lag = best + peakOffset(match[best - 1], match[best], match[best + 1]);
            bpm = bpmAt(lag);
            clock.follow(lastBeatAt(lag), lag * captureSeconds * 1000f, now);
        } else if (now - lastSureAt > KEEP_BEAT_MS) {
            loseBeat(now);
        }
    }

    private void loseBeat(long now) {
        sure = false;
        lastSureAt = 0;
        bpm = 0f;
        clock.pause(now);
    }

    private int bestLag(float[] match, int shortest, int longest) {
        int best = shortest;
        float bestScore = -1f;
        for (int lag = shortest; lag <= longest; lag++) {
            float score = match[lag] * preference(bpmAt(lag));
            if (score > bestScore) {
                bestScore = score;
                best = lag;
            }
        }
        return best;
    }

    /** The latest beat: the offset within one beat length at which the onsets pile up most. */
    private long lastBeatAt(float lag) {
        int bestOffset = 0;
        float bestSum = -1f;
        for (int offset = 0; offset < Math.round(lag); offset++) {
            float sum = 0f;
            for (float back = offset; back < filled - 0.5f; back += lag) {
                sum += onsetsBack(Math.round(back));
            }
            if (sum > bestSum) {
                bestSum = sum;
                bestOffset = offset;
            }
        }
        return lastAt - Math.round(bestOffset * captureSeconds * 1000f);
    }

    private float onsetsBack(int captures) {
        return onsets[(next - 1 - captures + HISTORY) % HISTORY];
    }

    private float mean() {
        float sum = 0f;
        for (int i = 0; i < filled; i++) {
            sum += onsetAt(i);
        }
        return sum / filled;
    }

    /** How alike the onsets are to themselves {@code lag} captures later. */
    private float correlation(int lag, float mean) {
        float sum = 0f;
        for (int i = 0; i + lag < filled; i++) {
            sum += (onsetAt(i) - mean) * (onsetAt(i + lag) - mean);
        }
        return sum;
    }

    /** The {@code index}th onset kept, oldest first. */
    private float onsetAt(int index) {
        int oldest = filled < HISTORY ? 0 : next;
        return onsets[(oldest + index) % HISTORY];
    }

    private float bpmAt(float lag) {
        return 60f / (lag * captureSeconds);
    }

    private static float preference(float bpm) {
        double octaves = Math.log(bpm / PREFERRED_BPM) / Math.log(2) / PREFERENCE_OCTAVES;
        return (float) Math.exp(-0.5 * octaves * octaves);
    }

    /** Where the true peak lies between whole captures, from the parabola through three points. */
    private static float peakOffset(float before, float peak, float after) {
        float curvature = before - 2f * peak + after;
        if (curvature >= 0f) {
            return 0f;
        }
        return Math.max(-0.5f, Math.min(0.5f, 0.5f * (before - after) / curvature));
    }
}
