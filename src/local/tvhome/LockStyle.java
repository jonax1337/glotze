package local.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The look of the lock screen as put together in the editor, kept on the device. */
final class LockStyle {
    /** One adjustable property; its choices are the labels in {@code choicesRes}, in this order. */
    enum Setting {
        LAYOUT(R.string.lock_setting_layout, R.array.lock_layouts, 0),
        FONT(R.string.lock_setting_font, R.array.lock_fonts, 1),
        WEIGHT(R.string.lock_setting_weight, R.array.lock_weights, 1),
        WIDTH(R.string.lock_setting_width, R.array.lock_widths, 1),
        COLOUR(R.string.lock_setting_colour, R.array.lock_colours, 0),
        BACKGROUND(R.string.lock_setting_background, R.array.lock_backgrounds, 0),
        MUSIC(R.string.lock_setting_music, R.array.lock_music, 1),
        FIRST_WIDGET(R.string.lock_setting_widget_1, R.array.schoner_widgets, 1),
        SECOND_WIDGET(R.string.lock_setting_widget_2, R.array.schoner_widgets, 0),
        NIGHT_SCHEDULE(R.string.schoner_setting_night_schedule, R.array.schoner_night_schedules, 0),
        NIGHT_FROM(R.string.schoner_setting_night_from, R.array.schoner_night_hours, NightMode.STARTS_AT_HOUR),
        NIGHT_UNTIL(R.string.schoner_setting_night_until, R.array.schoner_night_hours, NightMode.ENDS_AT_HOUR);

        final int labelRes;
        final int choicesRes;
        final int defaultChoice;

        Setting(int labelRes, int choicesRes, int defaultChoice) {
            this.labelRes = labelRes;
            this.choicesRes = choicesRes;
            this.defaultChoice = defaultChoice;
        }
    }

    /** In the order of R.array.lock_layouts. */
    enum Layout { CENTERED, CORNER, STACKED }

    /** In the order of R.array.lock_backgrounds. */
    enum Background { AURORA, BLACK, TIME_OF_DAY, SUNSET, OCEAN, FOREST, LAVENDER, GRAPHITE }

    /** In the order of R.array.schoner_widgets. */
    enum Widget { NONE, WEATHER, HOURS, MOON_AND_WORLD_CLOCK, NOW_PLAYING, DEPARTURES, CALENDAR, GLOTZE }

    private static final Setting[] WIDGET_SLOTS = {Setting.FIRST_WIDGET, Setting.SECOND_WIDGET};
    private static final String PREFERENCES = "lock_style";
    private static final String CLOCK_FONTS = "fonts/clock/";
    // File name parts made by tools/make_clock_fonts.py, in the order of the matching arrays.
    private static final String[] FONT_FILES = {"roboto_flex", "inter", "fraunces", "nunito",
            "unbounded", "bodoni_moda", "space_mono", "big_shoulders"};
    private static final String[] WEIGHT_FILES = {"thin", "light", "regular", "bold", "black"};
    private static final String[] WIDTH_FILES = {"narrow", "normal", "wide"};
    /** Only Roboto Flex has a width axis. */
    private static final int FONT_WITH_WIDTH = 0;
    private static final int[] COLOURS = {
            0xFFF4F6FA, 0xFFFFE0B5, 0xFFC4E4FF, 0xFFFFC9DA, 0xFFBDF4DC, 0xFFF3D27E};

    /** In the order of R.array.lock_music. */
    private static final int MUSIC_ON = 1;
    /** In the order of R.array.schoner_night_schedules. */
    private static final int NIGHT_BY_SUN = 1;

    private final SharedPreferences preferences;
    private final Resources res;

    LockStyle(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        res = context.getResources();
    }

    String choiceLabel(Setting setting) {
        return res.getStringArray(setting.choicesRes)[choice(setting)];
    }

    void step(Setting setting, int delta) {
        int count = res.getStringArray(setting.choicesRes).length;
        preferences.edit().putInt(setting.name(), Math.floorMod(choice(setting) + delta, count)).apply();
    }

    /**
     * Settings that change something with the current choices; the width only exists for one font,
     * and the night hours only while the night follows the clock.
     */
    List<Setting> adjustable() {
        List<Setting> adjustable = new ArrayList<>();
        for (Setting setting : Setting.values()) {
            if (changesSomething(setting)) {
                adjustable.add(setting);
            }
        }
        return adjustable;
    }

    private boolean changesSomething(Setting setting) {
        switch (setting) {
            case WIDTH:
                return choice(Setting.FONT) == FONT_WITH_WIDTH;
            case NIGHT_FROM:
            case NIGHT_UNTIL:
                return !nightFollowsSun();
            default:
                return true;
        }
    }

    Layout layout() {
        return Layout.values()[choice(Setting.LAYOUT)];
    }

    String clockFontAsset() {
        int font = choice(Setting.FONT);
        String name = FONT_FILES[font] + "_" + WEIGHT_FILES[choice(Setting.WEIGHT)];
        if (font == FONT_WITH_WIDTH) {
            name += "_" + WIDTH_FILES[choice(Setting.WIDTH)];
        }
        return CLOCK_FONTS + name + ".ttf";
    }

    int clockColour() {
        return COLOURS[choice(Setting.COLOUR)];
    }

    /** Whether playing music takes over the backdrop, see MusicMood. */
    boolean reactsToMusic() {
        return choice(Setting.MUSIC) == MUSIC_ON;
    }

    Background background() {
        return Background.values()[choice(Setting.BACKGROUND)];
    }

    List<Widget> widgets() {
        List<Widget> widgets = new ArrayList<>();
        for (Setting slot : WIDGET_SLOTS) {
            Widget widget = widgetIn(slot);
            if (widget != Widget.NONE) {
                widgets.add(widget);
            }
        }
        return widgets;
    }

    /** Whether the setting is one of the widget slots and holds the given widget. */
    boolean holds(Setting setting, Widget widget) {
        return Arrays.asList(WIDGET_SLOTS).contains(setting) && widgetIn(setting) == widget;
    }

    /** Whether the night starts and ends with the sun instead of the clock. */
    boolean nightFollowsSun() {
        return choice(Setting.NIGHT_SCHEDULE) == NIGHT_BY_SUN;
    }

    /** The choices are the hours of the day in order, so the position is the hour. */
    int nightStartHour() {
        return choice(Setting.NIGHT_FROM);
    }

    int nightEndHour() {
        return choice(Setting.NIGHT_UNTIL);
    }

    private Widget widgetIn(Setting slot) {
        return Widget.values()[choice(slot)];
    }

    private int choice(Setting setting) {
        return preferences.getInt(setting.name(), setting.defaultChoice);
    }
}
