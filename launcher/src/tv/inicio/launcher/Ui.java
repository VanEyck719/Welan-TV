package tv.inicio.launcher;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Small helpers shared by the launcher and the screensaver (the protector compiles these sources too). */
final class Ui {
    static final int TEXT = 0xFFF2F5FA;
    static final int TEXT_DIM = 0xB3DCE3EE;

    private Ui() {
    }

    /** The on-screen text follows the TV's language: Spanish if it's set to Spanish, otherwise English. */
    static String t(Context context, String english, String spanish) {
        return "es".equals(context.getResources().getConfiguration().locale.getLanguage()) ? spanish : english;
    }

    static int dp(Context context, float v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    static void style(TextView t, int color, float sp, String family) {
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(Typeface.create(family, Typeface.NORMAL));
        t.setShadowLayer(dp(t.getContext(), 6), 0, 0, 0x99000000);
    }

    static String get(String url, int timeoutMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        try (InputStream in = c.getInputStream()) {
            return readAll(in);
        } finally {
            c.disconnect();
        }
    }

    static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        return out.toString("UTF-8");
    }
}
