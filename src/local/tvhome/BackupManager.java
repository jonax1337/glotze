package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Saves every preferences file of the launcher into one JSON file in the app's external files
 * folder (reachable via adb pull) and restores it from there. The lock preferences never leave
 * the device and are never overwritten, so a backup cannot unlock or lock anything.
 */
final class BackupManager {
    static final String BACKUP_DIRECTORY = "backup";
    static final String BACKUP_FILE_NAME = "glotze-backup.json";
    private static final String EXCLUDED_PREFERENCES = "app_lock";
    private static final String FORMAT = "glotze-backup";
    private static final int VERSION = 1;
    private static final String PREFERENCES_DIRECTORY = "shared_prefs";
    private static final String PREFERENCES_SUFFIX = ".xml";
    private static final String TEMPORARY_SUFFIX = ".tmp";
    private static final Pattern PREFERENCES_NAME = Pattern.compile("[A-Za-z0-9_.\\-]+");

    private static final String KEY_FORMAT = "format";
    private static final String KEY_VERSION = "version";
    private static final String KEY_PREFERENCES = "preferences";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VALUE = "value";

    private static final String TYPE_STRING = "string";
    private static final String TYPE_INT = "int";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_STRING_SET = "string_set";

    /** Outcome of an export or restore; never null, errorMessage is empty on success. */
    static final class Result {
        final boolean success;
        final String errorMessage;
        final int entryCount;

        private Result(boolean success, String errorMessage, int entryCount) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.entryCount = entryCount;
        }

        static Result succeeded(int entryCount) {
            return new Result(true, "", entryCount);
        }

