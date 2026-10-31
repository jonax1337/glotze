package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * The device's settings sections: Amazon's pages on a Fire TV, the standard Android pages elsewhere.
 * Fire OS guards several of them (picture and sound, preferences, account, accessibility) with a
 * permission only Amazon's own apps hold; those rows open the full settings overview instead,
 * which is open to everyone and leads there.
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

    /** A row and the pages that may show it, the first one the device has wins. */
    private static final class Entry {
        final RowLook look;
        final String[] actions;

        Entry(RowLook look, String... actions) {
            this.look = look;
            this.actions = actions;
        }

        Intent firstExisting(Context context) {
            for (String action : actions) {
                Intent page = new Intent(action);
                if (exists(context, page)) {
                    return page;
                }
            }
            return null;
        }
    }

    private static final RowLook OVERVIEW =
            new RowLook(R.string.settings_all, R.attr.iconSettings, R.color.tile_graphite);
    private static final RowLook NETWORK =
            new RowLook(R.string.settings_network, R.attr.iconNetwork, R.color.tile_blue);
    private static final RowLook CONTROLLERS =
            new RowLook(R.string.settings_controllers, R.attr.iconBluetooth, R.color.tile_sky);
    private static final RowLook DISPLAY =
            new RowLook(R.string.settings_display, R.attr.iconDisplay, R.color.tile_violet);
    private static final RowLook SOUNDBAR =
            new RowLook(R.string.settings_soundbar, R.attr.iconSpeaker, R.color.tile_purple);
    private static final RowLook APPS =
            new RowLook(R.string.settings_apps, R.attr.iconApps, R.color.tile_indigo);
    private static final RowLook PERMISSIONS =
            new RowLook(R.string.settings_permissions, R.attr.iconShield, R.color.tile_green);
    private static final RowLook NOTIFICATIONS =
            new RowLook(R.string.settings_notifications, R.attr.iconBell, R.color.tile_red);
    private static final RowLook PREFERENCES =
            new RowLook(R.string.settings_preferences, R.attr.iconTune, R.color.tile_graphite);
    private static final RowLook ACCESSIBILITY =
            new RowLook(R.string.settings_accessibility, R.attr.iconAccessibility, R.color.tile_teal);
    private static final RowLook DEVICE =
            new RowLook(R.string.settings_device, R.attr.iconInfo, R.color.tile_slate);
    private static final RowLook ACCOUNT =
            new RowLook(R.string.settings_account, R.attr.iconAccount, R.color.tile_orange);

    private static final String AMAZON_ACTION = "com.amazon.device.settings.action.";
    private static final String GOOGLE_TV_PAIRING = "com.google.android.intent.action.CONNECT_INPUT";
    /** Android answers an action that several pages share with its own chooser instead of a page. */
    private static final String CHOOSER_PACKAGE = "android";

    private static final Entry[] FIRE_TV_ENTRIES = {
            new Entry(NETWORK, Settings.ACTION_WIFI_SETTINGS),
            new Entry(CONTROLLERS, AMAZON_ACTION + "CONTROLLERS"),
            new Entry(DISPLAY, AMAZON_ACTION + "DISPLAY_AND_SOUNDS"),
            new Entry(SOUNDBAR, AMAZON_ACTION + "SOUNDBAR_SETTINGS"),
            new Entry(APPS, AMAZON_ACTION + "APPLICATIONS"),
            new Entry(PERMISSIONS, AMAZON_ACTION + "APPLICATION_PERMISSION_SETTINGS"),
            new Entry(NOTIFICATIONS, AMAZON_ACTION + "NOTIFICATION_SETTING"),
            new Entry(PREFERENCES, AMAZON_ACTION + "PREFERENCES"),
            new Entry(ACCESSIBILITY, AMAZON_ACTION + "ACCESSIBILITY"),
            new Entry(DEVICE, AMAZON_ACTION + "DEVICE"),
            new Entry(ACCOUNT, AMAZON_ACTION + "MY_ACCOUNT"),
    };

    private static final Entry[] ANDROID_TV_ENTRIES = {
            new Entry(NETWORK, Settings.ACTION_WIFI_SETTINGS),
            // Google TV has no Bluetooth page of its own, only the pairing of remotes and accessories.
            new Entry(CONTROLLERS, Settings.ACTION_BLUETOOTH_SETTINGS, GOOGLE_TV_PAIRING),
            // Google TV joins picture and sound on one page, reached only as the sound settings.
            new Entry(DISPLAY, Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS),
            new Entry(APPS, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS, Settings.ACTION_APPLICATION_SETTINGS),
            new Entry(ACCESSIBILITY, Settings.ACTION_ACCESSIBILITY_SETTINGS),
            new Entry(DEVICE, Settings.ACTION_DEVICE_INFO_SETTINGS),
            new Entry(ACCOUNT, Settings.ACTION_SYNC_SETTINGS),
    };

    private SystemSettings() {
    }

    /** The overview first, if the device has one, then every section that exists on this device. */
    static List<Section> available(Context context) {
        boolean fireTv = Platform.of(context).isFireTv();
        Intent overview = fireTv ? amazonOverview() : new Intent(Settings.ACTION_SETTINGS);
        boolean hasOverview = isOpenToUs(context, overview);
        List<Section> available = new ArrayList<>();
        if (hasOverview) {
            available.add(new Section(OVERVIEW, overview));
        }
        for (Entry entry : fireTv ? FIRE_TV_ENTRIES : ANDROID_TV_ENTRIES) {
            Intent direct = entry.firstExisting(context);
            if (direct == null) {
                continue;
            }
            if (isOpenToUs(context, direct)) {
                available.add(new Section(entry.look, direct));
            } else if (hasOverview) {
                available.add(new Section(entry.look, overview));
            }
        }
        return available;
    }

    /** Where the device keeps the apps' storage: a full disk is freed there. */
    static Intent storage(Context context) {
        return new Intent(Platform.of(context).isFireTv()
                ? AMAZON_ACTION + "APPLICATIONS" : Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
    }

    private static Intent amazonOverview() {
        return new Intent().setClassName(
                "com.amazon.tv.launcher", "com.amazon.tv.launcher.ui.MainSettingsActivity");
    }

    private static boolean exists(Context context, Intent intent) {
        ResolveInfo match = context.getPackageManager().resolveActivity(intent, 0);
        return match != null && !CHOOSER_PACKAGE.equals(match.activityInfo.packageName);
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
