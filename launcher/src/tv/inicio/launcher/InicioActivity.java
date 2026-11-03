package tv.inicio.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Home screen: rotating wallpapers, clock, weather with forecast, and
 * compact named rows of app banners that all fit on one screen. Long-press OK
 * on an app to move, hide or uninstall it; the layout is saved.
 * Built from plain framework views. Apart from the small pet (Bichito), nothing
 * animates while idle, so the screen is only redrawn on navigation, clock minutes,
 * weather refreshes and wallpaper changes.
 */
public class InicioActivity extends Activity {
    private static final int ACCENT = 0xFFB26BFF;      // neon violet
    private static final int CARD_BG = 0xFF161B26;
    private static final int TEXT = Ui.TEXT;
    private static final int TEXT_DIM = Ui.TEXT_DIM;

    private static final int CARD_W = 96, CARD_H = 56;  // dp, including the 3dp focus ring
    private static final long WALLPAPER_EVERY_MS = 60 * 60 * 1000;
    private static final long NIGHT_CHECK_MS = 5 * 60 * 1000;
    private static final int NIGHT_FROM = 22, NIGHT_TO = 7;
    private static final File WALLPAPERS = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Fondos");

    private static final String MORE = "Más";
    /** Default rows; the user's layout (saved in prefs) overrides these. Unassigned apps go to "Más". */
    private static final Map<String, List<String>> DEFAULT_ROWS = new LinkedHashMap<>();
    static {
        DEFAULT_ROWS.put("Streaming", Arrays.asList("com.netflix.ninja", "com.disney.disneyplus", "com.wbd.stream",
                "com.amazon.amazonvideo.livingroom"));
        DEFAULT_ROWS.put("En vivo", Arrays.asList("tv.twitch.android.app", "com.kick.mobile",
                "com.google.android.youtube.tv"));
        DEFAULT_ROWS.put("Mi servidor", Arrays.asList("com.plexapp.android", "org.jellyfin.androidtv", "tv.emby.embyatv"));
        DEFAULT_ROWS.put(MORE, new ArrayList<String>());
    }
    private static final List<String> NEVER_SHOWN = Arrays.asList(
            "tv.inicio.launcher", "com.google.android.apps.tv.launcherx", "com.android.tv.settings");
    private static final String DEFAULT_HIDDEN = "com.tcl.suspension,com.tcl.tv,com.google.android.videos";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable wallpaperTick = () -> nextWallpaper(true);
    private final Runnable nightTick = this::applyNightMode;

    private SharedPreferences prefs;
    private ImageView wallpaper;
    private View night;
    private Bichito bichito;
    private static final String NETFLIX = "com.netflix.ninja";
    private static final long CHEER_DANCE_MS = 700;
    private Runnable pendingOpen;
    private ClimaView clima;
    private LinearLayout rows;
    private ScrollView rowsScroll;
    private TextView focusedName;
    private String appsSignature = "";
    private View firstCard;
    private final Map<String, View> cardsByPkg = new HashMap<>();
    /** What is on screen now: row name -> packages in order. */
    private final Map<String, List<String>> shownRows = new LinkedHashMap<>();
    private final Map<String, App> appsByPkg = new HashMap<>();
    private long wallpaperChangedAt = 0;
    private boolean leftForApp = false, swallowOkUp = false;
    private AlertDialog menu;

    private static final class App {
        final String pkg;
        final CharSequence label;
        final Drawable banner;
        final Drawable icon;
        final Intent intent;
        final boolean system;

