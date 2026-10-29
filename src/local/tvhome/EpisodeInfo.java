package local.tvhome;

/** One episode of a show as TVmaze announces it; {@code airMillis} is 0 while the broadcast time is unknown. */
final class EpisodeInfo {
    final int season;
    final int number;
    final String name;
    final long airMillis;

    EpisodeInfo(int season, int number, String name, long airMillis) {
        this.season = season;
        this.number = number;
        this.name = name;
        this.airMillis = airMillis;
    }
}
