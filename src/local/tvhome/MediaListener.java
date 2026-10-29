package local.tvhome;

import android.service.notification.NotificationListenerService;

/**
 * Exists because Android shows the playing media sessions only to enabled notification listeners.
 * The launcher reads no notifications. Enabled once over ADB, see NowPlayingWidget and MediaWatcher.
 */
public final class MediaListener extends NotificationListenerService {
}
