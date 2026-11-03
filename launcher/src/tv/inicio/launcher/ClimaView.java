package tv.inicio.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Current weather and forecast, right-aligned; refreshes itself while started. */
public final class ClimaView extends LinearLayout {
    private static final long EVERY_MS = 30 * 60 * 1000;
    private static final int TIMEOUT_MS = 8000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::refresh;
    private final TextView icon, temp, place, forecast;
    private Bichito pet;
    private long fetchedAt;

    public ClimaView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        LinearLayout now = new LinearLayout(context);
        now.setOrientation(HORIZONTAL);
        now.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        icon = new TextView(context);
        Ui.style(icon, Ui.TEXT, 26, "sans-serif");
        icon.setPadding(0, 0, Ui.dp(context, 8), 0);
        temp = new TextView(context);
        Ui.style(temp, Ui.TEXT, 36, "sans-serif-thin");
        now.addView(icon);
        now.addView(temp);
        addView(now);
        place = new TextView(context);
        Ui.style(place, Ui.TEXT_DIM, 12, "sans-serif-light");
        place.setAllCaps(true);
        place.setLetterSpacing(0.12f);
        place.setGravity(Gravity.END);
        addView(place);
        forecast = new TextView(context);
        Ui.style(forecast, Ui.TEXT_DIM, 11, "sans-serif");
        forecast.setGravity(Gravity.END);
        forecast.setPadding(0, Ui.dp(context, 6), 0, 0);
        addView(forecast);
    }

    /** The pet dresses for the weather. */
    public void setPet(Bichito pet) {
        this.pet = pet;
    }

    public void start() {
        long since = System.currentTimeMillis() - fetchedAt;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, since >= EVERY_MS ? 0 : EVERY_MS - since);
    }

    public void stop() {
        handler.removeCallbacks(tick);
    }

    private void refresh() {
        new Thread(() -> {
            try {
                SharedPreferences clima = getContext().getSharedPreferences("clima", Context.MODE_PRIVATE);
                if (!clima.contains("lat")) {
                    JSONObject geo = new JSONObject(Ui.get("https://ipwho.is/?fields=success,city,latitude,longitude", TIMEOUT_MS));
                    if (!geo.optBoolean("success")) return;
                    clima.edit()
                            .putString("lat", String.valueOf(geo.getDouble("latitude")))
                            .putString("lon", String.valueOf(geo.getDouble("longitude")))
                            .putString("city", geo.optString("city"))
                            .apply();
                }
                JSONObject all = new JSONObject(Ui.get("https://api.open-meteo.com/v1/forecast?latitude="
                        + clima.getString("lat", "") + "&longitude=" + clima.getString("lon", "")
                        + "&current=temperature_2m,weather_code,is_day"
                        + "&hourly=temperature_2m,weather_code,is_day"
                        + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
                        + "&forecast_days=2&timezone=auto", TIMEOUT_MS));
                JSONObject cur = all.getJSONObject("current");
                int degrees = (int) Math.round(cur.getDouble("temperature_2m"));
                int code = cur.getInt("weather_code");
                boolean day = cur.optInt("is_day", 1) == 1;
                String where = clima.getString("city", "") + " · " + describe(code);
                String next = forecast(all, cur.getString("time"));
                fetchedAt = System.currentTimeMillis();
                handler.post(() -> {
                    icon.setText(icon(code, day));
                    temp.setText(degrees + "°");
                    place.setText(where);
                    forecast.setText(next);
                    if (pet != null) pet.setWeather(code, degrees, day);
                });
            } catch (Exception ignored) {
                // Offline or service down: keep the last value and retry next tick.
            }
        }).start();
        handler.postDelayed(tick, EVERY_MS);
    }

    /** "23 h ☁️ 19°   02 h 🌧️ 18°   05 h ☁️ 17°   Mañana 🌤️ 14°/26°" */
    private static String forecast(JSONObject all, String nowIso) throws Exception {
        JSONObject hourly = all.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        String thisHour = nowIso.substring(0, 13);
        int start = 0;
        while (start < times.length() && times.getString(start).substring(0, 13).compareTo(thisHour) < 0) start++;
        List<String> parts = new ArrayList<>();
        for (int step = 3; step <= 9; step += 3) {
            int i = start + step;
            if (i >= times.length()) break;
            parts.add(times.getString(i).substring(11, 13) + " h " + icon(hourly.getJSONArray("weather_code").getInt(i),
                    hourly.getJSONArray("is_day").getInt(i) == 1) + " "
                    + Math.round(hourly.getJSONArray("temperature_2m").getDouble(i)) + "°");
        }
        JSONObject daily = all.getJSONObject("daily");
        parts.add("Mañana " + icon(daily.getJSONArray("weather_code").getInt(1), true) + " "
                + Math.round(daily.getJSONArray("temperature_2m_min").getDouble(1)) + "°/"
                + Math.round(daily.getJSONArray("temperature_2m_max").getDouble(1)) + "°");
        return TextUtils.join("   ", parts);
    }

    /** WMO weather code -> emoji. */
    private static String icon(int code, boolean day) {
        if (code == 0) return day ? "☀️" : "🌙";
        if (code <= 2) return day ? "🌤️" : "☁️";
        if (code == 3) return "☁️";
        if (code <= 48) return "🌫️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌦️";
        if (code <= 86) return "🌨️";
        return "⛈️";
    }

    private static String describe(int code) {
        if (code == 0) return "Despejado";
        if (code <= 2) return "Algo nublado";
        if (code == 3) return "Nublado";
        if (code <= 48) return "Niebla";
        if (code <= 57) return "Llovizna";
        if (code <= 67) return "Lluvia";
        if (code <= 77) return "Nieve";
        if (code <= 82) return "Chaparrones";
        if (code <= 86) return "Nevadas";
        return "Tormenta";
    }
}
