package local.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;

/** Which TV system the launcher runs on; it decides which settings pages exist and how they are named. */
enum Platform {
    FIRE_TV(R.string.platform_fire_tv),
    GOOGLE_TV(R.string.platform_google_tv),
    ANDROID_TV(R.string.platform_android_tv);

    /** Amazon's documented way to tell a Fire TV apart. */
    private static final String FIRE_TV_FEATURE = "amazon.hardware.fire_tv";
    /** Google TV has no feature flag of its own, only its launcher; it may be disabled to make room for ours. */
    private static final String GOOGLE_TV_LAUNCHER = "com.google.android.apps.tv.launcherx";

    final int nameRes;

    Platform(int nameRes) {
        this.nameRes = nameRes;
    }

    static Platform of(Context context) {
        PackageManager packages = context.getPackageManager();
        if (packages.hasSystemFeature(FIRE_TV_FEATURE)) {
            return FIRE_TV;
        }
        return isInstalled(packages, GOOGLE_TV_LAUNCHER) ? GOOGLE_TV : ANDROID_TV;
    }

    boolean isFireTv() {
        return this == FIRE_TV;
    }

    private static boolean isInstalled(PackageManager packages, String packageName) {
        try {
            packages.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
