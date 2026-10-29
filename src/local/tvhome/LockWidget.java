package local.tvhome;

import android.view.View;

/** A small panel below the lock screen clock. Every widget receives the weather; most ignore it. */
interface LockWidget extends WeatherService.Listener {
    View view();

    /** While the lock screen shows; widgets that poll or listen begin here. */
    default void start() {
    }

    /** Called once when the widget leaves the screen; it is not started again. */
    default void stop() {
    }

    /** Before {@link #start} and whenever the night begins or ends; widgets that change at night listen. */
    default void onNight(boolean night) {
    }

    @Override
    default void onWeather(Weather weather) {
    }

    @Override
    default void onWeatherUnavailable() {
    }
}
