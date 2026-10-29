package local.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.view.animation.LinearInterpolator;
import java.util.EnumMap;

/**
 * Glotze, the mascot, as a small animated figure. The motions follow assets/glotze/animations.json.
 * Nothing moves while the view is hidden or detached, or when the system has animations switched
 * off; the figure is then drawn at rest.
 */
public final class GlotzeView extends View {
    public enum Motion {
        IDLE, BLINK, DOUBLE_BLINK, WINK, LOOK_AROUND, BOUNCE, DOZE, APPEAR
    }

    private static final float ASPECT = GlotzeFigure.ART_WIDTH / GlotzeFigure.ART_HEIGHT;
    // Artwork units: how far the eyes follow the gaze, and how the figure looks when asleep.
    private static final float LOOK_RANGE = 18f;
    private static final float ASLEEP_EYE_SCALE = 0.12f;
    private static final float ASLEEP_TILT = -3f;
    // How fast the gaze and the sleepiness follow their targets: about 0.4 s to arrive.
    private static final float FOLLOW_RATE = 18f;
    private static final float ARRIVED_DISTANCE = 0.001f;
    private static final float ARRIVED_SPEED = 0.01f;
    private static final float NANOS_PER_SECOND = 1e9f;

    private static final Score REST = new Score(1);
    private static final EnumMap<Motion, Score> SCORES = new EnumMap<>(Motion.class);

    static {
        SCORES.put(Motion.IDLE, new Score(6400)
                .bodyDy(Track.of(0, 0, .25f, -3, .5f, 0, .75f, 2, 1, 0))
                .eyesScaleY(Track.of(0, 1, .56f, 1, .585f, .07f, .615f, 1, 1, 1)));
        SCORES.put(Motion.BLINK, new Score(4800)
                .eyesScaleY(Track.of(0, 1, .4f, 1, .42f, .07f, .45f, 1, 1, 1)));
        SCORES.put(Motion.DOUBLE_BLINK, new Score(6200)
                .eyesScaleY(Track.of(0, 1, .4f, 1, .418f, .07f, .44f, 1,
                        .48f, 1, .498f, .07f, .52f, 1, 1, 1)));
        SCORES.put(Motion.WINK, new Score(4400)
                .rightEyeScaleY(Track.of(0, 1, .35f, 1, .385f, .08f, .5f, .08f, .53f, 1, 1, 1)));
        SCORES.put(Motion.LOOK_AROUND, new Score(6000)
                .eyesShiftX(Track.of(0, 0, .12f, 0, .3f, 16, .44f, 16, .62f, -18, .8f, -18, 1, 0))
                .eyesShiftY(Track.of(0, 0, .12f, 0, .3f, -4, .44f, -4, .62f, 3, .8f, 3, 1, 0)));
        SCORES.put(Motion.BOUNCE, new Score(2800)
                .bodyDy(Track.of(0, 0, .2f, 0, .34f, -15, .48f, 0, .58f, -7, .67f, 0, 1, 0)));
        SCORES.put(Motion.DOZE, new Score(7200)
                .bodyRotation(Track.of(0, 0, .18f, 0, .45f, -3, .78f, -3, 1, 0))
                .eyesScaleY(Track.of(0, 1, .16f, 1, .38f, .28f, .5f, .12f, .74f, .12f,
                        .86f, .35f, 1, 1)));
        SCORES.put(Motion.APPEAR, new Score(700)
                .bodyScale(Track.of(0, .82f, .6f, 1.035f, .82f, .985f, 1, 1))
                .bodyOpacity(Track.of(0, 0, .5f, 1, 1, 1)));
    }

    private final GlotzeFigure figure;
    /** What is on screen; only replaced when a new pose would look different, see {@link #refresh}. */
    private final GlotzeFigure.Pose pose = new GlotzeFigure.Pose();
    private final GlotzeFigure.Pose nextPose = new GlotzeFigure.Pose();
    private final ValueAnimator clock = ValueAnimator.ofFloat(0f, 1f);
    private final Follower gaze = new Follower();
    private final Follower sleep = new Follower();
    private final Choreographer.FrameCallback following = this::followTargets;

    private Score requested = REST;
    private boolean wanted;
    private boolean looping;
    private Score shown = REST;
    private float fraction;
    private boolean followScheduled;
    private long followedAtNanos;

    public GlotzeView(Context context) {
        this(context, null);
    }

