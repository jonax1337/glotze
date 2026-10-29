package local.tvhome;

import android.media.audiofx.Visualizer;
import android.util.Log;

/**
 * Listens to everything the TV plays (Android's output mix) in three bands, one per backdrop
 * glow: bass (the kick), mids (voice, guitar) and highs (hi-hats). Each band is measured against
 * its own average of the last seconds, so a glow lights up when its part of the music rises above
 * the groove and rests when it falls back, whatever the song's volume. The bass also drives the
 * beat clock and shows the song's breakdowns and the moments it surges.
 * Needs RECORD_AUDIO, granted once over ADB: {@code pm grant local.tvhome android.permission.RECORD_AUDIO}.
 */
final class SoundLevel implements Pulse {
    private static final String TAG = "SoundLevel";
    private static final int OUTPUT_MIX = 0;
    /** Fine enough for bins of about 43 Hz, so the bass band is real bass. */
    private static final int WANTED_CAPTURE_SIZE = 1024;
    /** Band edges in Hz: bass, mids, highs. */
    private static final float[][] BANDS_HZ = {{40f, 170f}, {200f, 2_000f}, {2_000f, 8_000f}};
    /** Share of a new measurement in the running average; about three seconds at the capture rate. */
    private static final float AVERAGE_WEIGHT = 0.02f;
    /** Level at the average; rising above it brightens, falling below it rests. */
    private static final float LEVEL_AT_AVERAGE = 0.35f;
    /** How strongly a rise above the average shows. */
    private static final float RISE_GAIN = 1.4f;
    /** Below this the band counts as silent, so noise is not amplified. */
    private static final float QUIETEST = 1f;
    private static final int BASS = 0;

    private final float[] average = new float[BANDS_HZ.length];
    private final float[] levels = new float[BANDS_HZ.length];
    private final TempoTracker tempo = new TempoTracker();
    private final SongSections sections = new SongSections();
    private Visualizer visualizer;
    private float previousBass;

    void start() {
        try {
            visualizer = new Visualizer(OUTPUT_MIX);
            int[] sizes = Visualizer.getCaptureSizeRange();
            visualizer.setCaptureSize(Math.max(sizes[0], Math.min(sizes[1], WANTED_CAPTURE_SIZE)));
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer source, byte[] waveform, int samplingRate) {
                }

                @Override
                public void onFftDataCapture(Visualizer source, byte[] fft, int samplingRate) {
                    measure(fft, samplingRate);
                }
            }, Visualizer.getMaxCaptureRate(), false, true);
            visualizer.setEnabled(true);
        } catch (RuntimeException e) {
            Log.w(TAG, "No access to the sound; the backdrop stays calm", e);
            stop();
        }
    }

    void stop() {
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
        startNewSong();
    }

    /** Forgets everything heard so far: a new song has its own tempo, loudness and shape. */
    void startNewSong() {
        for (int band = 0; band < levels.length; band++) {
            levels[band] = 0f;
            average[band] = 0f;
        }
        previousBass = 0f;
        tempo.reset(System.currentTimeMillis());
        sections.reset();
    }

    @Override
    public float level(int glow) {
        return levels[glow];
    }

    @Override
    public float tempo() {
        return tempo.bpm();
    }

    @Override
    public float beat(long now) {
        return tempo.beat(now);
    }

    @Override
    public int surges() {
        return sections.surges();
    }

    @Override
    public float lift() {
        return sections.lift();
    }

    /** {@code samplingRate} comes in milliHertz. */
    private void measure(byte[] fft, int samplingRate) {
        float binHz = samplingRate / 1000f / fft.length;
        for (int band = 0; band < BANDS_HZ.length; band++) {
            float value = bandMagnitude(fft, binHz, BANDS_HZ[band]);
            average[band] += (value - average[band]) * AVERAGE_WEIGHT;
            float rise = average[band] < QUIETEST ? 0f : value / average[band] - 1f;
            levels[band] = Math.max(0f, Math.min(1f, LEVEL_AT_AVERAGE + RISE_GAIN * rise));
            if (band == BASS) {
                followBass(value);
            }
        }
    }

    /** A kick shows as a jump of the bass from one capture to the next. */
    private void followBass(float bass) {
        tempo.add(Math.max(0f, bass - previousBass), System.currentTimeMillis());
        previousBass = bass;
        sections.add(bass);
    }

    /** The FFT holds real and imaginary parts in turn, after the DC and Nyquist terms. */
    private static float bandMagnitude(byte[] fft, float binHz, float[] bandHz) {
        int bins = fft.length / 2;
        int first = Math.max(1, Math.round(bandHz[0] / binHz));
        int last = Math.min(bins - 1, Math.max(first, Math.round(bandHz[1] / binHz)));
        float sum = 0f;
        for (int bin = first; bin <= last; bin++) {
            sum += (float) Math.hypot(fft[2 * bin], fft[2 * bin + 1]);
        }
        return sum / (last - first + 1);
    }
}
