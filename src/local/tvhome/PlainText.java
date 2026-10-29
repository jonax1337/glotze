package local.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Spelling-insensitive comparison keys for names. */
final class PlainText {
    private PlainText() {
    }

    /** Lower case letters and digits only, so "Disney+" and "Prime Video" compare without regard to spacing and signs. */
    static String lettersAndDigits(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder plain = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                plain.append(c);
            }
        }
        return plain.toString();
    }

    /** True if the words of the sought text appear in the text next to each other and in order, so "fox" is not in "Dropbox". */
    static boolean containsWords(String text, String sought) {
        List<String> soughtWords = wordsOf(sought);
        return !soughtWords.isEmpty() && Collections.indexOfSubList(wordsOf(text), soughtWords) >= 0;
    }

    private static List<String> wordsOf(String text) {
        List<String> words = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (char c : text.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else if (word.length() > 0) {
                words.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            words.add(word.toString());
        }
        return words;
    }
}
