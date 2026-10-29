package local.tvhome;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Chooses the stops of the departures widget and the city of the world clock. */
public final class PlacesActivity extends Activity {
    private static final String TAG = "PlacesActivity";

    /** The stops matching a name, from the free BVG API. Blocking: call it off the main thread. */
    private static final class StopSearch {
        private static final String LOCATIONS_URL = "https://v6.bvg.transport.rest/locations"
                + "?query=%s&results=8&stops=true&addresses=false&poi=false&linesOfStops=false";
        private static final String STOP_TYPE = "stop";

        static List<PlaceSettings.Stop> find(String query) throws IOException, JSONException {
            String url = String.format(LOCATIONS_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
            JSONArray matches = HttpJson.getArray(url);
            List<PlaceSettings.Stop> stops = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                if (STOP_TYPE.equals(match.optString("type"))) {
                    stops.add(new PlaceSettings.Stop(match.getString("id"), match.getString("name")));
                }
            }
            return stops;
        }
    }

    /** The home address and the stops around it, from the free BVG API. Blocking: call it off the main thread. */
    private static final class HomeLookup {
        private static final String ADDRESS_URL = "https://v6.bvg.transport.rest/locations"
                + "?query=%s&results=5&stops=false&addresses=true&poi=false";
        private static final String NEARBY_URL = "https://v6.bvg.transport.rest/locations/nearby"
                + "?latitude=%s&longitude=%s&results=%d&distance=1000&stops=true&poi=false";
        private static final String ADDRESS_TYPE = "location";
        /** Slower than a brisk walk, to make up for corners and traffic lights. */
        private static final double METERS_PER_WALKING_MINUTE = 60;

        static List<PlaceSettings.Home> findAddresses(String query) throws IOException, JSONException {
            String url = String.format(ADDRESS_URL, URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
            JSONArray matches = HttpJson.getArray(url);
            List<PlaceSettings.Home> homes = new ArrayList<>();
            for (int i = 0; i < matches.length(); i++) {
                JSONObject match = matches.getJSONObject(i);
                if (ADDRESS_TYPE.equals(match.optString("type")) && match.has("address")) {
                    homes.add(new PlaceSettings.Home(match.getString("address"),
                            match.getDouble("latitude"), match.getDouble("longitude")));
                }
            }
            return homes;
        }

        /** The closest stops, nearest first, each with the time it takes to walk there. */
        static List<PlaceSettings.Stop> stopsAround(PlaceSettings.Home home) throws IOException, JSONException {
            String url = String.format(Locale.ROOT, NEARBY_URL, home.latitude, home.longitude, PlaceSettings.MAX_STOPS);
            JSONArray nearby = HttpJson.getArray(url);
            List<PlaceSettings.Stop> stops = new ArrayList<>();
            for (int i = 0; i < nearby.length(); i++) {
                JSONObject stop = nearby.getJSONObject(i);
                int walkMinutes = (int) Math.max(1, Math.ceil(stop.getDouble("distance") / METERS_PER_WALKING_MINUTE));
                stops.add(new PlaceSettings.Stop(stop.getString("id"), stop.getString("name"), walkMinutes));
            }
            return stops;
        }
    }

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<PlaceSettings.Stop> found = new ArrayList<>();
    private List<PlaceSettings.Stop> chosen;
    private PlaceSettings places;
    private UiSounds sounds;
    private Fonts fonts;
    private OptionsSheet sheet;
    private FocusScrollView scroller;
    private LinearLayout rows;
    private LinearLayout chosenRows;
    private LinearLayout foundRows;
    private TextView noStopsNote;
    private TextView searchStatus;
    private EditText searchField;
    private TextView homeCurrent;
    private TextView homeStatus;
    private EditText homeField;
    private LinearLayout homeRows;
    private TextView cityValue;
    /** Count the searches, so that the answer to an outdated one is ignored. */
    private int searchNumber;
    private int homeSearchNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        fonts = new Fonts(getAssets());
        sounds = new UiSounds(this);
        sheet = new OptionsSheet(this, sounds, fonts);
        places = new PlaceSettings(this);
        chosen = new ArrayList<>(places.stops());
        scroller = findViewById(R.id.settings_scroller);
        rows = findViewById(R.id.settings_rows);
        dressInSoftSolid();
        ((TextView) findViewById(R.id.settings_device)).setText(R.string.places_subtitle);
        addHomeSection();
        addStopSection();
        addWorldClockSection();
        fonts.applyTo(findViewById(android.R.id.content));
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(this::onFocusMoved);
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        sounds.release();
        super.onDestroy();
    }

