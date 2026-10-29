package local.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Soft light flowing through the dark, in the colours of the chosen palette. Each frame is
 * computed at a tiny resolution and stretched over the screen by the GPU: the glow is smooth
 * by nature, so the scaling costs nothing visible, and even a Fire TV Stick keeps up easily.
 * Each glow wanders within its own part of the screen, so they never pile up into one spot, and
 * the space between them ripples gently like light through water.
 * <p>
 * With a {@link Pulse} the field comes alive with music. Each glow brightens with its band, the
 * ripples swell with the melody and shimmer with the highs. When the song has a clear beat, the
 * glows dance to it: the bass glow hops and lands on every beat, the mids glow sways across every
 * two beats, the highs glow circles once a bar, and every other beat a wave of light rolls out.
 * A breakdown draws the light in and dims it; the moment the music surges back, it bursts out. The
 * tempo sets how much of all this a song gets: a ballad sways, a club track dances.
 */
public final class AuroraView extends View {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 36;
    private static final long FRAME_MS = 33;
    private static final float ASPECT = 16f / 9f;
    private static final float GLOW_RADIUS = 0.55f;
    private static final float GLOW_STRENGTH = 0.8f;
    /** Where each glow lives: upper left, upper right, bottom. */
    private static final float[][] HOMES = {{0.22f, 0.3f}, {0.8f, 0.28f}, {0.5f, 0.82f}};
    /** How far a glow strays from its home, across and down. */
    private static final float[] REACH = {0.17f, 0.14f};
    /** Seconds each glow takes for one loop around its home; mutually prime, so the picture never repeats. */
    private static final float[][] ORBIT_SECONDS = {{97f, 71f}, {83f, 113f}, {131f, 59f}};
    private static final float[][] ORBIT_PHASE = {{0.1f, 0.7f}, {2.3f, 4.1f}, {4.4f, 1.9f}};
    private static final long PALETTE_BLEND_MS = 2500;
    // The ripples that make the light look liquid: their size, density and pace.
    private static final float CALM_RIPPLE = 0.025f;
    private static final float RIPPLE_DENSITY = 5.5f;
    private static final float RIPPLE_SECONDS = 9f;
    // How the bands show, all eased so the light flows rather than jumps.
    private static final float MUSIC_FLOW = 3f;
    private static final float MUSIC_RIPPLE = 0.1f;
    private static final float MUSIC_BRIGHTENING = 0.7f;
    /** Per glow (bass, mids, highs): how much it grows with its band; the melody shows most. */
    private static final float[] MUSIC_SWELL = {0.15f, 0.4f, 0.25f};
    /** Per glow: share of the way to its band's level covered each frame, rising and falling. */
    private static final float[] RISE_EASING = {0.7f, 0.45f, 0.5f};
    private static final float[] FALL_EASING = {0.12f, 0.07f, 0.1f};
    /** The song's overall energy changes the flow only gently, over about a second. */
    private static final float ENERGY_EASING = 0.04f;
    // The highs make the light shimmer: fine, quick ripples on top of the slow ones.
    private static final float SHIMMER = 0.018f;
    private static final float SHIMMER_DENSITY = 16f;
    private static final float SHIMMER_SPEED = 7f;
    // The tempo sets the temperament: slow songs (or ones without a clear beat) sway gently with
    // a share of the show, fast ones get all of it.
    private static final float SLOW_BPM = 80f;
    private static final float FAST_BPM = 130f;
    private static final float CALM_VIGOUR = 0.4f;
    /** A new tempo takes over within a few seconds, so one misheard bar does not jolt the picture. */
    private static final float TEMPO_EASING = 0.02f;
    // On each beat the bass glow flares and the field surges forward, the start of a bar most.
    private static final int BEATS_PER_BAR = 4;
    private static final float BEAT_ACCENT = 0.6f;
    private static final float KICK_FADE = 0.86f;
    private static final float CALM_KICK_FADE = 0.94f;
    private static final float KICK_PUSH = 5f;
    private static final float KICK_FLARE = 0.7f;
    private static final float KICK_SWELL = 0.3f;
    // The dance: how far the bass glow hops, the mids glow sways and the highs glow circles.
    private static final float HOP = 0.05f;
    private static final float SWAY = 0.07f;
    private static final float CIRCLE = 0.04f;
    // Beat waves: soft rings that start at a glow, grow and fade. They go out every other beat, so
    // they read as rhythm rather than a blur of rings, and each lives two beats.
    private static final int MAX_WAVES = 4;
    private static final int BEATS_PER_WAVE = 2;
    private static final float WAVE_BEATS = 2f;
    private static final float SHORTEST_WAVE_SECONDS = 1f;
    private static final float LONGEST_WAVE_SECONDS = 2.2f;
    private static final float WAVE_START = 0.08f;
    private static final float WAVE_TRAVEL = 0.9f;
    private static final float WAVE_WIDTH = 0.1f;
    private static final float WAVE_STRENGTH = 0.4f;
    // A breakdown draws the light in and dims it, the more the further the bass has fallen.
    private static final float TENSION_RANGE = 0.5f;
    private static final float TENSION_EASING = 0.05f;
    private static final float TENSION_SHRINK = 0.2f;
    private static final float TENSION_DIM = 0.3f;
    // A surge (a drop, the beat kicking in): a wide wave from the centre, every glow flares, and
    // the show runs bigger for a while.
    private static final float SURGE_SECONDS = 6f;
    private static final float SURGE_BOOST = 0.7f;
    private static final float SURGE_FLARE = 0.6f;
    private static final float DROP_WAVE_SECONDS = 2.5f;
    private static final float DROP_WAVE_TRAVEL = 1.6f;
    private static final float DROP_WAVE_STRENGTH = 0.9f;
    private static final float[] SCREEN_CENTRE = {0.5f, 0.5f};
    private static final int BASS_GLOW = 0;
    private static final int MIDS_GLOW = 1;
    private static final int HIGHS_GLOW = 2;

