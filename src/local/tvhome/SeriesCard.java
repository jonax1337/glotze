package local.tvhome;

/**
 * What the home screen shows of a followed series. {@code fresh} marks a new episode of the last
 * 14 days that has not been opened yet; {@code subtitle} is empty while the episodes are unknown.
 */
final class SeriesCard {
    final long id;
    final String title;
    final String subtitle;
    final boolean fresh;
    /** May be null. */
    final String posterUrl;

    SeriesCard(long id, String title, String subtitle, boolean fresh, String posterUrl) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.fresh = fresh;
        this.posterUrl = posterUrl;
    }
}
