package local.tvhome;

import android.content.Context;
import android.media.MediaMetadata;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Recognises series from whatever plays: when a media session's texts name a show TVmaze knows
 * exactly, the show is followed. Music, live, kids and assistant apps are never asked about, and
 * nothing is done while the automatic recognition is switched off. The texts of the sessions that
 * are looked at are logged under the tag "GlotzeSeries" to learn what each app offers.
 */
final class SeriesDetector {
    private static final String TAG = "GlotzeSeries";

    /** Package name prefixes of apps that play music, live streams, kids' content or the assistant. Extend as needed. */
    private static final List<String> BLOCKED_PACKAGE_PREFIXES = Arrays.asList(
            "com.spotify.",
            "com.amazon.firetv.youtube",
            "tv.twitch.",
            "com.amazon.alexamediaplayer",
            "com.amazon.tahoe",
            "com.amazon.freetime.",
            "com.amazon.vizzini",
            "com.amazon.mp3",
            "com.google.android.apps.podcasts",
            "com.audible.",
            "tunein.",
            "de.radio.",
            "fm.player");
    /** TVmaze show types that are not series to follow. */
    private static final Set<String> EXCLUDED_TYPES = new HashSet<>(Arrays.asList("sports", "news"));
    /** The metadata texts that may name a show, most likely first. */
    private static final String[] TEXT_KEYS = {
            MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ALBUM,
            MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE};

    /** "S2:E5", "S02 E05", "Staffel 2", "Season 2", "Folge 5", "Episode 5", "Ep. 5". */
    private static final Pattern EPISODE_MARKER = Pattern.compile(
            "\\bS\\d+\\s*[:.\\-]?\\s*E\\d+\\b|\\b(?:staffel|season|folge|episode|ep)\\.?\\s*\\d+\\b",
            Pattern.CASE_INSENSITIVE);
    /** Where one text holds several names: "Show · S2:E5", "Show | Episode", "Show - Episode". */
    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[·|]\\s*|\\s+[-–—]\\s+");
    private static final Pattern EDGE_JUNK = Pattern.compile("^[\\s,;:\\-–—·|]+|[\\s,;:\\-–—·|]+$");
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 80;

    /** A name TVmaze answered for is not asked about again for this long. */
    private static final long RECHECK_MILLIS = TimeUnit.HOURS.toMillis(24);
    /** A name whose lookup failed (offline, rate limit) is held back this long, so lookups do not pile up. */
    private static final long RETRY_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(5);
    /** TVmaze allows 20 requests per 10 seconds; one per 5 seconds keeps the detector far below. */
    private static final long REQUEST_SPACING_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private static final ExecutorService WORKER = SeriesWorkers.newPool(1);
    /** Until when a name is not asked about, by plain-text key. */
    private static final Map<String, Long> NOT_ASKED_BEFORE = new HashMap<>();
    private static String lastLoggedSession = "";
    /** Only touched by the worker thread. */
    private static long lastRequestAt;

    private SeriesDetector() {
    }

    /** Call it when a session's metadata changes; returns at once, the lookups run in the background. */
    static void onMetadata(Context context, String packageName, MediaMetadata metadata, boolean playing) {
        SeriesStore store = new SeriesStore(context);
        if (isBlocked(packageName) || !store.autoDetectEnabled()) {
            return;
        }
        logOnChange(packageName, metadata);
        if (!playing) {
            return;
        }
        List<String> names = candidateNames(metadata);
        if (!names.isEmpty()) {
            WORKER.execute(() -> recognise(store, names));
        }
    }

    private static synchronized void logOnChange(String packageName, MediaMetadata metadata) {
        StringBuilder session = new StringBuilder("package=").append(packageName);
        for (String key : TEXT_KEYS) {
            session.append(' ').append(key.substring(key.lastIndexOf('.') + 1)).append('=').append(metadata.getString(key));
        }
        String line = session.toString();
        if (!line.equals(lastLoggedSession)) {
            lastLoggedSession = line;
            Log.d(TAG, line);
        }
    }

