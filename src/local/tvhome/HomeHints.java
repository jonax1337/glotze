package local.tvhome;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * A small notice next to the status pill about something worth fixing: no internet, or
 * storage running out. Selecting it opens the matching Fire TV settings.
 */
final class HomeHints implements NetworkIndicator.Listener {
    private static final long LOW_STORAGE_BYTES = 1L << 30;
    private static final float LOW_STORAGE_SHARE = 0.1f;
    /** Right after boot the network takes a few seconds; only a lasting outage is worth a notice. */
    private static final long OFFLINE_GRACE_MS = TimeUnit.SECONDS.toMillis(15);
    private static final String APPS_SETTINGS = "com.amazon.device.settings.action.APPLICATIONS";
    private static final String NETWORK_SETTINGS = "android.settings.WIFI_SETTINGS";

    private final TextView notice;
    private final Runnable confirmOffline = this::confirmOffline;
    private boolean offline;
    private Intent target;

    HomeHints(TextView notice, ActivityStarter starter) {
        this.notice = notice;
        Resources res = notice.getResources();
        float corner = res.getDimension(R.dimen.stage_pill_corner);
        notice.setBackground(SurfaceDrawable.focusableCard(notice.getContext(), corner));
        notice.setOnClickListener(view -> starter.start(target, notice.getText()));
    }

    /** Storage changes slowly, so it is checked whenever home comes back. */
    void refresh() {
        if (offline) {
            show(notice.getContext().getString(R.string.hint_offline), NETWORK_SETTINGS);
        } else if (isStorageLow()) {
            String free = Formatter.formatShortFileSize(notice.getContext(), dataStorage().getAvailableBytes());
            show(notice.getContext().getString(R.string.hint_storage_low, free), APPS_SETTINGS);
        } else {
            notice.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOnlineChanged(boolean online) {
        notice.removeCallbacks(confirmOffline);
        if (online) {
            offline = false;
            refresh();
        } else {
            notice.postDelayed(confirmOffline, OFFLINE_GRACE_MS);
        }
    }

    private void confirmOffline() {
        offline = true;
        refresh();
    }

    private void show(String text, String action) {
        target = new Intent(action);
        notice.setText(text);
        notice.setVisibility(View.VISIBLE);
    }

    private static boolean isStorageLow() {
        StatFs storage = dataStorage();
        long free = storage.getAvailableBytes();
        return free < LOW_STORAGE_BYTES || free < storage.getTotalBytes() * LOW_STORAGE_SHARE;
    }

    private static StatFs dataStorage() {
        return new StatFs(Environment.getDataDirectory().getPath());
    }
}
