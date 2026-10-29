package local.tvhome;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Background threads for the series features: they start on demand and end when idle, so nothing needs shutting down. */
final class SeriesWorkers {
    private static final long IDLE_SECONDS = 30;

    private SeriesWorkers() {
    }

    static ThreadPoolExecutor newPool(int threads) {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, IDLE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }
}