        static Result failed(String errorMessage) {
            return new Result(false, errorMessage, 0);
        }
    }

    private static final class BackupException extends Exception {
        BackupException(String message) {
            super(message);
        }

        BackupException(String message, Throwable cause) {
            super(message + ": " + cause, cause);
        }
    }

    private BackupManager() {
    }

    /** The file written by {@link #export} and read by {@link #restore}, or null without external storage. */
    static File backupFile(Context context) {
        File base = context.getExternalFilesDir(null);
        if (base == null) {
            return null;
        }
        return new File(new File(base, BACKUP_DIRECTORY), BACKUP_FILE_NAME);
    }

    static Result export(Context context) {
        try {
            File target = requireBackupFile(context);
            JSONObject preferences = new JSONObject();
            int entryCount = 0;
            for (String name : preferencesNames(context)) {
                Map<String, ?> entries = context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
                preferences.put(name, entriesToJson(name, entries));
                entryCount += entries.size();
            }
            JSONObject document = new JSONObject();
            document.put(KEY_FORMAT, FORMAT);
            document.put(KEY_VERSION, VERSION);
            document.put(KEY_PREFERENCES, preferences);
            writeAtomically(target, document.toString(2));
            return Result.succeeded(entryCount);
        } catch (BackupException e) {
            return Result.failed(e.getMessage());
        } catch (JSONException e) {
            return Result.failed("Sicherung konnte nicht erzeugt werden: " + e);
        }
    }

    /** Replaces the backed-up preferences files; a damaged or unknown file changes nothing. */
    static Result restore(Context context) {
        try {
            File source = requireBackupFile(context);
            Map<String, Map<String, Object>> backup = parse(readText(source));
            applyAll(context, backup);
            return Result.succeeded(countEntries(backup));
        } catch (BackupException e) {
            return Result.failed(e.getMessage());
        }
    }

    private static File requireBackupFile(Context context) throws BackupException {
        File file = backupFile(context);
        if (file == null) {
            throw new BackupException("Kein externer Speicher fuer die Sicherung verfuegbar");
        }
        return file;
    }

    private static String[] preferencesNames(Context context) {
        File directory = new File(context.getApplicationInfo().dataDir, PREFERENCES_DIRECTORY);
        String[] files = directory.list();
        if (files == null) {
            return new String[0];
        }
        Set<String> names = new HashSet<>();
        for (String file : files) {
            if (file.endsWith(PREFERENCES_SUFFIX)) {
                names.add(file.substring(0, file.length() - PREFERENCES_SUFFIX.length()));
            }
        }
        names.remove(EXCLUDED_PREFERENCES);
        String[] sorted = names.toArray(new String[0]);
        Arrays.sort(sorted);
        return sorted;
    }

    private static JSONObject entriesToJson(String preferencesName, Map<String, ?> entries)
            throws BackupException, JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            try {
                json.put(entry.getKey(), valueToJson(entry.getValue()));
            } catch (BackupException e) {
                throw new BackupException(preferencesName + "/" + entry.getKey() + ": " + e.getMessage());
            }
        }
        return json;
    }

    private static JSONObject valueToJson(Object value) throws BackupException, JSONException {
        JSONObject json = new JSONObject();
        if (value instanceof String) {
            json.put(KEY_TYPE, TYPE_STRING).put(KEY_VALUE, value);
        } else if (value instanceof Integer) {
            json.put(KEY_TYPE, TYPE_INT).put(KEY_VALUE, value);
        } else if (value instanceof Long) {
            json.put(KEY_TYPE, TYPE_LONG).put(KEY_VALUE, value);
        } else if (value instanceof Float) {
            json.put(KEY_TYPE, TYPE_FLOAT).put(KEY_VALUE, Float.toString((Float) value));
        } else if (value instanceof Boolean) {
            json.put(KEY_TYPE, TYPE_BOOLEAN).put(KEY_VALUE, value);
        } else if (value instanceof Set) {
            json.put(KEY_TYPE, TYPE_STRING_SET).put(KEY_VALUE, new JSONArray((Set<?>) value));
        } else {
            throw new BackupException("Nicht unterstuetzter Werttyp " + value);
        }
        return json;
    }

    private static Map<String, Map<String, Object>> parse(String text) throws BackupException {
        try {
            JSONObject document = new JSONObject(text);
            if (!FORMAT.equals(document.optString(KEY_FORMAT))) {
                throw new BackupException("Das ist keine Glotze-Sicherung");
            }
            int version = document.optInt(KEY_VERSION, -1);
            if (version != VERSION) {
                throw new BackupException("Nicht unterstuetzte Sicherungsversion " + version);
            }
            return parsePreferences(document.getJSONObject(KEY_PREFERENCES));
        } catch (JSONException e) {
            throw new BackupException("Sicherung ist beschaedigt", e);
        }
    }

    private static Map<String, Map<String, Object>> parsePreferences(JSONObject preferences)
            throws BackupException, JSONException {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        Iterator<String> names = preferences.keys();
        while (names.hasNext()) {
            String name = names.next();
            if (EXCLUDED_PREFERENCES.equals(name)) {
                continue;
            }
            if (!PREFERENCES_NAME.matcher(name).matches()) {
                throw new BackupException("Ungueltiger Einstellungsname '" + name + "'");
            }
            result.put(name, parseEntries(name, preferences.getJSONObject(name)));
        }
        return result;
    }

    private static Map<String, Object> parseEntries(String preferencesName, JSONObject entries)
            throws BackupException, JSONException {
        Map<String, Object> result = new LinkedHashMap<>();
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                result.put(key, valueFromJson(entries.getJSONObject(key)));
            } catch (BackupException e) {
                throw new BackupException(preferencesName + "/" + key + ": " + e.getMessage());
            }
        }
        return result;
    }

    private static Object valueFromJson(JSONObject json) throws BackupException, JSONException {
        String type = json.getString(KEY_TYPE);
        Object value = json.get(KEY_VALUE);
        switch (type) {
            case TYPE_STRING:
                return requireType(value, String.class, type);
            case TYPE_INT:
                return requireType(value, Integer.class, type);
            case TYPE_LONG:
                return requireLong(value);
            case TYPE_FLOAT:
                return parseFloat(requireType(value, String.class, type));
            case TYPE_BOOLEAN:
                return requireType(value, Boolean.class, type);
            case TYPE_STRING_SET:
                return parseStringSet(requireType(value, JSONArray.class, type));
            default:
                throw new BackupException("Unbekannter Werttyp '" + type + "'");
        }
    }

    private static <T> T requireType(Object value, Class<T> expected, String type) throws BackupException {
        if (!expected.isInstance(value)) {
            throw new BackupException("Wert passt nicht zum Typ " + type);
        }
        return expected.cast(value);
    }

    private static long requireLong(Object value) throws BackupException {
        if (!(value instanceof Integer || value instanceof Long)) {
            throw new BackupException("Wert passt nicht zum Typ " + TYPE_LONG);
        }
        return ((Number) value).longValue();
    }

    private static Float parseFloat(String text) throws BackupException {
        try {
            return Float.valueOf(text);
        } catch (NumberFormatException e) {
            throw new BackupException("Ungueltige Kommazahl '" + text + "'", e);
        }
    }

    private static Set<String> parseStringSet(JSONArray array) throws BackupException, JSONException {
        Set<String> result = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            result.add(requireType(array.get(i), String.class, TYPE_STRING_SET));
        }
        return result;
    }

    private static void applyAll(Context context, Map<String, Map<String, Object>> backup)
            throws BackupException {
        Map<String, Map<String, ?>> previous = new LinkedHashMap<>();
        for (String name : backup.keySet()) {
            previous.put(name, context.getSharedPreferences(name, Context.MODE_PRIVATE).getAll());
        }
        try {
            for (Map.Entry<String, Map<String, Object>> file : backup.entrySet()) {
                replace(context, file.getKey(), file.getValue());
            }
        } catch (BackupException e) {
            rollBack(context, previous);
            throw e;
        }
    }

    private static void rollBack(Context context, Map<String, Map<String, ?>> previous) {
        for (Map.Entry<String, Map<String, ?>> file : previous.entrySet()) {
            try {
                replace(context, file.getKey(), file.getValue());
            } catch (BackupException ignored) {
                // The original failure is reported; a second one cannot be handled any better.
            }
        }
    }

    private static void replace(Context context, String name, Map<String, ?> entries) throws BackupException {
        SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            put(editor, entry.getKey(), entry.getValue());
        }
        if (!editor.commit()) {
            throw new BackupException("Einstellungen '" + name + "' konnten nicht geschrieben werden");
        }
    }

    @SuppressWarnings("unchecked")
    private static void put(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else {
            editor.putStringSet(key, (Set<String>) value);
        }
    }

    private static int countEntries(Map<String, Map<String, Object>> backup) {
        int count = 0;
        for (Map<String, Object> entries : backup.values()) {
            count += entries.size();
        }
        return count;
    }

    private static String readText(File file) throws BackupException {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BackupException("Sicherung " + file + " nicht lesbar", e);
        }
    }

    private static void writeAtomically(File target, String text) throws BackupException {
        File directory = target.getParentFile();
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new BackupException("Ordner " + directory + " konnte nicht angelegt werden");
        }
        File temporary = new File(directory, target.getName() + TEMPORARY_SUFFIX);
        try (OutputStream out = new FileOutputStream(temporary)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BackupException("Sicherung " + temporary + " nicht schreibbar", e);
        }
        if (!temporary.renameTo(target)) {
            throw new BackupException("Sicherung konnte nicht nach " + target + " verschoben werden");
        }
    }
}
