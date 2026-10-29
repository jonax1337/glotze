package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Remembers which tiles are hidden or explicitly shown, and orders the rest: either as the
 * user placed them, or by how much each app is used. Tiles can also be given a category; once
 * any is a streaming or games app, the home screen groups the tiles by category instead of
 * showing one plain grid.
 */
final class TileArrangement {
    private static final String PREFERENCES = "arrangement";
    private static final String KEY_ORDER = "order";
    private static final String KEY_HIDDEN = "hidden";
    private static final String KEY_SHOWN = "shown";
    private static final String KEY_BY_USAGE = "order_by_usage";
    private static final String KEY_PINNED = "pinned";
    private static final String KEY_CATEGORY_PREFIX = "category.";
    private static final String ORDER_SEPARATOR = "\n";

    private final SharedPreferences preferences;
    private final UsageHistory usage;

    TileArrangement(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        usage = new UsageHistory(context);
    }

    List<AppTile> visibleInOrder(List<AppTile> tiles) {
        List<AppTile> visible = new ArrayList<>();
        for (AppTile tile : tiles) {
            if (!isHidden(tile)) {
                visible.add(tile);
            }
        }
        visible.sort(isOrderedByUsage() ? pinnedThenMostUsed() : savedOrderThenAlphabetical());
        return visible;
    }

    /**
     * The order in which the dock on the stage takes apps: the pinned ones first, as they stand in
     * the list; then the ones that have been used, the most used first and the most recent among
     * equals; then all others, as they stand in the list. Hidden apps are not in the list.
     */
    List<AppTile> inDockOrder(List<AppTile> orderedTiles) {
        Set<String> pinned = stringSet(KEY_PINNED);
        List<AppTile> pinnedTiles = new ArrayList<>();
        List<AppTile> used = new ArrayList<>();
        List<AppTile> unused = new ArrayList<>();
        for (AppTile tile : orderedTiles) {
            if (pinned.contains(tile.key)) {
                pinnedTiles.add(tile);
            } else if (usage.lastUsedAt(tile.key) > 0) {
                used.add(tile);
            } else {
                unused.add(tile);
            }
        }
        used.sort(usage.mostUsedFirst().thenComparing(usage.mostRecentFirst()));
        pinnedTiles.addAll(used);
        pinnedTiles.addAll(unused);
        return pinnedTiles;
    }

    /**
     * Splits already ordered tiles into the sections to show: one plain section while no tile has
     * a category, otherwise one per category in use, with uncategorized tiles under "other".
     */
    Map<TileCategory, List<AppTile>> sectionsOf(List<AppTile> orderedTiles) {
        Map<TileCategory, List<AppTile>> sections = new EnumMap<>(TileCategory.class);
        boolean grouped = isGrouped(orderedTiles);
        for (AppTile tile : orderedTiles) {
            TileCategory section = grouped ? sectionCategoryOf(tile) : TileCategory.NONE;
            sections.computeIfAbsent(section, category -> new ArrayList<>()).add(tile);
        }
        return sections;
    }

    TileCategory categoryOf(AppTile tile) {
        return TileCategory.fromStored(preferences.getString(KEY_CATEGORY_PREFIX + tile.key, ""));
    }

    void setCategory(AppTile tile, TileCategory category) {
        SharedPreferences.Editor edit = preferences.edit();
        if (category == TileCategory.NONE) {
            edit.remove(KEY_CATEGORY_PREFIX + tile.key);
        } else {
            edit.putString(KEY_CATEGORY_PREFIX + tile.key, category.name());
        }
        edit.apply();
    }

    /** Pinned apps lead while the order follows usage, so a favourite stays put on quiet days. */
    boolean isPinned(AppTile tile) {
        return stringSet(KEY_PINNED).contains(tile.key);
    }

    void togglePin(AppTile tile) {
        Set<String> pinned = stringSet(KEY_PINNED);
        if (!pinned.remove(tile.key)) {
            pinned.add(tile.key);
        }
        preferences.edit().putStringSet(KEY_PINNED, pinned).apply();
    }

    boolean isOrderedByUsage() {
        return preferences.getBoolean(KEY_BY_USAGE, false);
    }

    void toggleOrdering() {
        preferences.edit().putBoolean(KEY_BY_USAGE, !isOrderedByUsage()).apply();
    }

