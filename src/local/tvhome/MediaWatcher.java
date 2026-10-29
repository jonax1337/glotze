package local.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;

import java.util.List;
import java.util.Optional;

/**
 * Follows whatever app is playing (Spotify, YouTube Music, …) through its media session. Android
 * only shares media sessions with an enabled notification listener, granted once over ADB:
 * {@code settings put secure enabled_notification_listeners local.tvhome/local.tvhome.MediaListener}.
 */
final class MediaWatcher {
    interface Listener {
        /** A track is playing; called again when it changes. */
        void onPlaying(MediaMetadata track);

        void onStopped();
    }

    private static final String TAG = "MediaWatcher";
    private static final String[] ARTWORK_KEYS = {
            MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON};

    private final MediaSessionManager sessions;
    private final Context context;
    private final ComponentName access;
    private final Listener listener;
    private final MediaSessionManager.OnActiveSessionsChangedListener onSessionsChanged = this::follow;
    private final MediaController.Callback onPlaybackChanged = new MediaController.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            report();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            report();
        }
    };
    private MediaController followed;

    MediaWatcher(Context context, Listener listener) {
        this.context = context;
        this.sessions = context.getSystemService(MediaSessionManager.class);
        this.access = new ComponentName(context, MediaListener.class);
        this.listener = listener;
    }

    void start() {
        try {
            sessions.addOnActiveSessionsChangedListener(onSessionsChanged, access);
            follow(sessions.getActiveSessions(access));
        } catch (SecurityException e) {
            Log.w(TAG, "No notification access; grant it over ADB to see what is playing", e);
        }
    }

    void stop() {
        sessions.removeOnActiveSessionsChangedListener(onSessionsChanged);
        unfollow();
    }

    static Optional<Bitmap> artworkOf(MediaMetadata track) {
        for (String key : ARTWORK_KEYS) {
            Bitmap artwork = track.getBitmap(key);
            if (artwork != null) {
                return Optional.of(artwork);
            }
        }
        return Optional.empty();
    }

    /** Follows the first session that is playing, so a paused app in the background does not win. */
    private void follow(List<MediaController> active) {
        unfollow();
        for (MediaController controller : active) {
            if (isPlaying(controller)) {
                followed = controller;
                controller.registerCallback(onPlaybackChanged);
                break;
            }
        }
        report();
    }

    private void unfollow() {
        if (followed != null) {
            followed.unregisterCallback(onPlaybackChanged);
            followed = null;
        }
    }

    private void report() {
        MediaMetadata track = followed == null ? null : followed.getMetadata();
        if (track != null && isPlaying(followed)) {
            SeriesDetector.onMetadata(context, followed.getPackageName(), track, true);
            listener.onPlaying(track);
        } else {
            listener.onStopped();
        }
    }

    private static boolean isPlaying(MediaController controller) {
        PlaybackState state = controller.getPlaybackState();
        return state != null && state.getState() == PlaybackState.STATE_PLAYING;
    }
}
