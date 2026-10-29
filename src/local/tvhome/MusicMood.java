package local.tvhome;

import android.content.Context;
import android.media.MediaMetadata;

/**
 * While music plays, the backdrop glows breathe with it in the chosen theme's colours: one glow
 * with the bass, one with the mids, one with the highs.
 */
final class MusicMood implements MediaWatcher.Listener {
    interface Listener {
        void onMoodChanged();
    }

    private final MediaWatcher watcher;
    private final SoundLevel sound = new SoundLevel();
    private final Listener listener;
    private boolean started;
    private boolean playing;
    private String playingTitle = "";

    MusicMood(Context context, Listener listener) {
        this.watcher = new MediaWatcher(context, this);
        this.listener = listener;
    }

    void start() {
        if (!started) {
            started = true;
            watcher.start();
        }
    }

    void stop() {
        if (started) {
            started = false;
            watcher.stop();
            onStopped();
        }
    }

    boolean isPlaying() {
        return playing;
    }

    Pulse pulse() {
        return sound;
    }

    /** A new track starts the listening afresh, so nothing of the last song carries over. */
    @Override
    public void onPlaying(MediaMetadata track) {
        String title = titleOf(track);
        if (!playing) {
            playing = true;
            sound.start();
            listener.onMoodChanged();
        } else if (!title.equals(playingTitle)) {
            sound.startNewSong();
        }
        playingTitle = title;
    }

    private static String titleOf(MediaMetadata track) {
        return track.getString(MediaMetadata.METADATA_KEY_ARTIST) + " – "
                + track.getString(MediaMetadata.METADATA_KEY_TITLE);
    }

    @Override
    public void onStopped() {
        if (playing) {
            playing = false;
            sound.stop();
            listener.onMoodChanged();
        }
    }
}
