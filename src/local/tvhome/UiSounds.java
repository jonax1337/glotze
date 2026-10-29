package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;

/** The launcher's own soft navigation and selection sounds, which the user can switch off. */
final class UiSounds {
    private static final String PREFERENCES = "ui";
    private static final String KEY_ENABLED = "sounds_enabled";
    private static final float VOLUME = 0.55f;
    private static final float NIGHT_VOLUME = 0.2f;
    private static final int MAX_STREAMS = 3;
    private static final int NORMAL_PRIORITY = 1;
    private static final int NO_LOOP = 0;
    private static final float NORMAL_RATE = 1f;

    private final SharedPreferences preferences;
    private final NightMode nightMode;
    private final SoundPool pool;
    private final int focusSound;
    private final int selectSound;

    UiSounds(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        nightMode = new NightMode(context);
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(MAX_STREAMS).setAudioAttributes(attributes).build();
        focusSound = pool.load(context, R.raw.focus, NORMAL_PRIORITY);
        selectSound = pool.load(context, R.raw.select, NORMAL_PRIORITY);
    }

    void playFocus() {
        play(focusSound);
    }

    void playSelect() {
        play(selectSound);
    }

    boolean areEnabled() {
        return preferences.getBoolean(KEY_ENABLED, true);
    }

    void toggle() {
        preferences.edit().putBoolean(KEY_ENABLED, !areEnabled()).apply();
    }

    void release() {
        pool.release();
    }

    private void play(int sound) {
        if (areEnabled()) {
            float volume = nightMode.isActive() ? NIGHT_VOLUME : VOLUME;
            pool.play(sound, volume, volume, NORMAL_PRIORITY, NO_LOOP, NORMAL_RATE);
        }
    }
}