    void recordLaunch(AppTile tile) {
        usage.recordLaunch(tile.key);
    }

    /** Keeps the usage memory current, in either ordering, so switching to usage finds it ready. */
    void catchUpOnUsage() {
        usage.catchUp();
    }

    List<AppTile> hiddenAlphabetical(List<AppTile> tiles) {
        List<AppTile> hidden = new ArrayList<>();
        for (AppTile tile : tiles) {
            if (isHidden(tile)) {
                hidden.add(tile);
            }
        }
        hidden.sort(alphabetical());
        return hidden;
    }

    void hide(String key) {
        moveBetweenSets(key, KEY_SHOWN, KEY_HIDDEN);
    }

    void unhide(String key) {
        moveBetweenSets(key, KEY_HIDDEN, KEY_SHOWN);
    }

    /**
     * Remembers tiles as placed by hand: their order, and the category of the section each one ended up in.
     * The tiles of the dock come first in the order; they have no section of their own to be moved between.
     */
    void saveArrangement(List<AppTile> dock, Map<TileCategory, List<AppTile>> sections) {
        List<String> keys = new ArrayList<>();
        for (AppTile tile : dock) {
            keys.add(tile.key);
        }
        for (Map.Entry<TileCategory, List<AppTile>> section : sections.entrySet()) {
            for (AppTile tile : section.getValue()) {
                keys.add(tile.key);
                keepInSection(tile, section.getKey());
            }
        }
        preferences.edit().putString(KEY_ORDER, TextUtils.join(ORDER_SEPARATOR, keys)).apply();
    }

    /** "Other" alone does not group: it is where uncategorized tiles go, so choosing it adds nothing to sort by. */
    private boolean isGrouped(List<AppTile> tiles) {
        for (AppTile tile : tiles) {
            TileCategory category = categoryOf(tile);
            if (category == TileCategory.STREAMING || category == TileCategory.GAMES) {
                return true;
            }
        }
        return false;
    }

    /** The section a tile is shown in while grouped; tiles without a category belong to "other". */
    private TileCategory sectionCategoryOf(AppTile tile) {
        TileCategory category = categoryOf(tile);
        return category == TileCategory.NONE ? TileCategory.OTHER : category;
    }

    private void keepInSection(AppTile tile, TileCategory section) {
        if (section != TileCategory.NONE && sectionCategoryOf(tile) != section) {
            setCategory(tile, section);
        }
    }

    private boolean isHidden(AppTile tile) {
        if (stringSet(KEY_HIDDEN).contains(tile.key)) {
            return true;
        }
        return tile.isHiddenByDefault() && !stringSet(KEY_SHOWN).contains(tile.key);
    }

    private void moveBetweenSets(String key, String fromSet, String toSet) {
        Set<String> from = stringSet(fromSet);
        Set<String> to = stringSet(toSet);
        from.remove(key);
        to.add(key);
        preferences.edit().putStringSet(fromSet, from).putStringSet(toSet, to).apply();
    }

    private Set<String> stringSet(String preferenceKey) {
        // getStringSet's result must not be modified, so always hand out a copy.
        return new HashSet<>(preferences.getStringSet(preferenceKey, Collections.emptySet()));
    }

    private List<String> savedOrder() {
        String saved = preferences.getString(KEY_ORDER, "");
        return saved.isEmpty() ? Collections.emptyList() : Arrays.asList(saved.split(ORDER_SEPARATOR));
    }

    private Comparator<AppTile> pinnedThenMostUsed() {
        Set<String> pinned = stringSet(KEY_PINNED);
        return Comparator.comparing((AppTile tile) -> !pinned.contains(tile.key))
                .thenComparing(usage.mostUsedFirst())
                .thenComparing(savedOrderThenAlphabetical());
    }

    private Comparator<AppTile> savedOrderThenAlphabetical() {
        List<String> order = savedOrder();
        return Comparator.<AppTile>comparingInt(tile -> rank(order, tile)).thenComparing(alphabetical());
    }

    /** Tiles the user never placed (e.g. newly installed apps) sort after all placed ones. */
    private static int rank(List<String> order, AppTile tile) {
        int index = order.indexOf(tile.key);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private static Comparator<AppTile> alphabetical() {
        Collator collator = Collator.getInstance();
        return (a, b) -> collator.compare(a.label.toString(), b.label.toString());
    }
}
