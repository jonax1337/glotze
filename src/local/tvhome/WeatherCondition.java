package local.tvhome;

enum WeatherCondition {
    CLEAR(R.string.weather_clear),
    PARTLY_CLOUDY(R.string.weather_partly_cloudy),
    CLOUDY(R.string.weather_cloudy),
    FOG(R.string.weather_fog),
    DRIZZLE(R.string.weather_drizzle),
    RAIN(R.string.weather_rain),
    SNOW(R.string.weather_snow),
    THUNDER(R.string.weather_thunder);

    final int labelRes;

    WeatherCondition(int labelRes) {
        this.labelRes = labelRes;
    }

    /** Groups the WMO weather interpretation codes that Open-Meteo reports (WMO 4677 subset). */
    static WeatherCondition fromWmoCode(int code) {
        if (code == 0) {
            return CLEAR;
        }
        if (code <= 2) {
            return PARTLY_CLOUDY;
        }
        if (code == 45 || code == 48) {
            return FOG;
        }
        if (code >= 51 && code <= 57) {
            return DRIZZLE;
        }
        if ((code >= 61 && code <= 67) || (code >= 80 && code <= 82)) {
            return RAIN;
        }
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) {
            return SNOW;
        }
        return code >= 95 ? THUNDER : CLOUDY;
    }
}
