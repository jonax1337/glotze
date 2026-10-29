package local.tvhome;

import java.util.Random;

/**
 * When the idle mascot blinks and what it does besides: irregular pauses, now and then a double
 * blink and, rarely, a wink or a look around. Two special motions never follow each other.
 */
final class BlinkRhythm {
    static final float MIN_PAUSE_SECONDS = 2.5f;
    static final float MAX_PAUSE_SECONDS = 7f;
    /** Chance of a wink or a look around, which are told apart by a coin toss. */
    static final float WINK_OR_LOOK_CHANCE = 0.05f;
    static final float DOUBLE_BLINK_CHANCE = 0.20f;

    private final Random random = new Random(System.nanoTime());
    private boolean lastWasSpecial;

    float nextPauseSeconds() {
        return MIN_PAUSE_SECONDS + random.nextFloat() * (MAX_PAUSE_SECONDS - MIN_PAUSE_SECONDS);
    }

    /** Looking around is offered only while the eyes are not busy following the focus. */
    GlotzeView.Motion nextMotion(boolean mayLookAround) {
        GlotzeView.Motion motion = lastWasSpecial ? GlotzeView.Motion.BLINK : draw(mayLookAround);
        lastWasSpecial = motion != GlotzeView.Motion.BLINK;
        return motion;
    }

    private GlotzeView.Motion draw(boolean mayLookAround) {
        float roll = random.nextFloat();
        if (roll < WINK_OR_LOOK_CHANCE) {
            return mayLookAround && random.nextBoolean() ? GlotzeView.Motion.LOOK_AROUND : GlotzeView.Motion.WINK;
        }
        return roll < WINK_OR_LOOK_CHANCE + DOUBLE_BLINK_CHANCE
                ? GlotzeView.Motion.DOUBLE_BLINK : GlotzeView.Motion.BLINK;
    }
}
