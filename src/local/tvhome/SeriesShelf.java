package local.tvhome;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * What the home screen needs of the followed series. Call it from the main thread: it answers from
 * the stored copy at once and asks TVmaze about stale series in the background.
 */
final class SeriesShelf {
    interface Listener {
        /** The shelf changed; always called on the main thread. */
        void onSeriesChanged(List<SeriesCard> cards);
    }

    private static final String TAG = "SeriesShelf";
    private static final long SYNC_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(6);
    /** After a failed update (offline, unknown show, rate limit) the series is left alone this long. */
    private static final long RETRY_PAUSE_MILLIS = TimeUnit.MINUTES.toMillis(15);
    /** TVmaze allows 20 requests per 10 seconds; one per 600 ms stays well below. */
    private static final long REQUEST_PAUSE_MILLIS = 600;

    private final SeriesStore store;
    private final AppResolver appResolver;
    private final SeriesCardFactory cardFactory;
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = SeriesWorkers.newPool(1);
    /** When the update of a series last failed, by series id. Only touched by the worker thread. */
    private final Map<Long, Long> failedAt = new HashMap<>();
    private volatile Listener listener;

    SeriesShelf(Context context) {
        Context app = context.getApplicationContext();
        this.store = new SeriesStore(app);
        this.appResolver = new AppResolver(app);
        this.cardFactory = new SeriesCardFactory(app.getResources());
    }

    /** As many as the shelf row holds: new episodes first, then what was watched or recognised last, then what airs next. */
    List<SeriesCard> cards() {
        long now = System.currentTimeMillis();
        List<Series> followed = store.all();
        Collections.sort(followed, byRelevance(now));
        List<SeriesCard> cards = new ArrayList<>();
        for (Series series : followed.subList(0, Math.min(HomeShelf.ITEM_LIMIT, followed.size()))) {
            cards.add(cardFactory.cardOf(series, now));
        }
        return cards;
    }

    /** Reports the stored cards at once, then again for every series TVmaze has news about. */
    void start(Listener newListener) {
        listener = newListener;
        newListener.onSeriesChanged(cards());
        worker.execute(this::syncStaleSeries);
    }

    void stop() {
        listener = null;
    }

    /** The app to open for this series, or null if neither a chosen nor a matching app is installed. Counts the episode as seen. */
    Intent launchIntentFor(SeriesCard card) {
        Series series = followedSeries(card.id);
        if (series == null) {
            return null;
        }
        Intent intent = appResolver.launchIntent(series.launchPackage, series.networkName);
        if (intent != null) {
            markOpened(series.id);
        }
        return intent;
    }

    List<AppChoice> appChoices() {
        return appResolver.choices();
    }

    void chooseApp(SeriesCard card, String packageName) {
        store.update(card.id, series -> series.launchPackage = packageName);
    }

    void unfollow(SeriesCard card) {
        store.unfollow(card.id);
        reportChange();
    }

    private static Comparator<Series> byRelevance(final long now) {
        return new Comparator<Series>() {
            @Override
            public int compare(Series a, Series b) {
                boolean freshA = SeriesCardFactory.isFresh(a, now);
                boolean freshB = SeriesCardFactory.isFresh(b, now);
                if (freshA != freshB) {
                    return freshA ? -1 : 1;
                }
                int byLastSeen = Long.compare(b.lastDetectedAt, a.lastDetectedAt);
                if (byLastSeen != 0) {
                    return byLastSeen;
                }
                int byNextAir = Long.compare(nextAirTime(a), nextAirTime(b));
                return byNextAir != 0 ? byNextAir : Long.compare(b.addedAt, a.addedAt);
            }
        };
    }

    private static long nextAirTime(Series series) {
        boolean known = series.next != null && series.next.airMillis > 0;
        return known ? series.next.airMillis : Long.MAX_VALUE;
    }

    private Series followedSeries(long id) {
        for (Series series : store.all()) {
            if (series.id == id) {
                return series;
            }
        }
        return null;
    }

    private void markOpened(long id) {
        long now = System.currentTimeMillis();
        store.update(id, series -> {
            if (series.previous != null) {
                series.acknowledgedAirMillis = series.previous.airMillis;
            }
            series.lastDetectedAt = now;
        });
    }

    /** Runs on the worker thread. */
    private void syncStaleSeries() {
        for (Series series : store.all()) {
            if (needsUpdate(series)) {
                refresh(series.id);
                if (!pauseBetweenRequests()) {
                    return;
                }
            }
        }
    }

    private boolean needsUpdate(Series series) {
        long now = System.currentTimeMillis();
        Long failure = failedAt.get(series.id);
        boolean waitingAfterFailure = failure != null && now - failure < RETRY_PAUSE_MILLIS;
        return !waitingAfterFailure && now - series.syncedAt >= SYNC_INTERVAL_MILLIS;
    }

    private void refresh(long id) {
        try {
            ShowDetails details = TvMazeClient.details(id);
            long now = System.currentTimeMillis();
            store.update(id, series -> apply(details, series, now));
            failedAt.remove(id);
            reportChange();
        } catch (IOException e) {
            failedAt.put(id, System.currentTimeMillis());
            Log.w(TAG, "The episodes of show " + id + " could not be updated", e);
        }
    }

    private static void apply(ShowDetails details, Series series, long now) {
        series.name = details.hit.name;
        if (details.hit.imageUrl != null) {
            series.imageUrl = details.hit.imageUrl;
        }
        if (details.hit.networkName != null) {
            series.networkName = details.hit.networkName;
        }
        series.next = details.next;
        series.previous = details.previous;
        series.syncedAt = now;
    }

    private static boolean pauseBetweenRequests() {
        try {
            Thread.sleep(REQUEST_PAUSE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void reportChange() {
        mainThread.post(() -> {
            Listener current = listener;
            if (current != null) {
                current.onSeriesChanged(cards());
            }
        });
    }
}
