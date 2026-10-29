package local.tvhome;

import android.content.res.Resources;

/** Words for the sun's next turn, as in "Sonnenuntergang 16:42". */
final class WeatherOutlook {
    private final Resources res;

    WeatherOutlook(Resources res) {
        this.res = res;
    }

    /** The next sunrise or sunset, whichever comes first. */
    String sunEvent(Weather weather) {
        if (weather.observedAt.compareTo(weather.sunrise) < 0) {
            return res.getString(R.string.outlook_sunrise, weather.sunrise);
        }
        if (weather.observedAt.compareTo(weather.sunset) < 0) {
            return res.getString(R.string.outlook_sunset, weather.sunset);
        }
        return res.getString(R.string.outlook_sunrise, weather.sunriseTomorrow);
    }
}
