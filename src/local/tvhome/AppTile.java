package local.tvhome;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

final class AppTile {
    private static final int LAUNCH_FLAGS =
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;

    /** Stable identity: the package name. */
    final String key;
    final CharSequence label;
    final Drawable artwork;
    final boolean hasBanner;
    final Intent launchIntent;
    private final boolean hiddenByDefault;

    private AppTile(ActivityInfo activity, PackageManager packageManager, String category) {
        Drawable banner = activity.loadBanner(packageManager);
        this.key = activity.packageName;
        this.label = activity.loadLabel(packageManager);
        this.hasBanner = banner != null;
        this.artwork = hasBanner ? banner : activity.loadIcon(packageManager);
        this.launchIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setClassName(activity.packageName, activity.name)
                .addFlags(LAUNCH_FLAGS);
        this.hiddenByDefault = isPhoneStyleSystemApp(activity, category);
    }

    static AppTile forActivity(ActivityInfo activity, PackageManager packageManager, String category) {
        return new AppTile(activity, packageManager, category);
    }

    boolean isHiddenByDefault() {
        return hiddenByDefault;
    }

    /** System utilities without a TV banner (downloads, browser, ...) stay out of the way until asked for. */
    private static boolean isPhoneStyleSystemApp(ActivityInfo activity, String category) {
        boolean systemApp = (activity.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        boolean tvApp = Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category);
        return systemApp && !tvApp;
    }
}