    private static boolean isBlocked(String packageName) {
        String lowerCase = packageName.toLowerCase(Locale.ROOT);
        for (String prefix : BLOCKED_PACKAGE_PREFIXES) {
            if (lowerCase.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** The cleaned, distinct names the texts may hold, in the order of {@link #TEXT_KEYS}. */
    private static List<String> candidateNames(MediaMetadata metadata) {
        Map<String, String> byPlainText = new LinkedHashMap<>();
        for (String key : TEXT_KEYS) {
            String text = metadata.getString(key);
            if (text == null) {
                continue;
            }
            String withoutEpisode = EPISODE_MARKER.matcher(text).replaceAll(" ");
            for (String part : NAME_SEPARATOR.split(withoutEpisode)) {
                String name = EDGE_JUNK.matcher(part).replaceAll("");
                boolean plausible = name.length() >= MIN_NAME_LENGTH && name.length() <= MAX_NAME_LENGTH;
                if (plausible && !byPlainText.containsKey(PlainText.lettersAndDigits(name))) {
                    byPlainText.put(PlainText.lettersAndDigits(name), name);
                }
            }
        }
        return new ArrayList<>(byPlainText.values());
    }

    /** Runs on the worker thread: a followed series is only noted as watched, an unknown name is asked about at TVmaze. */
    private static void recognise(SeriesStore store, List<String> names) {
        for (String name : names) {
            Series followed = followedNamed(store, name);
            if (followed != null) {
                noteWatched(store, followed.id);
                return;
            }
            if (!askedRecently(name) && !askTvMaze(store, name)) {
                return;
            }
        }
    }

    private static Series followedNamed(SeriesStore store, String name) {
        String wanted = PlainText.lettersAndDigits(name);
        for (Series series : store.all()) {
            if (PlainText.lettersAndDigits(series.name).equals(wanted)) {
                return series;
            }
        }
        return null;
    }

    private static void noteWatched(SeriesStore store, long id) {
        long now = System.currentTimeMillis();
        store.update(id, series -> series.lastDetectedAt = now);
    }

    /** Follows the show if it carries exactly this name. Returns false if the lookup should not go on, because it failed. */
    private static boolean askTvMaze(SeriesStore store, String name) {
        if (!awaitRequestSlot()) {
            return false;
        }
        try {
            ShowHit hit = TvMazeClient.singleSearch(name);
            holdBackFor(name, RECHECK_MILLIS);
            if (isSeriesNamed(hit, name)) {
                store.follow(hit, true);
            }
            return true;
        } catch (IOException e) {
            holdBackFor(name, RETRY_PAUSE_MILLIS);
            Log.w(TAG, "TVmaze could not be asked about \"" + name + "\"; it is tried again later", e);
            return false;
        }
    }

    private static boolean isSeriesNamed(ShowHit hit, String name) {
        if (hit == null) {
            return false;
        }
        boolean sameName = PlainText.lettersAndDigits(hit.name).equals(PlainText.lettersAndDigits(name));
        boolean excluded = hit.type != null && EXCLUDED_TYPES.contains(hit.type.toLowerCase(Locale.ROOT));
        return sameName && !excluded;
    }

    private static boolean awaitRequestSlot() {
        long wait = lastRequestAt + REQUEST_SPACING_MILLIS - SystemClock.elapsedRealtime();
        try {
            if (wait > 0) {
                Thread.sleep(wait);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        lastRequestAt = SystemClock.elapsedRealtime();
        return true;
    }

    private static boolean askedRecently(String name) {
        synchronized (NOT_ASKED_BEFORE) {
            Long until = NOT_ASKED_BEFORE.get(PlainText.lettersAndDigits(name));
            return until != null && System.currentTimeMillis() < until;
        }
    }

    private static void holdBackFor(String name, long holdMillis) {
        long now = System.currentTimeMillis();
        synchronized (NOT_ASKED_BEFORE) {
            NOT_ASKED_BEFORE.put(PlainText.lettersAndDigits(name), now + holdMillis);
            NOT_ASKED_BEFORE.values().removeIf(until -> until <= now);
        }
    }
}