    public GlotzeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources resources = getResources();
        figure = new GlotzeFigure(resources.getColor(R.color.glotze_lime, null),
                resources.getColor(R.color.glotze_ink, null));
        if (getContentDescription() == null) {
            setContentDescription(context.getString(R.string.mascot_description));
        }
        clock.setInterpolator(new LinearInterpolator());
        clock.addUpdateListener(animation -> advance(animation.getAnimatedFraction()));
    }

    /**
     * Plays the motion once and returns to rest. If the view cannot animate right now, the motion
     * starts as soon as it can.
     */
    public void play(Motion motion) {
        start(motion, false);
    }

    /** Repeats the motion until {@link #stop()} or the next call of {@link #play} or {@link #loop}. */
    public void loop(Motion motion) {
        start(motion, true);
    }

    /** Ends the current motion; the figure returns to rest. */
    public void stop() {
        wanted = false;
        stopMotion();
    }

    /** Moves both eyes towards one side, from -1 (left) to +1 (right); other values are clamped. */
    public void lookAt(float horizontal) {
        retarget(Math.max(-1f, Math.min(1f, horizontal)), sleep.target);
    }

    /** Lets the figure nod off with closed eyes, or wakes it. Motions keep playing meanwhile. */
    public void setAsleep(boolean asleep) {
        retarget(gaze.target, asleep ? 1f : 0f);
    }

    private void start(Motion motion, boolean repeat) {
        requested = SCORES.get(motion);
        looping = repeat;
        wanted = true;
        stopMotion();
        sync();
    }

    /** Runs the requested motion if the view can be seen, otherwise freezes everything. */
    private void sync() {
        if (!canAnimate()) {
            halt();
        } else if (wanted && !clock.isStarted()) {
            begin();
        }
    }

    private void begin() {
        shown = requested;
        fraction = 0f;
        clock.setDuration(requested.durationMs);
        clock.setRepeatCount(looping ? ValueAnimator.INFINITE : 0);
        clock.start();
        refresh();
    }

    private void stopMotion() {
        clock.cancel();
        shown = REST;
        refresh();
    }

    private void halt() {
        stopMotion();
        snapToTargets();
    }

    private void advance(float progress) {
        fraction = progress;
        if (progress >= 1f && !looping) {
            wanted = false;
            shown = REST;
        }
        refresh();
    }

    /** A new target bends the course the gaze is already on, so quick focus changes stay smooth. */
    private void retarget(float newGaze, float newSleep) {
        if (newGaze == gaze.target && newSleep == sleep.target) {
            return;
        }
        gaze.target = newGaze;
        sleep.target = newSleep;
        if (canAnimate()) {
            scheduleFollowing();
        } else {
            snapToTargets();
        }
    }

    private void scheduleFollowing() {
        if (!followScheduled) {
            followScheduled = true;
            followedAtNanos = 0;
            Choreographer.getInstance().postFrameCallback(following);
        }
    }

    private void followTargets(long frameTimeNanos) {
        float seconds = followedAtNanos == 0 ? 0f : (frameTimeNanos - followedAtNanos) / NANOS_PER_SECOND;
        followedAtNanos = frameTimeNanos;
        gaze.follow(seconds);
        sleep.follow(seconds);
        refresh();
        if (gaze.hasArrived() && sleep.hasArrived()) {
            snapToTargets();
        } else {
            Choreographer.getInstance().postFrameCallback(following);
        }
    }

    private void snapToTargets() {
        Choreographer.getInstance().removeFrameCallback(following);
        followScheduled = false;
        gaze.snap();
        sleep.snap();
        refresh();
    }

    private boolean canAnimate() {
        return isAttachedToWindow() && isShown() && getWindowVisibility() == VISIBLE
                && animationsEnabled();
    }

    private boolean animationsEnabled() {
        return Settings.Global.getFloat(getContext().getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        sync();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        halt();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        // Also runs from the View constructor, before this class has been set up.
        if (isAttachedToWindow()) {
            sync();
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        sync();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width;
        int height;
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.EXACTLY
                && MeasureSpec.getMode(widthSpec) != MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightSpec);
            width = Math.round(height * ASPECT);
        } else {
            width = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED
                    ? getResources().getDimensionPixelSize(R.dimen.mascot_default_width)
                    : MeasureSpec.getSize(widthSpec);
            height = Math.round(width / ASPECT);
        }
        setMeasuredDimension(resolveSize(width, widthSpec), resolveSize(height, heightSpec));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        figure.fit(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        figure.draw(canvas, pose);
    }

    /**
     * Redraws only when the figure would look different. The idle motion moves the body by under
     * two pixels, so most frames of the clock change nothing and are not worth a pass over the
     * whole window.
     */
    private void refresh() {
        composePose(nextPose);
        if (nextPose.differsVisiblyFrom(pose)) {
            pose.copyFrom(nextPose);
            invalidate();
        }
    }

    /** The running motion, with the gaze and the sleepiness laid over it. */
    private void composePose(GlotzeFigure.Pose target) {
        shown.writeTo(target, fraction);
        target.eyesDx += gaze.value * LOOK_RANGE;
        float lid = 1f - sleep.value * (1f - ASLEEP_EYE_SCALE);
        target.leftEyeScaleY = Math.min(target.leftEyeScaleY, lid);
        target.rightEyeScaleY = Math.min(target.rightEyeScaleY, lid);
        target.rotation += sleep.value * ASLEEP_TILT;
    }

    /**
     * A value that glides to its target like a critically damped spring: it never overshoots, and
     * a new target changes its course without a jolt, because position and speed carry over.
     */
    private static final class Follower {
        float value;
        float target;
        private float speed;

        void follow(float seconds) {
            float offset = value - target;
            float carried = speed + FOLLOW_RATE * offset;
            float decay = (float) Math.exp(-FOLLOW_RATE * seconds);
            value = target + (offset + carried * seconds) * decay;
            speed = (speed - FOLLOW_RATE * carried * seconds) * decay;
        }

        boolean hasArrived() {
            return Math.abs(value - target) < ARRIVED_DISTANCE && Math.abs(speed) < ARRIVED_SPEED;
        }

        void snap() {
            value = target;
            speed = 0f;
        }
    }

    /** One motion: its length and, per property of the pose, how it changes over that time. */
    private static final class Score {
        final long durationMs;
        private Track bodyDy = Track.flat(0f);
        private Track bodyRotation = Track.flat(0f);
        private Track bodyScale = Track.flat(1f);
        private Track bodyOpacity = Track.flat(1f);
        private Track leftEyeScaleY = Track.flat(1f);
        private Track rightEyeScaleY = Track.flat(1f);
        private Track eyesShiftX = Track.flat(0f);
        private Track eyesShiftY = Track.flat(0f);

        Score(long durationMs) {
            this.durationMs = durationMs;
        }

        Score bodyDy(Track track) {
            bodyDy = track;
            return this;
        }

        Score bodyRotation(Track track) {
            bodyRotation = track;
            return this;
        }

        Score bodyScale(Track track) {
            bodyScale = track;
            return this;
        }

        Score bodyOpacity(Track track) {
            bodyOpacity = track;
            return this;
        }

        Score eyesScaleY(Track track) {
            leftEyeScaleY = track;
            rightEyeScaleY = track;
            return this;
        }

        Score rightEyeScaleY(Track track) {
            rightEyeScaleY = track;
            return this;
        }

        Score eyesShiftX(Track track) {
            eyesShiftX = track;
            return this;
        }

        Score eyesShiftY(Track track) {
            eyesShiftY = track;
            return this;
        }

        void writeTo(GlotzeFigure.Pose pose, float fraction) {
            pose.dy = bodyDy.at(fraction);
            pose.rotation = bodyRotation.at(fraction);
            pose.scale = bodyScale.at(fraction);
            pose.opacity = bodyOpacity.at(fraction);
            pose.leftEyeScaleY = leftEyeScaleY.at(fraction);
            pose.rightEyeScaleY = rightEyeScaleY.at(fraction);
            pose.eyesDx = eyesShiftX.at(fraction);
            pose.eyesDy = eyesShiftY.at(fraction);
        }
    }

    /** A value over one cycle, linear between keyframes like SMIL's keyTimes and values. */
    private static final class Track {
        private final float[] times;
        private final float[] values;

        private Track(float[] times, float[] values) {
            this.times = times;
            this.values = values;
        }

        /** Keyframes as alternating time (0..1) and value, starting at time 0 and ending at 1. */
        static Track of(float... keyframes) {
            float[] times = new float[keyframes.length / 2];
            float[] values = new float[times.length];
            for (int i = 0; i < times.length; i++) {
                times[i] = keyframes[2 * i];
                values[i] = keyframes[2 * i + 1];
            }
            return new Track(times, values);
        }

        static Track flat(float value) {
            return of(0f, value, 1f, value);
        }

        float at(float fraction) {
            int next = 1;
            while (next < times.length - 1 && fraction > times[next]) {
                next++;
            }
            float span = times[next] - times[next - 1];
            float progress = (fraction - times[next - 1]) / span;
            return values[next - 1] + (values[next] - values[next - 1]) * progress;
        }
    }
}