    private final Bitmap frame = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    private final int[] pixels = new int[WIDTH * HEIGHT];
    private final Paint stretch = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Rect screen = new Rect();
    private final Runnable nextFrame = this::advance;
    private final float[] glowLevels = new float[HOMES.length];
    private final Wave[] waves = new Wave[MAX_WAVES];
    private int[] shown;
    private int[] blendFrom;
    private int[] blendTo;
    private long blendStartedAt;
    private Pulse pulse = Pulse.NONE;
    private float energy;
    private float kick;
    /** 0 for a slow song, 1 for a fast one, eased. */
    private float pace;
    /** 1 while the song has a clear beat to dance to, eased. */
    private float groove;
    /** 1 deep in a breakdown, eased. */
    private float tension;
    /** 1 right as the music surges, fading over SURGE_SECONDS. */
    private float surge;
    private float beat;
    private int lastWholeBeat;
    private int surgesSeen;
    private float flowSeconds;
    private long lastFrameAt;
    private boolean running;

    public AuroraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < waves.length; i++) {
            waves[i] = new Wave();
        }
    }

    /**
     * {@code palette} holds the base colour and one colour per glow, see LockBackdrop. While
     * running, the light blends over to the new colours.
     */
    void start(int[] palette) {
        if (running) {
            blendFrom = shown.clone();
            blendTo = palette;
            blendStartedAt = System.currentTimeMillis();
            return;
        }
        shown = palette.clone();
        blendFrom = palette;
        blendTo = palette;
        running = true;
        lastFrameAt = System.currentTimeMillis();
        advance();
    }

    void stop() {
        running = false;
        removeCallbacks(nextFrame);
    }

    void setPulse(Pulse pulse) {
        this.pulse = pulse;
        lastWholeBeat = (int) Math.floor(pulse.beat(System.currentTimeMillis()));
        surgesSeen = pulse.surges();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        screen.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(frame, null, screen, stretch);
    }

    private void advance() {
        if (!running) {
            return;
        }
        long now = System.currentTimeMillis();
        float elapsed = (now - lastFrameAt) / 1000f;
        lastFrameAt = now;
        followMusic(now, elapsed);
        flowSeconds += elapsed * (1f + vigour() * (MUSIC_FLOW * energy + KICK_PUSH * kick));
        blendPalette(now);
        render();
        invalidate();
        postDelayed(nextFrame, FRAME_MS);
    }

    private void followMusic(long now, float elapsed) {
        followBands();
        float tempo = pulse.tempo();
        pace += (paceOf(tempo) - pace) * TEMPO_EASING;
        groove += ((tempo > 0f ? 1f : 0f) - groove) * TEMPO_EASING;
        float breakdown = Math.max(0f, Math.min(1f, (1f - pulse.lift()) / TENSION_RANGE));
        tension += (breakdown - tension) * TENSION_EASING;
        kick *= CALM_KICK_FADE + (KICK_FADE - CALM_KICK_FADE) * pace;
        surge = Math.max(0f, surge - elapsed / SURGE_SECONDS);
        for (Wave wave : waves) {
            wave.age += elapsed;
        }
        followBeat(now, tempo);
    }

    private void followBands() {
        float total = 0f;
        for (int i = 0; i < glowLevels.length; i++) {
            float target = pulse.level(i);
            float easing = target > glowLevels[i] ? RISE_EASING[i] : FALL_EASING[i];
            glowLevels[i] += (target - glowLevels[i]) * easing;
            total += glowLevels[i];
        }
        energy += (total / glowLevels.length - energy) * ENERGY_EASING;
    }

    private void followBeat(long now, float tempo) {
        beat = pulse.beat(now);
        int whole = (int) Math.floor(beat);
        if (tempo > 0f && whole > lastWholeBeat) {
            hitBeat(whole, tempo);
        }
        lastWholeBeat = whole;
        if (pulse.surges() != surgesSeen) {
            surgesSeen = pulse.surges();
            surge();
        }
    }

    private void hitBeat(int whole, float tempo) {
        kick = whole % BEATS_PER_BAR == 0 ? 1f : BEAT_ACCENT;
        if (whole % BEATS_PER_WAVE == 0) {
            float[] from = {glowX(BASS_GLOW), glowY(BASS_GLOW)};
            sendWave(from, waveSeconds(tempo), WAVE_TRAVEL, WAVE_STRENGTH * kick * vigour(), BASS_GLOW);
        }
    }

    private void surge() {
        surge = 1f;
        kick = 1f;
        sendWave(SCREEN_CENTRE, DROP_WAVE_SECONDS, DROP_WAVE_TRAVEL, DROP_WAVE_STRENGTH, MIDS_GLOW);
    }

    /** A new wave replaces the oldest one; it shines in the colour of glow {@code colourOf}. */
    private void sendWave(float[] from, float seconds, float travel, float strength, int colourOf) {
        Wave oldest = waves[0];
        for (Wave wave : waves) {
            if (wave.progress() > oldest.progress()) {
                oldest = wave;
            }
        }
        oldest.x = from[0];
        oldest.y = from[1];
        oldest.age = 0f;
        oldest.seconds = seconds;
        oldest.travel = travel;
        oldest.strength = strength;
        oldest.colourOf = colourOf;
    }

    private static float waveSeconds(float bpm) {
        float seconds = WAVE_BEATS * 60f / bpm;
        return Math.max(SHORTEST_WAVE_SECONDS, Math.min(LONGEST_WAVE_SECONDS, seconds));
    }

    /** An unclear tempo counts as slow: better a calm picture than one that rushes ahead. */
    private static float paceOf(float bpm) {
        return Math.max(0f, Math.min(1f, (bpm - SLOW_BPM) / (FAST_BPM - SLOW_BPM)));
    }

    /** How much of the music show the song gets now: its pace, lifted for a while after a surge. */
    private float vigour() {
        return (CALM_VIGOUR + (1f - CALM_VIGOUR) * pace) * (1f + SURGE_BOOST * surge);
    }

    private void blendPalette(long now) {
        float progress = Math.min(1f, (now - blendStartedAt) / (float) PALETTE_BLEND_MS);
        for (int i = 0; i < shown.length; i++) {
            shown[i] = mix(blendFrom[i], blendTo[i], progress);
        }
    }

    /** Linear blend of two colours, channel by channel including alpha. */
    private static int mix(int start, int end, float progress) {
        return channel(start, end, progress, 24) | channel(start, end, progress, 16)
                | channel(start, end, progress, 8) | channel(start, end, progress, 0);
    }

    private static int channel(int start, int end, float progress, int shift) {
        int from = (start >>> shift) & 0xFF;
        int to = (end >>> shift) & 0xFF;
        return Math.round(from + (to - from) * progress) << shift;
    }

    private void render() {
        float vigour = vigour();
        float drawnIn = 1f - TENSION_SHRINK * tension;
        float dimmed = 1f - TENSION_DIM * tension;
        float[][] glows = new float[HOMES.length][4];
        for (int i = 0; i < glows.length; i++) {
            glows[i][0] = glowX(i);
            glows[i][1] = glowY(i);
            float level = glowLevels[i] * vigour;
            float flare = (i == BASS_GLOW ? kick * vigour : 0f) + SURGE_FLARE * surge;
            float radius = GLOW_RADIUS * (1f + MUSIC_SWELL[i] * level + KICK_SWELL * flare) * drawnIn;
            glows[i][2] = radius * radius;
            glows[i][3] = GLOW_STRENGTH * (1f + MUSIC_BRIGHTENING * level + KICK_FLARE * flare) * dimmed;
        }
        float ripple = CALM_RIPPLE + MUSIC_RIPPLE * glowLevels[MIDS_GLOW] * vigour;
        float ripplePhase = (float) (2 * Math.PI * flowSeconds / RIPPLE_SECONDS);
        float shimmer = SHIMMER * glowLevels[HIGHS_GLOW] * vigour;
        float shimmerPhase = (System.currentTimeMillis() % 100_000L) / 1000f * SHIMMER_SPEED;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                float u = x / (float) WIDTH;
                float v = y / (float) HEIGHT;
                // Bend the space a little, so the light looks liquid rather than round.
                float bentU = u + ripple * (float) Math.sin(v * RIPPLE_DENSITY + ripplePhase)
                        + shimmer * (float) Math.sin(v * SHIMMER_DENSITY + shimmerPhase);
                float bentV = v + ripple * (float) Math.sin(u * RIPPLE_DENSITY * 0.8f - ripplePhase * 1.3f)
                        + shimmer * (float) Math.sin(u * SHIMMER_DENSITY - shimmerPhase * 1.2f);
                pixels[y * WIDTH + x] = colourAt(bentU, bentV, glows);
            }
        }
        frame.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT);
    }

    private float glowX(int glow) {
        float orbit = REACH[0] * orbit(ORBIT_SECONDS[glow][0], ORBIT_PHASE[glow][0]);
        return HOMES[glow][0] + orbit + danceX(glow) * dancing();
    }

    private float glowY(int glow) {
        float orbit = REACH[1] * orbit(ORBIT_SECONDS[glow][1], ORBIT_PHASE[glow][1]);
        return HOMES[glow][1] + orbit + danceY(glow) * dancing();
    }

    /** The glows dance only to a clear beat, and hold still in a breakdown. */
    private float dancing() {
        return groove * (1f - tension);
    }

    /** The mids glow sways across once every two beats, the highs glow circles once a bar. */
    private float danceX(int glow) {
        if (glow == MIDS_GLOW) {
            return SWAY * (float) Math.sin(Math.PI * beat);
        }
        if (glow == HIGHS_GLOW) {
            return CIRCLE * (float) Math.cos(2 * Math.PI * beat / BEATS_PER_BAR);
        }
        return 0f;
    }

    /** The bass glow hops up between beats and lands on each one. */
    private float danceY(int glow) {
        if (glow == BASS_GLOW) {
            return -HOP * Math.abs((float) Math.sin(Math.PI * beat));
        }
        if (glow == HIGHS_GLOW) {
            return CIRCLE * (float) Math.sin(2 * Math.PI * beat / BEATS_PER_BAR);
        }
        return 0f;
    }

    /**
     * Each glow and wave is laid over the base like light through a screen: overlaps mix their
     * colours and grow brighter, but never burn out to white. The screen is treated as 16:9.
     */
    private int colourAt(float x, float y, float[][] glows) {
        int base = shown[0];
        float red = Color.red(base);
        float green = Color.green(base);
        float blue = Color.blue(base);
        for (int i = 0; i < glows.length; i++) {
            float dx = (x - glows[i][0]) * ASPECT;
            float dy = y - glows[i][1];
            float light = Math.min(1f, falloff((dx * dx + dy * dy) / glows[i][2]) * glows[i][3]);
            int colour = shown[i + 1];
            red = screen(red, Color.red(colour) * light);
            green = screen(green, Color.green(colour) * light);
            blue = screen(blue, Color.blue(colour) * light);
        }
        for (Wave wave : waves) {
            float light = wave.lightAt(x, y);
            if (light > 0f) {
                int colour = shown[wave.colourOf + 1];
                red = screen(red, Color.red(colour) * light);
                green = screen(green, Color.green(colour) * light);
                blue = screen(blue, Color.blue(colour) * light);
            }
        }
        return Color.rgb(Math.round(red), Math.round(green), Math.round(blue));
    }

    private static float screen(float below, float light) {
        return 255f - (255f - below) * (255f - light) / 255f;
    }

    /** 1 at the centre of a glow, easing to 0 at its rim, without the cost of exp(). */
    private static float falloff(float distanceSquared) {
        if (distanceSquared >= 1f) {
            return 0f;
        }
        float rest = 1f - distanceSquared;
        return rest * rest;
    }

    /** Swings between -1 and 1, once per period of flowing time. */
    private float orbit(float periodSeconds, float phase) {
        return (float) Math.sin(2 * Math.PI * flowSeconds / periodSeconds + phase);
    }

    /** A ring of light that grows from where it started and fades out by the end of its life. */
    private static final class Wave {
        float x;
        float y;
        float age = Float.MAX_VALUE;
        float seconds = 1f;
        float travel;
        float strength;
        int colourOf;

        float progress() {
            return age / seconds;
        }

        float lightAt(float atX, float atY) {
            float progress = progress();
            if (progress >= 1f) {
                return 0f;
            }
            float dx = (atX - x) * ASPECT;
            float dy = atY - y;
            float ringRadius = WAVE_START + progress * travel;
            float fromRing = ((float) Math.sqrt(dx * dx + dy * dy) - ringRadius) / WAVE_WIDTH;
            float fade = (1f - progress) * (1f - progress);
            return Math.min(1f, falloff(fromRing * fromRing) * fade * strength);
        }
    }
}