        App(String pkg, CharSequence label, Drawable banner, Drawable icon, Intent intent, boolean system) {
            this.pkg = pkg;
            this.label = label;
            this.banner = banner;
            this.icon = icon;
            this.intent = intent;
            this.system = system;
        }
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("inicio", MODE_PRIVATE);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        wallpaper = new ImageView(this);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setImageResource(R.drawable.fondo);
        root.addView(wallpaper, matchParent());

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0x40000000, 0xA6060A12, 0xD9060A12, 0xF2060A12}));
        root.addView(shade, matchParent());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(48), dp(24), dp(48), dp(16));
        content.addView(header());

        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        // More rows than fit on one screen: it scrolls as the focus moves down.
        rowsScroll = new ScrollView(this);
        rowsScroll.setVerticalScrollBarEnabled(false);
        rowsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rowsScroll.addView(rows);
        LinearLayout.LayoutParams rowsSize = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        rowsSize.topMargin = dp(14);
        content.addView(rowsScroll, rowsSize);
        root.addView(content, matchParent());

        // Runs in the free space right of the last row, right up to the corner of the screen.
        bichito = new Bichito(this);
        root.addView(bichito, new FrameLayout.LayoutParams(dp(376), dp(104), Gravity.BOTTOM | Gravity.END));
        bichito.setLogo(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
        // Its fireworks go off above its own space.
        root.setClipChildren(false);

        clima.setPet(bichito);

        night = new View(this);
        night.setBackgroundColor(Color.BLACK);
        night.setAlpha(0f);
        root.addView(night, matchParent());

        setContentView(root);
        nextWallpaper(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadIfChanged(false);

        long now = System.currentTimeMillis();
        if (leftForApp || now - wallpaperChangedAt >= WALLPAPER_EVERY_MS) nextWallpaper(false);
        leftForApp = false;
        handler.removeCallbacks(wallpaperTick);
        handler.postDelayed(wallpaperTick, WALLPAPER_EVERY_MS);

        handler.removeCallbacks(nightTick);
        applyNightMode();
        clima.start();
        bichito.start();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        clima.stop();
        bichito.stop();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already home: jump back to the first app.
        if (firstCard != null) firstCard.requestFocus();
        bichito.hop();
        String demo = intent.getStringExtra("demo");
        if (demo != null) bichito.demo(demo);
    }

    @Override
    public void onBackPressed() {
        // The home screen has nowhere to go back to.
    }

    /**
     * Long-press OK opens the edit menu. Besides the view's own long-click timer, treat the
     * first key repeat (or a long-press flagged event) as a long press, since some remotes
     * and injected events never let that timer fire; the matching key-up is swallowed.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) bichito.poke();
        int k = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
            if (k == KeyEvent.KEYCODE_DPAD_LEFT) bichito.arrow(-1, 0);
            if (k == KeyEvent.KEYCODE_DPAD_RIGHT) bichito.arrow(1, 0);
            if (k == KeyEvent.KEYCODE_DPAD_UP) bichito.arrow(0, -1);
            if (k == KeyEvent.KEYCODE_DPAD_DOWN) bichito.arrow(0, 1);
        }
        if (k == KeyEvent.KEYCODE_DPAD_CENTER || k == KeyEvent.KEYCODE_ENTER || k == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && !swallowOkUp
                    && (e.getRepeatCount() == 1 || e.isLongPress())) {
                View f = getCurrentFocus();
                if (f != null && f.isLongClickable()) {
                    swallowOkUp = true;
                    f.setPressed(false);
                    f.performLongClick();
                    return true;
                }
            }
            if (swallowOkUp) {
                if (e.getAction() == KeyEvent.ACTION_UP) swallowOkUp = false;
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    // ---------------------------------------------------------------- header

    private View header() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.TOP);

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        styleText(clock, TEXT, 44, "sans-serif-thin");
        clockBox.addView(clock);

        TextClock date = new TextClock(this);
        String pattern = DateFormat.getBestDateTimePattern(getResources().getConfiguration().locale, "EEEEdMMMM");
        date.setFormat24Hour(pattern);
        date.setFormat12Hour(pattern);
        styleText(date, TEXT_DIM, 12, "sans-serif-light");
        date.setAllCaps(true);
        date.setLetterSpacing(0.12f);
        clockBox.addView(date);

        focusedName = new TextView(this);
        styleText(focusedName, ACCENT, 14, "sans-serif-medium");
        focusedName.setPadding(0, dp(8), 0, 0);
        clockBox.addView(focusedName);
        bar.addView(clockBox);
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 0, 1f));

        clima = new ClimaView(this);
        clima.setPadding(0, 0, dp(24), 0);
        bar.addView(clima);

        TextView settings = pill("Ajustes");
        settings.setOnClickListener(v -> launch(new Intent(Settings.ACTION_SETTINGS)));
        bar.addView(settings);
        return bar;
    }

    private void styleText(TextView t, int color, float sp, String family) {
        Ui.style(t, color, sp, family);
    }

    private TextView pill(String text) {
        TextView pill = new TextView(this);
        pill.setText(text);
        pill.setTextColor(TEXT);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        pill.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        pill.setLetterSpacing(0.06f);
        pill.setPadding(dp(16), dp(7), dp(16), dp(7));
        pill.setFocusable(true);
        pill.setClickable(true);
        if (Build.VERSION.SDK_INT >= 26) pill.setDefaultFocusHighlightEnabled(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0x33FFFFFF);
        pill.setBackground(bg);
        pill.setOnFocusChangeListener((v, focused) -> {
            bg.setColor(focused ? ACCENT : 0x33FFFFFF);
            if (focused) focusedName.setText("");
        });
        return pill;
    }

    // ------------------------------------------------------ wallpaper, night

    /** Shows the next image from Pictures/Fondos (falls back to the bundled one). */
    private void nextWallpaper(boolean crossfade) {
        File[] files = WALLPAPERS.listFiles((dir, name) -> {
            String n = name.toLowerCase(Locale.ROOT);
            return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
        });
        wallpaperChangedAt = System.currentTimeMillis();
        if (files == null || files.length == 0) return;
        Arrays.sort(files);
        int index = (prefs.getInt("wallpaper", -1) + 1) % files.length;
        prefs.edit().putInt("wallpaper", index).apply();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(files[index].getPath(), bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (bounds.outWidth / (opts.inSampleSize * 2) >= getResources().getDisplayMetrics().widthPixels) {
            opts.inSampleSize *= 2;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(files[index].getPath(), opts);
        if (bitmap == null) return;

        Drawable next = new BitmapDrawable(getResources(), bitmap);
        Drawable current = wallpaper.getDrawable();
        if (current instanceof TransitionDrawable) current = ((TransitionDrawable) current).getDrawable(1);
        if (crossfade && current != null) {
            TransitionDrawable fade = new TransitionDrawable(new Drawable[] {current, next});
            fade.setCrossFadeEnabled(true);
            wallpaper.setImageDrawable(fade);
            fade.startTransition(800);
        } else {
            wallpaper.setImageDrawable(next);
        }
    }

    private void applyNightMode() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isNight = hour >= NIGHT_FROM || hour < NIGHT_TO;
        float target = isNight ? 0.45f : 0f;
        if (night.getAlpha() != target) night.animate().alpha(target).setDuration(1500).start();
        bichito.setNightTime(isNight);
        handler.postDelayed(nightTick, NIGHT_CHECK_MS);
    }

    // ------------------------------------------------------------------ apps

    private void reloadIfChanged(boolean force) {
        List<App> apps = loadApps();
        StringBuilder sig = new StringBuilder();
        for (App a : apps) sig.append(a.pkg).append(';');
        if (force || !sig.toString().equals(appsSignature)) {
            appsSignature = sig.toString();
            buildRows(apps);
        }
    }

    private List<App> loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<App> apps = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (NEVER_SHOWN.contains(pkg)) continue;
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    .setComponent(new ComponentName(pkg, ri.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            boolean system = (ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            apps.add(new App(pkg, ri.loadLabel(pm), ri.activityInfo.loadBanner(pm), ri.loadIcon(pm), launch, system));
        }
        // Sideloaded phone apps have no TV launcher entry; show user-installed ones with their icon.
        Set<String> seen = new LinkedHashSet<>();
        for (App a : apps) seen.add(a.pkg);
        Intent phone = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(phone, 0)) {
            String pkg = ri.activityInfo.packageName;
            if ((ri.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (NEVER_SHOWN.contains(pkg) || !seen.add(pkg)) continue;
            Intent launch = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(new ComponentName(pkg, ri.activityInfo.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            apps.add(new App(pkg, ri.loadLabel(pm), null, ri.loadIcon(pm), launch, false));
        }
        return apps;
    }

    private Set<String> hidden() {
        return new LinkedHashSet<>(Arrays.asList(TextUtils.split(prefs.getString("hidden", DEFAULT_HIDDEN), ",")));
    }

    /** Saved layout (or defaults): row -> packages. */
    private Map<String, List<String>> savedRows() {
        Map<String, List<String>> layout = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : DEFAULT_ROWS.entrySet()) {
            String saved = prefs.getString("row:" + e.getKey(), null);
            layout.put(e.getKey(), saved == null ? new ArrayList<>(e.getValue())
                    : new ArrayList<>(Arrays.asList(TextUtils.split(saved, ","))));
        }
        return layout;
    }

    private void saveRows() {
        SharedPreferences.Editor ed = prefs.edit();
        for (Map.Entry<String, List<String>> e : shownRows.entrySet()) {
            ed.putString("row:" + e.getKey(), TextUtils.join(",", e.getValue()));
        }
        ed.apply();
    }

    private void buildRows(List<App> apps) {
        rows.removeAllViews();
        rowsScroll.scrollTo(0, 0);
        cardsByPkg.clear();
        appsByPkg.clear();
        shownRows.clear();
        firstCard = null;
        for (App a : apps) appsByPkg.put(a.pkg, a);

        Set<String> hidden = hidden();
        Map<String, List<String>> layout = savedRows();
        Set<String> placed = new LinkedHashSet<>();
        for (Map.Entry<String, List<String>> e : layout.entrySet()) {
            List<String> pkgs = new ArrayList<>();
            for (String pkg : e.getValue()) {
                if (appsByPkg.containsKey(pkg) && !hidden.contains(pkg) && placed.add(pkg)) pkgs.add(pkg);
            }
            shownRows.put(e.getKey(), pkgs);
        }
        for (App a : apps) {
            if (!hidden.contains(a.pkg) && placed.add(a.pkg)) shownRows.get(MORE).add(a.pkg);
        }

        for (Map.Entry<String, List<String>> e : shownRows.entrySet()) {
            addRow(e.getKey(), e.getValue());
        }
        addInputsRow();
        if (firstCard != null) firstCard.requestFocus();
    }

    private LinearLayout rowLine(String title, TextView[] labelOut) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setClipChildren(false);
        line.setPadding(0, 0, 0, dp(8));

        TextView label = new TextView(this);
        label.setText(title);
        label.setAllCaps(true);
        label.setLetterSpacing(0.14f);
        styleText(label, TEXT_DIM, 11, "sans-serif-medium");
        line.addView(label, new LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT));
        labelOut[0] = label;
        return line;
    }

    private LinearLayout strip(LinearLayout line) {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setClipChildren(false);
        scroller.setClipToPadding(false);
        LinearLayout strip = new LinearLayout(this);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setClipChildren(false);
        strip.setClipToPadding(false);
        scroller.addView(strip);
        line.addView(scroller, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return strip;
    }

    /** One line: row name on the left, cards scrolling to the right. */
    private void addRow(String title, List<String> pkgs) {
        if (pkgs.isEmpty()) return;
        TextView[] label = new TextView[1];
        LinearLayout line = rowLine(title, label);
        LinearLayout strip = strip(line);
        for (String pkg : pkgs) {
            App a = appsByPkg.get(pkg);
            View card = card(a.label, a.banner, a.icon, a.intent, label[0]);
            card.setOnLongClickListener(v -> {
                showMenu(a, title);
                return true;
            });
            cardsByPkg.put(pkg, card);
            strip.addView(card);
        }
        if (firstCard == null) firstCard = strip.getChildAt(0);
        rows.addView(line);
    }

    private void addInputsRow() {
        Map<String, Intent> inputs = inputs();
        if (inputs.isEmpty()) return;
        TextView[] label = new TextView[1];
        LinearLayout line = rowLine("Entradas", label);
        LinearLayout strip = strip(line);
        for (Map.Entry<String, Intent> in : inputs.entrySet()) {
            strip.addView(card(in.getKey(), null, null, in.getValue(), label[0]));
        }
        rows.addView(line);
    }

    private Map<String, Intent> inputs() {
        Map<String, Intent> inputs = new TreeMap<>();
        TvInputManager tim = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        if (tim == null) return inputs;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getType() != TvInputInfo.TYPE_HDMI || info.getParentId() != null || info.isHidden(this)) continue;
            Intent watch = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(info.getId()))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CharSequence name = info.loadCustomLabel(this);
            if (name == null) name = info.loadLabel(this);
            inputs.put(name.toString(), watch);
        }
        return inputs;
    }

    // ------------------------------------------------------------- edit menu

    private void showMenu(App app, String row) {
        if (menu != null && menu.isShowing()) return;
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        List<String> pkgs = shownRows.get(row);
        int pos = pkgs.indexOf(app.pkg);
        if (pos > 0) {
            labels.add("◀  Mover a la izquierda");
            actions.add(() -> { pkgs.remove(pos); pkgs.add(pos - 1, app.pkg); applyLayout(app.pkg); });
        }
        if (pos < pkgs.size() - 1) {
            labels.add("▶  Mover a la derecha");
            actions.add(() -> { pkgs.remove(pos); pkgs.add(pos + 1, app.pkg); applyLayout(app.pkg); });
        }
        for (String other : shownRows.keySet()) {
            if (other.equals(row)) continue;
            labels.add("⇅  Pasar a «" + other + "»");
            actions.add(() -> { pkgs.remove(app.pkg); shownRows.get(other).add(app.pkg); applyLayout(app.pkg); });
        }
        labels.add("✕  Ocultar");
        actions.add(() -> {
            Set<String> h = hidden();
            h.add(app.pkg);
            prefs.edit().putString("hidden", TextUtils.join(",", h)).apply();
            pkgs.remove(app.pkg);
            applyLayout(null);
        });
        if (!app.system) {
            labels.add("🗑  Desinstalar");
            actions.add(() -> {
                leftForApp = true;
                startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg)));
            });
        }
        List<String> hiddenNow = new ArrayList<>();
        for (String h : hidden()) if (appsByPkg.containsKey(h)) hiddenNow.add(h);
        if (!hiddenNow.isEmpty()) {
            labels.add("👁  Mostrar apps ocultas (" + hiddenNow.size() + ")");
            actions.add(() -> showHidden(hiddenNow));
        }
        menu = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(app.label)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private void showHidden(List<String> hiddenNow) {
        String[] names = new String[hiddenNow.size()];
        for (int i = 0; i < names.length; i++) names[i] = appsByPkg.get(hiddenNow.get(i)).label.toString();
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Mostrar de nuevo")
                .setItems(names, (d, which) -> {
                    Set<String> h = hidden();
                    h.remove(hiddenNow.get(which));
                    prefs.edit().putString("hidden", TextUtils.join(",", h)).apply();
                    reloadIfChanged(true);
                    View card = cardsByPkg.get(hiddenNow.get(which));
                    if (card != null) card.requestFocus();
                })
                .show();
    }

    private void applyLayout(String focusPkg) {
        saveRows();
        reloadIfChanged(true);
        View card = focusPkg == null ? null : cardsByPkg.get(focusPkg);
        if (card != null) card.requestFocus();
    }

    // ----------------------------------------------------------------- cards

    /** A small 16:9 tile; banner if the app has one, otherwise icon + name on a dark card. */
    private View card(CharSequence name, Drawable banner, Drawable icon, Intent intent, TextView rowLabel) {
        // Focusable frame with the ring; the banner is pre-rounded so nothing is clipped at draw time.
        FrameLayout frame = new FrameLayout(this);
        frame.setFocusable(true);
        frame.setClickable(true);
        if (Build.VERSION.SDK_INT >= 26) frame.setDefaultFocusHighlightEnabled(false);
        frame.setPadding(dp(3), dp(3), dp(3), dp(3));
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(dp(10));
        ring.setColor(Color.TRANSPARENT);
        ring.setStroke(dp(3), Color.TRANSPARENT);
        frame.setBackground(ring);

        FrameLayout tile = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(7));
        bg.setColor(CARD_BG);
        tile.setBackground(bg);
        int w = dp(CARD_W - 6), h = dp(CARD_H - 6);

        if (banner != null) {
            ImageView img = new ImageView(this);
            img.setImageBitmap(rounded(banner, w, h, dp(7)));
            tile.addView(img, matchParent());
        } else {
            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            if (icon != null) {
                ImageView img = new ImageView(this);
                img.setImageDrawable(icon);
                inner.addView(img, new LinearLayout.LayoutParams(dp(22), dp(22)));
            }
            TextView t = new TextView(this);
            t.setText(name);
            t.setTextColor(TEXT);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, icon != null ? 9 : 13);
            t.setTypeface(Typeface.create(icon != null ? "sans-serif" : "sans-serif-light", Typeface.NORMAL));
            t.setGravity(Gravity.CENTER);
            t.setMaxLines(1);
            t.setPadding(dp(4), dp(3), dp(4), 0);
            inner.addView(t);
            tile.addView(inner, matchParent());
        }
        frame.addView(tile, matchParent());

        frame.setOnFocusChangeListener((v, focused) -> {
            ring.setStroke(dp(3), focused ? ACCENT : Color.TRANSPARENT);
            rowLabel.setTextColor(focused ? ACCENT : TEXT_DIM);
            if (focused) focusedName.setText(name);
        });
        frame.setOnClickListener(v -> launch(intent));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(CARD_W), dp(CARD_H));
        lp.rightMargin = dp(10);
        frame.setLayoutParams(lp);
        return frame;
    }

    /** Renders a drawable center-cropped into a w x h bitmap with rounded corners. */
    private static Bitmap rounded(Drawable d, int w, int h, float radius) {
        int dw = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : w;
        int dh = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : h;
        Bitmap src = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, dw, dh);
        d.draw(new Canvas(src));

        float scale = Math.max((float) w / dw, (float) h / dh);
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate((w - dw * scale) / 2f, (h - dh * scale) / 2f);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shader.setLocalMatrix(m);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setShader(shader);

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        new Canvas(out).drawRoundRect(new RectF(0, 0, w, h), radius, radius, p);
        return out;
    }

    /** The pet cheers first: a dance for Netflix, and the app opens right after. */
    private void launch(Intent intent) {
        handler.removeCallbacks(pendingOpen);
        ComponentName target = intent.getComponent();
        String pkg = target != null ? target.getPackageName() : intent.getPackage();
        pendingOpen = () -> open(intent);
        if (NETFLIX.equals(pkg)) {
            bichito.dance();
            handler.postDelayed(pendingOpen, CHEER_DANCE_MS);
        } else {
            open(intent);
        }
    }

    private void open(Intent intent) {
        leftForApp = true;
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            // Inputs: fall back to TCL's live TV app, which can switch sources itself.
            Intent tv = getPackageManager().getLaunchIntentForPackage("com.tcl.tv");
            if (tv != null && Intent.ACTION_VIEW.equals(intent.getAction())) startActivity(tv);
        }
    }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
