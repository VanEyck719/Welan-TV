package tv.inicio.protector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import tv.inicio.launcher.Bichito;
import tv.inicio.launcher.ClimaView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Screensaver that cycles the bundled image and every video/image found in Movies/Protector
 * on the TV (sorted by name), SLIDE_MS each, with a short dip to black between items.
 * On top: the weather and the launcher's pet.
 */
public class ProtectorDream extends DreamService implements TextureView.SurfaceTextureListener {
    private static final String TAG = "Protector";
    private static final long SLIDE_MS = 20_000;
    private static final long FADE_MS = 400;
    private static final int NIGHT_FROM = 22, NIGHT_TO = 7;  // same as the launcher
    private static final File FOLDER = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Protector");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable next = this::showNext;
    /** Playlist; null stands for the bundled image. */
    private final List<File> items = new ArrayList<>();
    private int index = 0;
    private int consecutiveErrors = 0;

    private ImageView image;
    private View black;
    private MediaPlayer player;
    private Surface surface;
    private ClimaView clima;
    private Bichito pet;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);

        items.add(null);
        File[] files = FOLDER.listFiles();
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                if (isVideo(f) || isImage(f)) items.add(f);
            }
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        TextureView video = new TextureView(this);
        video.setSurfaceTextureListener(this);
        root.addView(video, full);

        image = new ImageView(this);
        image.setBackgroundColor(Color.BLACK);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(R.drawable.fondo);
        root.addView(image, new FrameLayout.LayoutParams(full));

        black = new View(this);
        black.setBackgroundColor(Color.BLACK);
        black.setAlpha(0f);
        root.addView(black, new FrameLayout.LayoutParams(full));
        addOverlays(root);

        setContentView(root);
        Log.i(TAG, "playlist: " + items.size() + " items from " + FOLDER);
        if (items.size() > 1) handler.postDelayed(next, SLIDE_MS);
    }

    /** Above the dip to black, so they stay put while the slides change. */
    private void addOverlays(FrameLayout root) {
        float density = getResources().getDisplayMetrics().density;
        int margin = Math.round(24 * density);

        clima = new ClimaView(this);
        FrameLayout.LayoutParams corner = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        corner.setMargins(margin, margin, margin, margin);
        root.addView(clima, corner);

        pet = new Bichito(this);
        root.addView(pet, new FrameLayout.LayoutParams(
                Math.round(376 * density), Math.round(104 * density), Gravity.BOTTOM | Gravity.END));
        pet.setLogo(BitmapFactory.decodeResource(getResources(), R.drawable.logo));
        // Its fireworks go off above its own space.
        root.setClipChildren(false);
        clima.setPet(pet);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        pet.setNightTime(hour >= NIGHT_FROM || hour < NIGHT_TO);
        clima.start();
        pet.start();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        surface = new Surface(texture);
        player = new MediaPlayer();
        player.setSurface(surface);
        player.setLooping(true);
        player.setVolume(0f, 0f);
    }

    private void showNext() {
        index = (index + 1) % items.size();
        black.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> load(items.get(index)));
    }

    private void load(File item) {
        Log.i(TAG, "show " + (item == null ? "bundled image" : item.getName()));
        if (item == null || isImage(item)) {
            if (player != null) player.reset();
            if (item == null) {
                image.setImageResource(R.drawable.fondo);
            } else {
                Bitmap bitmap = decode(item);
                if (bitmap == null) { onItemFailed(); return; }
                image.setImageBitmap(bitmap);
            }
            image.setVisibility(View.VISIBLE);
            reveal();
            return;
        }
        if (player == null) { onItemFailed(); return; }
        try {
            player.reset();
            player.setSurface(surface);
            player.setLooping(true);
            player.setVolume(0f, 0f);
            player.setDataSource(item.getPath());
            player.setOnPreparedListener(mp -> {
                mp.start();
                image.setVisibility(View.GONE);
                reveal();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "error " + what + "/" + extra + " playing " + item.getName());
                onItemFailed();
                return true;
            });
            player.prepareAsync();
        } catch (Exception e) {
            Log.w(TAG, "cannot open " + item.getName(), e);
            onItemFailed();
        }
    }

    private void reveal() {
        consecutiveErrors = 0;
        black.animate().alpha(0f).setDuration(FADE_MS).withEndAction(null);
        handler.postDelayed(next, SLIDE_MS);
    }

    /** Skips a broken item; falls back to the bundled image if nothing in the folder plays. */
    private void onItemFailed() {
        if (++consecutiveErrors >= items.size()) {
            items.subList(1, items.size()).clear();
            index = 0;
            load(null);
            handler.removeCallbacks(next);
            return;
        }
        handler.post(this::showNextNow);
    }

    private void showNextNow() {
        index = (index + 1) % items.size();
        load(items.get(index));
    }

    private static Bitmap decode(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (bounds.outWidth / (opts.inSampleSize * 2) >= 1920) opts.inSampleSize *= 2;
        return BitmapFactory.decodeFile(file.getPath(), opts);
    }

    private static boolean isVideo(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".webm");
    }

    private static boolean isImage(File f) {
        String n = f.getName().toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
    }

    @Override
    public void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        clima.stop();
        pet.stop();
        if (player != null) {
            player.release();
            player = null;
        }
        if (surface != null) {
            surface.release();
            surface = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
    }
}