    private void dressInSoftSolid() {
        Resources res = getResources();
        // The root paints the ground colour; skipping the window background saves another full-screen fill.
        getWindow().setBackgroundDrawable(null);
        findViewById(R.id.settings_badge).setBackground(
                SurfaceDrawable.card(res, res.getDimension(R.dimen.surface_card_corner)));
    }

    private void addHomeSection() {
        addHeading(R.string.places_home_heading);
        homeCurrent = addNote();
        PlaceSettings.Home home = places.home();
        if (home != null) {
            showHome(home);
        }
        homeField = addSearchField(R.string.places_home_hint, this::startHomeSearch);
        homeStatus = addNote();
        homeRows = newColumn();
    }

    private void addStopSection() {
        addHeading(R.string.places_stops_heading);
        chosenRows = newColumn();
        noStopsNote = addNote();
        noStopsNote.setText(R.string.places_no_stops);
        searchField = addSearchField(R.string.places_search_hint, this::startSearch);
        searchStatus = addNote();
        foundRows = newColumn();
        showChosen();
    }

    private void addWorldClockSection() {
        addHeading(R.string.places_clock_heading);
        View row = newRow(R.drawable.ic_moon, R.color.tile_purple, this::chooseCity);
        ((TextView) row.findViewById(R.id.row_label)).setText(R.string.places_clock_city);
        cityValue = row.findViewById(R.id.row_value);
        cityValue.setText(places.worldClock().name);
        rows.addView(row);
    }

    private void addHeading(int textRes) {
        TextView heading = (TextView) getLayoutInflater().inflate(R.layout.settings_heading, rows, false);
        heading.setText(textRes);
        rows.addView(heading);
    }

    /** A column of rows that is rebuilt as a whole; it is added to the list right away. */
    private LinearLayout newColumn() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        rows.addView(column, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return column;
    }

