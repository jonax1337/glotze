package local.tvhome;

/** A show found on TVmaze. {@code premieredYear} is 0, {@code imageUrl}, {@code networkName} and {@code type} may be null. */
final class ShowHit {
    final long id;
    final String name;
    final int premieredYear;
    /** The medium poster. */
    final String imageUrl;
    final String networkName;
    /** TVmaze's show type, for example "Scripted", "News" or "Sports". */
    final String type;

    ShowHit(long id, String name, int premieredYear, String imageUrl, String networkName) {
        this(id, name, premieredYear, imageUrl, networkName, null);
    }

    ShowHit(long id, String name, int premieredYear, String imageUrl, String networkName, String type) {
        this.id = id;
        this.name = name;
        this.premieredYear = premieredYear;
        this.imageUrl = imageUrl;
        this.networkName = networkName;
        this.type = type;
    }
}
