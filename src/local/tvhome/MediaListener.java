package local.tvhome;

import android.media.MediaMetadata;
import android.service.notification.NotificationListenerService;

/**
 * Exists because Android shows the playing media sessions to enabled notification listeners.
 * The launcher reads no notifications. Enabled once over ADB, see NowPlayingWidget.
 * While enabled it also watches what plays in other apps, so that series are recognised
 * even when the home screen is not open (see SeriesDetector).
 */
public final class MediaListener extends NotificationListenerService {
    /** The watcher reports to SeriesDetector by itself; nothing else needs to react here. */
    private static final MediaWatcher.Listener DETECTION_ONLY = new MediaWatcher.Listener() {
        @Override
        public void onPlaying(MediaMetadata track) {
        }

        @Override
        public void onStopped() {
        }
    };

    private MediaWatcher watcher;

    @Override
    public void onListenerConnected() {
        stopWatching();
        watcher = new MediaWatcher(this, DETECTION_ONLY);
        watcher.start();
    }

    @Override
    public void onListenerDisconnected() {
        stopWatching();
    }

    private void stopWatching() {
        if (watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }
}