    /** A line of small text below the rows; hidden until it has something to say. */
    private TextView addNote() {
        Resources res = getResources();
        int inset = res.getDimensionPixelSize(R.dimen.places_note_inset);
        TextView note = new TextView(this);
        note.setTextColor(getColor(R.color.text_muted));
        note.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.places_note_text));
        note.setPadding(inset, 0, inset, res.getDimensionPixelSize(R.dimen.places_note_gap));
        note.setVisibility(View.GONE);
        rows.addView(note);
        return note;
    }

    private EditText addSearchField(int hintRes, Runnable onSearch) {
        Resources res = getResources();
        float corner = res.getDimension(R.dimen.surface_tile_corner);
        int inset = res.getDimensionPixelSize(R.dimen.places_field_inset);
        EditText field = new EditText(this);
        field.setBackground(SurfaceDrawable.focusableCard(res, corner));
        field.setPadding(inset, 0, inset, 0);
        field.setHint(hintRes);
        field.setTextColor(getColor(R.color.text_primary));
        field.setHintTextColor(getColor(R.color.text_muted));
        field.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimension(R.dimen.places_field_text));
        field.setSingleLine();
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) {
                return false;
            }
            onSearch.run();
            return true;
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, res.getDimensionPixelSize(R.dimen.row_height));
        params.bottomMargin = res.getDimensionPixelSize(R.dimen.places_row_gap);
        rows.addView(field, params);
        return field;
    }

    private View newRow(int iconRes, int colorRes, Runnable action) {
        Resources res = getResources();
        View row = getLayoutInflater().inflate(R.layout.settings_row, rows, false);
        row.setBackground(SurfaceDrawable.focusableCard(res, res.getDimension(R.dimen.surface_tile_corner)));
        ImageView icon = row.findViewById(R.id.row_icon);
        icon.setImageResource(iconRes);
        icon.setBackground(iconTile(colorRes));
        row.setOnClickListener(view -> {
            sounds.playSelect();
            action.run();
        });
        fonts.applyTo(row);
        return row;
    }

    private GradientDrawable iconTile(int colorRes) {
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(getColor(colorRes));
        tile.setCornerRadius(getResources().getDimension(R.dimen.icon_tile_corner));
        return tile;
    }

    private View stopRow(PlaceSettings.Stop stop, int actionRes, int colorRes, Runnable action) {
        View row = newRow(R.drawable.ic_timer, colorRes, action);
        ((TextView) row.findViewById(R.id.row_label)).setText(stop.name);
        ((TextView) row.findViewById(R.id.row_value)).setText(actionRes);
        return row;
    }

    private void onFocusMoved(View oldFocus, View newFocus) {
        if (newFocus != null) {
            scroller.glideToReveal(newFocus);
        }
        if (oldFocus != null) {
            sounds.playFocus();
        }
    }

    private void showChosen() {
        chosenRows.removeAllViews();
        for (PlaceSettings.Stop stop : chosen) {
            chosenRows.addView(stopRow(stop, R.string.places_remove, R.color.tile_green, () -> remove(stop)));
        }
        noStopsNote.setVisibility(chosen.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Stops that are already chosen are not offered again. */
    private void showFound() {
        foundRows.removeAllViews();
        for (PlaceSettings.Stop stop : found) {
            if (!isChosen(stop)) {
                foundRows.addView(stopRow(stop, R.string.places_add, R.color.tile_teal, () -> add(stop)));
            }
        }
    }

    private boolean isChosen(PlaceSettings.Stop candidate) {
        for (PlaceSettings.Stop stop : chosen) {
            if (stop.id.equals(candidate.id)) {
                return true;
            }
        }
        return false;
    }

    private void add(PlaceSettings.Stop stop) {
        if (chosen.size() >= PlaceSettings.MAX_STOPS) {
            showStatus(R.string.places_stops_full, PlaceSettings.MAX_STOPS);
            return;
        }
        int index = foundRows.indexOfChild(foundRows.getFocusedChild());
        chosen.add(stop);
        places.saveStops(chosen);
        showChosen();
        showFound();
        focusNear(foundRows, index, searchField);
    }

    private void remove(PlaceSettings.Stop stop) {
        int index = chosenRows.indexOfChild(chosenRows.getFocusedChild());
        chosen.remove(stop);
        places.saveStops(chosen);
        showChosen();
        showFound();
        focusNear(chosenRows, index, searchField);
    }

    /** Focus would be lost with the row that held it; it moves to the row now at its place. */
    private static void focusNear(ViewGroup column, int index, View fallback) {
        int count = column.getChildCount();
        View target = count > 0 ? column.getChildAt(Math.min(Math.max(index, 0), count - 1)) : fallback;
        target.requestFocus();
    }

    private void showHome(PlaceSettings.Home home) {
        homeCurrent.setText(getString(R.string.places_home_current, home.address));
        homeCurrent.setVisibility(View.VISIBLE);
    }

    private void startHomeSearch() {
        hideKeyboard(homeField);
        String query = homeField.getText().toString().trim();
        int search = ++homeSearchNumber;
        homeRows.removeAllViews();
        if (query.isEmpty()) {
            homeStatus.setVisibility(View.GONE);
            return;
        }
        showHomeStatus(R.string.places_searching);
        worker.execute(() -> runHomeSearch(search, query));
    }

    private void runHomeSearch(int search, String query) {
        try {
            List<PlaceSettings.Home> homes = HomeLookup.findAddresses(query);
            runOnUiThread(() -> onAddressesFound(search, homes));
        } catch (IOException e) {
            Log.w(TAG, "Address search for \"" + query + "\" failed", e);
            runOnUiThread(() -> onHomeFailed(search, R.string.places_search_offline));
        } catch (JSONException e) {
            Log.w(TAG, "Address search for \"" + query + "\" answered something unreadable", e);
            runOnUiThread(() -> onHomeFailed(search, R.string.places_search_unreadable));
        }
    }

    private void onAddressesFound(int search, List<PlaceSettings.Home> homes) {
        if (search != homeSearchNumber) {
            return;
        }
        if (homes.isEmpty()) {
            showHomeStatus(R.string.places_home_no_results);
            return;
        }
        homeStatus.setVisibility(View.GONE);
        for (PlaceSettings.Home home : homes) {
            View row = newRow(R.drawable.ic_account, R.color.tile_teal, () -> chooseHome(home));
            ((TextView) row.findViewById(R.id.row_label)).setText(home.address);
            ((TextView) row.findViewById(R.id.row_value)).setText(R.string.places_home_choose);
            homeRows.addView(row);
        }
        if (homeField.hasFocus()) {
            homeRows.getChildAt(0).requestFocus();
        }
    }

    /** The stops around the new home replace the chosen ones, each with its walk. */
    private void chooseHome(PlaceSettings.Home home) {
        int search = ++homeSearchNumber;
        showHomeStatus(R.string.places_home_looking);
        worker.execute(() -> {
            try {
                List<PlaceSettings.Stop> stops = HomeLookup.stopsAround(home);
                runOnUiThread(() -> onStopsAround(search, home, stops));
            } catch (IOException e) {
                Log.w(TAG, "Stops around " + home.address + " failed", e);
                runOnUiThread(() -> onHomeFailed(search, R.string.places_search_offline));
            } catch (JSONException e) {
                Log.w(TAG, "Stops around " + home.address + " answered something unreadable", e);
                runOnUiThread(() -> onHomeFailed(search, R.string.places_search_unreadable));
            }
        });
    }

    private void onStopsAround(int search, PlaceSettings.Home home, List<PlaceSettings.Stop> stops) {
        if (search != homeSearchNumber) {
            return;
        }
        if (stops.isEmpty()) {
            showHomeStatus(R.string.places_home_no_stops);
            return;
        }
        places.saveHome(home);
        new WeatherService(this).forgetForecast();
        places.saveStops(stops);
        chosen = new ArrayList<>(stops);
        showHome(home);
        showChosen();
        showFound();
        homeRows.removeAllViews();
        homeStatus.setVisibility(View.GONE);
        homeField.setText("");
        homeField.requestFocus();
    }

    private void onHomeFailed(int search, int messageRes) {
        if (search == homeSearchNumber) {
            showHomeStatus(messageRes);
        }
    }

    private void showHomeStatus(int textRes) {
        homeStatus.setText(textRes);
        homeStatus.setVisibility(View.VISIBLE);
    }

    private void startSearch() {
        hideKeyboard(searchField);
        String query = searchField.getText().toString().trim();
        int search = ++searchNumber;
        found.clear();
        showFound();
        if (query.isEmpty()) {
            searchStatus.setVisibility(View.GONE);
            return;
        }
        showStatus(R.string.places_searching);
        worker.execute(() -> runSearch(search, query));
    }

    private void runSearch(int search, String query) {
        try {
            List<PlaceSettings.Stop> stops = StopSearch.find(query);
            runOnUiThread(() -> onFound(search, stops));
        } catch (IOException e) {
            Log.w(TAG, "Stop search for \"" + query + "\" failed", e);
            runOnUiThread(() -> onSearchFailed(search, R.string.places_search_offline));
        } catch (JSONException e) {
            Log.w(TAG, "Stop search for \"" + query + "\" answered something unreadable", e);
            runOnUiThread(() -> onSearchFailed(search, R.string.places_search_unreadable));
        }
    }

    private void onFound(int search, List<PlaceSettings.Stop> stops) {
        if (search != searchNumber) {
            return;
        }
        found.addAll(stops);
        showFound();
        if (foundRows.getChildCount() == 0) {
            showStatus(R.string.places_no_results);
            return;
        }
        searchStatus.setVisibility(View.GONE);
        if (searchField.hasFocus()) {
            foundRows.getChildAt(0).requestFocus();
        }
    }

    private void onSearchFailed(int search, int messageRes) {
        if (search == searchNumber) {
            showStatus(messageRes);
        }
    }

    private void showStatus(int textRes, Object... formatArgs) {
        searchStatus.setText(getString(textRes, formatArgs));
        searchStatus.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard(EditText field) {
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(field.getWindowToken(), 0);
    }

    private void chooseCity() {
        List<OptionsSheet.Option> options = new ArrayList<>();
        for (PlaceSettings.City city : places.cityChoices()) {
            options.add(new OptionsSheet.Option(city.name, () -> {
                places.saveWorldClock(city);
                cityValue.setText(city.name);
            }));
        }
        sheet.show(getString(R.string.places_clock_city), options);
    }
}
