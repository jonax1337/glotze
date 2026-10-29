package local.tvhome;

/** A followed series as {@link SeriesStore} keeps it. Nullable: imageUrl, networkName, next, previous, launchPackage. */
final class Series {
    long id;
    String name;
    String imageUrl;
    String networkName;
    EpisodeInfo next;
    EpisodeInfo previous;
    long addedAt;
    /** When playback of this series was last recognised or the series was last opened from the shelf. */
    long lastDetectedAt;
    /** Air time of the latest episode the user has opened. */
    long acknowledgedAirMillis;
    /** The app the user chose to watch this series in. */
    String launchPackage;
    /** True if it was added by recognising playback rather than by a search. */
    boolean auto;
    /** When TVmaze was last asked about the episodes; 0 if never. */
    long syncedAt;

    Series(long id, String name, long addedAt) {
        this.id = id;
        this.name = name;
        this.addedAt = addedAt;
    }
}
