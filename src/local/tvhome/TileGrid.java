package local.tvhome;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lays the tiles out in rows of {@link #COLUMNS}, grouped under headings when they have
 * categories, and keeps views and tiles in step. The shelf above, see {@link HomeShelf}, has
 * its own tiles; the ones here are the arrangement.
 */
final class TileGrid {
    static final int COLUMNS = 6;

    interface Listener {
        void onTileClicked(AppTile tile, View source);

        void onTileLongPressed(AppTile tile);
    }

    private final LinearLayout rows;
    private final LayoutInflater inflater;
    private final Fonts fonts;
    private final Listener listener;
    /** The arrangeable tiles, section after section; {@link #sectionSizes} tells where each section ends. */
    private final List<AppTile> tiles = new ArrayList<>();
    private final List<TileView> views = new ArrayList<>();
    private final Map<TileCategory, Integer> sectionSizes = new LinkedHashMap<>();
    private boolean entranceShown;

    /** Finds the sections inside the scrolling home content. */
    TileGrid(View home, Fonts fonts, Listener listener) {
        this.rows = home.findViewById(R.id.tile_rows);
        this.inflater = LayoutInflater.from(home.getContext());
        this.fonts = fonts;
        this.listener = listener;
    }

    /** Makes the cells of a row share its width by weight, one share per column, filled or not. */
    static void alignToColumns(LinearLayout row) {
        row.setWeightSum(COLUMNS);
    }

    /** Shows the tiles; focus stays on the same app, even if a new order has moved it. */
    void show(Map<TileCategory, List<AppTile>> newSections) {
        List<AppTile> newTiles = flatten(newSections);
        if (isShowing(newTiles, sizesOf(newSections))) {
            bindAll(newTiles);
            return;
        }
        View focused = rows.findFocus();
        String focusedKey = contains(focused) ? tileOf(focused).key : "";
        int formerIndex = views.indexOf(focused);
        rebuild(newSections);
        if (!focusedKey.isEmpty()) {
            focus(indexToRefocus(focusedKey, formerIndex));
        }
        playEntranceOnce();
    }

    void swap(int first, int second) {
        Collections.swap(tiles, first, second);
        views.get(first).bind(tiles.get(first));
        views.get(second).bind(tiles.get(second));
    }

    /**
     * The arrangeable tile that lies next to this one in the given focus direction (a
     * {@code View.FOCUS_*} constant), or -1 if there is none. Neighbours are looked up inside the
     * section of the tile, row by row from the section's start, so a move never carries a tile
     * into another category.
     */
    int neighbourIndex(int index, int focusDirection) {
        int sectionStart = 0;
        for (int sectionSize : sectionSizes.values()) {
            int sectionEnd = sectionStart + sectionSize;
            if (index < sectionEnd) {
                int neighbour = index + stepTowards(focusDirection);
                return neighbour != index && neighbour >= sectionStart && neighbour < sectionEnd ? neighbour : -1;
            }
            sectionStart = sectionEnd;
        }
        return -1;
    }

    /** Focuses the tile at this place among the arrangeable tiles. */
    void focus(int index) {
        if (index >= 0 && index < views.size()) {
            views.get(index).requestFocus();
        }
    }

    /** Focuses the tile of this app, if it is among the arrangeable tiles. */
    void focusApp(String key) {
        focus(indexOfKey(tiles, key));
    }

    void startWiggleAt(int index) {
        views.get(index).startWiggle();
    }

    void stopWiggleAt(int index) {
        views.get(index).stopWiggle();
    }

    /** Where the focus is among the arrangeable tiles, or -1 if it is elsewhere. */
    int focusedIndex() {
        return views.indexOf(rows.findFocus());
    }

    boolean contains(View view) {
        return views.contains(view);
    }

    AppTile tileOf(View tileView) {
        return tiles.get(views.indexOf(tileView));
    }

    /** The tiles of the first row, where focus enters the grid from the shelf and leaves it again. */
    List<View> firstRow() {
        int firstSectionSize = sectionSizes.isEmpty() ? 0 : sectionSizes.values().iterator().next();
        return Collections.unmodifiableList(views.subList(0, Math.min(COLUMNS, firstSectionSize)));
    }

    /** The arrangeable tiles as they are placed now, grouped like the sections they were shown in. */
    Map<TileCategory, List<AppTile>> arranged() {
        Map<TileCategory, List<AppTile>> sections = new LinkedHashMap<>();
        int start = 0;
        for (Map.Entry<TileCategory, Integer> section : sectionSizes.entrySet()) {
            int end = start + section.getValue();
            sections.put(section.getKey(), new ArrayList<>(tiles.subList(start, end)));
            start = end;
        }
        return sections;
    }

    private static int stepTowards(int focusDirection) {
        switch (focusDirection) {
            case View.FOCUS_LEFT:
                return -1;
            case View.FOCUS_RIGHT:
                return 1;
            case View.FOCUS_UP:
                return -COLUMNS;
            case View.FOCUS_DOWN:
                return COLUMNS;
            default:
                return 0;
        }
    }

    private boolean isShowing(List<AppTile> newTiles, Map<TileCategory, Integer> newSizes) {
        return sectionSizes.equals(newSizes) && haveSameKeysInOrder(tiles, newTiles);
    }

    private static boolean haveSameKeysInOrder(List<AppTile> shown, List<AppTile> other) {
        if (other.size() != shown.size()) {
            return false;
        }
        for (int i = 0; i < shown.size(); i++) {
            if (!shown.get(i).key.equals(other.get(i).key)) {
                return false;
            }
        }
        return true;
    }

    private static List<AppTile> flatten(Map<TileCategory, List<AppTile>> sections) {
        List<AppTile> flat = new ArrayList<>();
        for (List<AppTile> section : sections.values()) {
            flat.addAll(section);
        }
        return flat;
    }

    private static Map<TileCategory, Integer> sizesOf(Map<TileCategory, List<AppTile>> sections) {
        Map<TileCategory, Integer> sizes = new LinkedHashMap<>();
        for (Map.Entry<TileCategory, List<AppTile>> section : sections.entrySet()) {
            sizes.put(section.getKey(), section.getValue().size());
        }
        return sizes;
    }

    private int indexToRefocus(String key, int formerIndex) {
        int index = indexOfKey(tiles, key);
        return index >= 0 ? index : Math.max(0, Math.min(formerIndex, tiles.size() - 1));
    }

    private static int indexOfKey(List<AppTile> list, String key) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).key.equals(key)) {
                return i;
            }
        }
        return -1;
    }

    private void bindAll(List<AppTile> newTiles) {
        tiles.clear();
        tiles.addAll(newTiles);
        for (int i = 0; i < views.size(); i++) {
            views.get(i).bind(tiles.get(i));
        }
    }

    private void rebuild(Map<TileCategory, List<AppTile>> newSections) {
        rows.removeAllViews();
        tiles.clear();
        views.clear();
        sectionSizes.clear();
        for (Map.Entry<TileCategory, List<AppTile>> section : newSections.entrySet()) {
            sectionSizes.put(section.getKey(), section.getValue().size());
            addSection(headingOf(section.getKey()), section.getValue());
        }
    }

    /** A plain grid is headed "All apps", so it reads as the full list below the stage. */
    private static int headingOf(TileCategory category) {
        return category == TileCategory.NONE ? R.string.home_section_all : category.labelRes;
    }

    private void addSection(int headingRes, List<AppTile> sectionTiles) {
        if (sectionTiles.isEmpty()) {
            return;
        }
        rows.addView(newHeading(headingRes));
        for (int start = 0; start < sectionTiles.size(); start += COLUMNS) {
            int end = Math.min(start + COLUMNS, sectionTiles.size());
            rows.addView(newRow(sectionTiles.subList(start, end)));
        }
    }

    private TextView newHeading(int textRes) {
        TextView heading = (TextView) inflater.inflate(R.layout.home_section_heading, rows, false);
        heading.setText(textRes);
        fonts.applyTo(heading);
        return heading;
    }

    private LinearLayout newRow(List<AppTile> rowTiles) {
        LinearLayout row = (LinearLayout) inflater.inflate(R.layout.home_tile_row, rows, false);
        alignToColumns(row);
        for (AppTile tile : rowTiles) {
            TileView view = TileView.inflate(inflater, row, tile, listener);
            tiles.add(tile);
            views.add(view);
            row.addView(view);
        }
        return row;
    }

    private void playEntranceOnce() {
        if (!entranceShown) {
            entranceShown = true;
            rows.scheduleLayoutAnimation();
        }
    }
}
