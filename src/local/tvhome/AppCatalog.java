package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AppCatalog {
    // TV category first, so an app's TV banner wins over its phone-style icon.
    private static final String[] LAUNCH_CATEGORIES = {
            Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};

    private final String ownPackage;
    private final PackageManager packageManager;

    AppCatalog(Context context) {
        this.ownPackage = context.getPackageName();
        this.packageManager = context.getPackageManager();
    }

    /** One tile per installed app, excluding this launcher. */
    List<AppTile> loadApps() {
        Map<String, AppTile> appsByPackage = new LinkedHashMap<>();
        for (String category : LAUNCH_CATEGORIES) {
            for (ActivityInfo activity : launchableActivities(category)) {
                if (isUnlistedOtherApp(activity, appsByPackage)) {
                    appsByPackage.put(activity.packageName, AppTile.forActivity(activity, packageManager, category));
                }
            }
        }
        return new ArrayList<>(appsByPackage.values());
    }

    private List<ActivityInfo> launchableActivities(String category) {
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
        List<ActivityInfo> activities = new ArrayList<>();
        for (ResolveInfo match : packageManager.queryIntentActivities(query, 0)) {
            activities.add(match.activityInfo);
        }
        return activities;
    }

    private boolean isUnlistedOtherApp(ActivityInfo activity, Map<String, AppTile> listed) {
        return !activity.packageName.equals(ownPackage) && !listed.containsKey(activity.packageName);
    }
}
