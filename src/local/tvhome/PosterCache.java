package local.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Posters from the web: decoded ones stay in memory, downloaded files in the app's cache folder.
 * Downloading and decoding never happen on the main thread.
 */
final class PosterCache {
    interface Callback {
        /** Called on the main thread; the bitmap is null if the poster could not be loaded. */
        void onBitmap(Bitmap bitmap);
    }

    private static final String TAG = "PosterCache";
    private static final String DIRECTORY = "posters";
    private static final String PARTIAL_SUFFIX = ".part";
    private static final int TIMEOUT_MS = 8000;
    private static final int BUFFER_SIZE = 8192;
    private static final String USER_AGENT = "tvhome-launcher";
    /** The longest edge a decoded poster may have; larger files are decoded smaller. */
    private static final int MAX_EDGE_PX = 512;
    private static final int MEMORY_SHARE = 16;
    private static final int DOWNLOAD_THREADS = 2;

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());
    private static final ExecutorService WORKERS = SeriesWorkers.newPool(DOWNLOAD_THREADS);
    private static final Map<String, List<Callback>> WAITING = new HashMap<>();
    private static final LruCache<String, Bitmap> MEMORY = new LruCache<String, Bitmap>(memoryBudgetBytes()) {
        @Override
        protected int sizeOf(String url, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    private PosterCache() {
    }

    /** Hands the poster to the callback on the main thread: at once if it is in memory, else once it is loaded. No url means no poster. */
    static void load(Context context, String url, Callback callback) {
        if (url == null) {
            deliver(callback, null);
            return;
        }
        Bitmap cached = MEMORY.get(url);
        if (cached != null) {
            deliver(callback, cached);
            return;
        }
        if (joinRunningLoad(url, callback)) {
            return;
        }
        File directory = new File(context.getApplicationContext().getCacheDir(), DIRECTORY);
        WORKERS.execute(() -> finish(url, loadInBackground(directory, url)));
    }

    private static int memoryBudgetBytes() {
        return (int) (Runtime.getRuntime().maxMemory() / MEMORY_SHARE);
    }

    /** True if the poster is already on its way; the callback then waits for that load instead of starting another. */
    private static boolean joinRunningLoad(String url, Callback callback) {
        synchronized (WAITING) {
            List<Callback> waiting = WAITING.get(url);
            boolean running = waiting != null;
            if (!running) {
                waiting = new ArrayList<>();
                WAITING.put(url, waiting);
            }
            waiting.add(callback);
            return running;
        }
    }

    private static void finish(String url, Bitmap bitmap) {
        if (bitmap != null) {
            MEMORY.put(url, bitmap);
        }
        List<Callback> waiting;
        synchronized (WAITING) {
            waiting = WAITING.remove(url);
        }
        for (Callback callback : waiting) {
            deliver(callback, bitmap);
        }
    }

    private static void deliver(Callback callback, Bitmap bitmap) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onBitmap(bitmap);
        } else {
            MAIN_THREAD.post(() -> callback.onBitmap(bitmap));
        }
    }

    /**
     * Runs on a worker thread; null if the poster can be neither read from the cache nor downloaded.
     * Whatever goes wrong, it returns: the callbacks waiting for this url must always be answered.
     */
    private static Bitmap loadInBackground(File directory, String url) {
        try {
            File file = new File(directory, fileNameFor(url));
            if (!file.exists()) {
                download(url, directory, file);
            }
            return decode(file);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            Log.w(TAG, "Poster " + url + " could not be loaded", e);
            return null;
        }
    }

    private static void download(String url, File directory, File target) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create the poster folder " + directory);
        }
        File partial = new File(directory, target.getName() + PARTIAL_SUFFIX);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(partial)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
        if (!partial.renameTo(target)) {
            throw new IOException("Cannot move " + partial + " to " + target);
        }
    }

    /** A cached file that cannot be decoded is deleted, so the next request downloads it again. */
    private static Bitmap decode(File file) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSizeFor(Math.max(bounds.outWidth, bounds.outHeight));
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap == null) {
            file.delete();
            throw new IOException("Poster file " + file + " is not a readable image");
        }
        return bitmap;
    }

    private static int sampleSizeFor(int longestEdge) {
        int sampleSize = 1;
        while (longestEdge / (sampleSize * 2) >= MAX_EDGE_PX) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static String fileNameFor(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder();
            for (byte b : digest) {
                name.append(String.format("%02x", b));
            }
            return name.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 exists on every Android device", e);
        }
    }
}
