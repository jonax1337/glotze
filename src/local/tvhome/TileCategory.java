package local.tvhome;

/**
 * The fixed rows the home screen can group tiles into. A tile without a category stays in the
 * plain grid; once any tile is in {@link #STREAMING} or {@link #GAMES}, the rest gather under
 * {@link #OTHER}.
 */
enum TileCategory {
    // The sections follow this order, and NONE comes last because it is no section of its own.
    STREAMING(R.string.home_category_streaming),
    GAMES(R.string.home_category_games),
    OTHER(R.string.home_category_other),
    NONE(R.string.home_category_none);

    final int labelRes;

    TileCategory(int labelRes) {
        this.labelRes = labelRes;
    }

    /** Unknown or empty text, e.g. a tile that was never categorized, means {@link #NONE}. */
    static TileCategory fromStored(String name) {
        for (TileCategory category : values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return NONE;
    }
}
