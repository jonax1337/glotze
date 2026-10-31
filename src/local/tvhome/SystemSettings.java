package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The device's settings sections. Fire OS guards several of them (picture and sound,
 * preferences, account, accessibility) with a permission only Amazon's own apps hold; those
 * rows open Amazon's full settings overview instead, which is open to everyone and leads there.
 */
final class SystemSettings {
    static final class Section {
        final RowLook look;
        final Intent intent;

        private Section(RowLook look, Intent intent) {
            this.look = look;
            this.intent = intent;
        }
    }

    private static final class Entry {
        final RowLook look;
        final String action;

        Entry(int labelRes, int iconAttr, int colorRes, String action) {
            this.look = new RowLook(labelRes, iconAttr, colorRes);
            this.action = action;
        }
    }

    private static final RowLook OVERVIEW_LOOK =
            new RowLook(R.string.settings_all, R.attr.iconSettings, R.color.tile_graphite);
    private static final String AMAZON_ACTION = "com.amazon.device.settings.action.";

    private static final Entry[] ENTRIES = {
            new Entry(R.string.settings_network, R.attr.iconNetwork, R.color.tile_blue,
                    "android.settings.WIFI_SETTINGS"),
            new Entry(R.string.settings_controllers, R.attr.iconBluetooth, R.color.tile_sky,
                    AMAZON_ACTION + "CONTROLLERS"),
            new Entry(R.string.settings_display, R.attr.iconDisplay, R.color.tile_violet,
                    AMAZON_ACTION + "DISPLAY_AND_SOUNDS"),
            new Entry(R.string.settings_soundbar, R.attr.iconSpeaker, R.color.tile_purple,
                    AMAZON_ACTION + "SOUNDBAR_SETTINGS"),
            new Entry(R.string.settings_apps, R.attr.iconApps, R.color.tile_indigo,
                    AMAZON_ACTION + "APPLICATIONS"),
            new Entry(R.string.settings_permissions, R.attr.iconShield, R.color.tile_green,
                    AMAZON_ACTION + "APPLICATION_PERMISSION_SETTINGS"),
            new Entry(R.string.settings_notifications, R.attr.iconBell, R.color.tile_red,
                    AMAZON_ACTION + "NOTIFICATION_SETTING"),
            new Entry(R.string.settings_preferences, R.attr.iconTune, R.color.tile_graphite,
                    AMAZON_ACTION + "PREFERENCES"),
            new Entry(R.string.settings_accessibility, R.attr.iconAccessibility, R.color.tile_teal,
                    AMAZON_ACTION + "ACCESSIBILITY"),
            new Entry(R.string.settings_device, R.attr.iconInfo, R.color.tile_slate,
                    AMAZON_ACTION + "DEVICE"),
            new Entry(R.string.settings_account, R.attr.iconAccount, R.color.tile_orange,
                    AMAZON_ACTION + "MY_ACCOUNT"),
    };

    private SystemSettings() {
    }

    /** The overview first, if the device has one, then every section that exists on this device. */
    static List<Section> available(Context context) {
        Intent overview = new Intent().setClassName(
                "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity");
        boolean hasOverview = isOpenToUs(context, overview);
        List<Section> available = new ArrayList<>();
        if (hasOverview) {
            available.add(new Section(OVERVIEW_LOOK, overview));
        }
        for (Entry entry : ENTRIES) {
            Intent direct = new Intent(entry.action);
            if (isOpenToUs(context, direct)) {
                available.add(new Section(entry.look, direct));
            } else if (hasOverview && exists(context, direct)) {
                available.add(new Section(entry.look, overview));
            }
        }
        return available;
    }

    private static boolean exists(Context context, Intent intent) {
        return context.getPackageManager().resolveActivity(intent, 0) != null;
    }

    private static boolean isOpenToUs(Context context, Intent intent) {
        ResolveInfo match = context.getPackageManager().resolveActivity(intent, 0);
        if (match == null) {
            return false;
        }
        ActivityInfo activity = match.activityInfo;
        boolean permitted = activity.permission == null
                || context.checkSelfPermission(activity.permission) == PackageManager.PERMISSION_GRANTED;
        return activity.exported && permitted;
    }
}
