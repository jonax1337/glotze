package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Finds the installed app a series is watched in: the one the user chose, else the streaming app
 * that carries the series' network ("Disney+", "Prime Video", ...). It asks the system for launchable
 * apps on every call, so it sees apps installed since, but loads no icons or banners: only labels,
 * which it remembers. Call it only when the user acts.
 */
final class AppResolver {
    // TV category first, so an app's TV entry wins over its phone-style one.
    private static final String[] LAUNCH_CATEGORIES = {
            Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
    private static final int MIN_LABEL_LENGTH_IN_NETWORK = 4;
    private static final int MIN_NETWORK_LENGTH_IN_LABEL = 3;

    private final String ownPackage;
    private final PackageManager packageManager;
    /** App labels by package name; a label is read once. */
    private final Map<String, String> labels = new HashMap<>();

    AppResolver(Context context) {
        this.ownPackage = context.getPackageName();
        this.packageManager = context.getPackageManager();
    }

    /** All launchable apps, alphabetically. */
    List<AppChoice> choices() {
        List<AppChoice> choices = new ArrayList<>();
        for (Map.Entry<String, String> app : installedLabels().entrySet()) {
            choices.add(new AppChoice(app.getValue(), app.getKey()));
        }
        Collections.sort(choices, new Comparator<AppChoice>() {
            @Override
            public int compare(AppChoice a, AppChoice b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return choices;
    }

    /** The launch intent of the chosen app if it is still installed, else of the app matching the network; null if there is none. */
    Intent launchIntent(String chosenPackage, String networkName) {
        Intent chosen = chosenPackage == null ? null : launchIntentOf(chosenPackage);
        if (chosen != null) {
            return chosen;
        }
        String networkPackage = packageForNetwork(installedLabels(), networkName);
        return networkPackage == null ? null : launchIntentOf(networkPackage);
    }

    private Intent launchIntentOf(String packageName) {
        Intent leanback = packageManager.getLeanbackLaunchIntentForPackage(packageName);
        return leanback != null ? leanback : packageManager.getLaunchIntentForPackage(packageName);
    }

    /** Label by package name for every launchable app except this launcher. */
    private Map<String, String> installedLabels() {
        Map<String, String> installed = new LinkedHashMap<>();
        for (String category : LAUNCH_CATEGORIES) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo match : packageManager.queryIntentActivities(query, 0)) {
                String packageName = match.activityInfo.packageName;
                if (!packageName.equals(ownPackage) && !installed.containsKey(packageName)) {
                    installed.put(packageName, labelOf(packageName, match));
                }
            }
        }
        return installed;
    }

    private String labelOf(String packageName, ResolveInfo match) {
        String label = labels.get(packageName);
        if (label == null) {
            label = match.loadLabel(packageManager).toString();
            labels.put(packageName, label);
        }
        return label;
    }

    /**
     * Prefers an app named exactly like the network, then one whose name holds it as whole words,
     * then one whose name the network holds as whole words ("Fox" is not found in "Dropbox").
     */
    private static String packageForNetwork(Map<String, String> labelsByPackage, String networkName) {
        String network = PlainText.lettersAndDigits(networkName);
        if (network.isEmpty()) {
            return null;
        }
        String found = firstMatch(labelsByPackage, networkName, (label, wanted) -> PlainText.lettersAndDigits(label).equals(network));
        if (found == null) {
            found = firstMatch(labelsByPackage, networkName,
                    (label, wanted) -> network.length() >= MIN_NETWORK_LENGTH_IN_LABEL && PlainText.containsWords(label, wanted));
        }
        if (found == null) {
            found = firstMatch(labelsByPackage, networkName,
                    (label, wanted) -> PlainText.lettersAndDigits(label).length() >= MIN_LABEL_LENGTH_IN_NETWORK
                            && PlainText.containsWords(wanted, label));
        }
        return found;
    }

    private static String firstMatch(Map<String, String> labelsByPackage, String networkName, BiPredicate<String, String> matches) {
        for (Map.Entry<String, String> app : labelsByPackage.entrySet()) {
            if (matches.test(app.getValue(), networkName)) {
                return app.getKey();
            }
        }
        return null;
    }
}
