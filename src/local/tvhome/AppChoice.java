package local.tvhome;

/** An installed app the user can pick as the place where a series is watched. */
final class AppChoice {
    final String label;
    final String packageName;

    AppChoice(String label, String packageName) {
        this.label = label;
        this.packageName = packageName;
    }
}
