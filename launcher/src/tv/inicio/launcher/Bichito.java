package tv.inicio.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hyperactive pixel-art pet: it runs along the bottom of the screen up to the corner, hops, blinks,
 * pretends to be busy and says hello in a pixel speech bubble. It dresses for the weather. Given a
 * logo, it keeps it in the corner and plays with it: stomps on it, pushes it, carries it around,
 * shelters under it from the rain and sleeps on it. It follows the remote's arrows with its eyes,
 * hides a secret code, cheers when Netflix opens and dresses up on special dates. It only redraws
 * while acting; during its short rests it draws nothing.
 */
public class Bichito extends View {
    private static final int BODY = 0xFFB26BFF, EYE = 0xFF1C1310, BUBBLE = 0xFFF6F1E7, WORD = 0xFFE6E1D8;
    private static final String[] TORSO = {
            "..XXXXXXXXX..",
            "..XXXXXXXXX..",
            "..XXXXXXXXX..",
            "XXXXXXXXXXXXX",
            "XXXXXXXXXXXXX",
            "..XXXXXXXXX..",
            "..XXXXXXXXX..",
    };
    private static final int COLS = 13, ROWS = 9, LEG_ROW = 7, EYE_ROW = 2;
    private static final int[] LEGS = {2, 4, 8, 10};
    private static final int[] EYES = {4, 8};
    private static final int[] HOP_HEIGHT = {1, 2, 3, 3, 2, 1, 0};

    private static final String SPINNER = "|/-\\";
    /** What it pretends to be doing. */
    private static final String[] WORDS = {
            "Thinking", "Cooking", "Calculating", "Reading", "Strolling", "Tidying", "Whistling",
            "Dancing", "Drawing", "Dreaming", "Researching", "Sweeping", "Sniffing", "Trotting",
    };
    private static final String[] WORDS_ES = {
            "Pensando", "Cocinando", "Calculando", "Leyendo", "Paseando", "Ordenando", "Silbando",
            "Bailando", "Dibujando", "Soñando", "Investigando", "Barriendo", "Husmeando", "Trotando",
    };
    private static final String[] SNORES = {"z", "zZ", "zZz"};

    private static final String LETTERS = "HolaFn¡!BiezñNvdYyMrChstmpw";
    private static final String[][] GLYPHS = {
            {"X...X", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"},
            {".....", ".....", ".XXX.", "X...X", "X...X", "X...X", ".XXX."},
            {".XX..", "..X..", "..X..", "..X..", "..X..", "..X..", ".XXX."},
            {".....", ".....", ".XXX.", "....X", ".XXXX", "X...X", ".XXXX"},
            {"XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "X...."},
            {".....", ".....", "X.XX.", "XX..X", "X...X", "X...X", "X...X"},
            {"..X..", ".....", "..X..", "..X..", "..X..", "..X..", "..X.."},
            {"..X..", "..X..", "..X..", "..X..", "..X..", ".....", "..X.."},
            {"XXXX.", "X...X", "X...X", "XXXX.", "X...X", "X...X", "XXXX."},
            {"..X..", ".....", ".XX..", "..X..", "..X..", "..X..", ".XXX."},
            {".....", ".....", ".XXX.", "X...X", "XXXXX", "X....", ".XXX."},
            {".....", ".....", "XXXXX", "...X.", "..X..", ".X...", "XXXXX"},
            {".XXX.", ".....", "X.XX.", "XX..X", "X...X", "X...X", "X...X"},
            {"X...X", "XX..X", "XX..X", "X.X.X", "X..XX", "X..XX", "X...X"},
            {".....", ".....", "X...X", "X...X", "X...X", ".X.X.", "..X.."},
            {"....X", "....X", ".XX.X", "X..XX", "X...X", "X...X", ".XXXX"},
            {"X...X", "X...X", ".X.X.", "..X..", "..X..", "..X..", "..X.."},
            {".....", ".....", "X...X", "X...X", ".XXXX", "....X", ".XXX."},
            {"X...X", "XX.XX", "X.X.X", "X.X.X", "X...X", "X...X", "X...X"},
            {".....", ".....", "X.XX.", "XX..X", "X....", "X....", "X...."},
            {".XXX.", "X...X", "X....", "X....", "X....", "X...X", ".XXX."},
            {"X....", "X....", "X.XX.", "XX..X", "X...X", "X...X", "X...X"},
            {".....", ".....", ".XXXX", "X....", ".XXX.", "....X", "XXXX."},
            {"..X..", "..X..", "XXXXX", "..X..", "..X..", "..X..", "...XX"},
            {".....", ".....", "XX.X.", "X.X.X", "X.X.X", "X.X.X", "X.X.X"},
            {".....", ".....", "XXXX.", "X...X", "XXXX.", "X....", "X...."},
            {".....", ".....", "X...X", "X...X", "X.X.X", "X.X.X", ".X.X."},
    };
    private static final int GLYPH_W = 5, GLYPH_H = 7;

    private static final int UMBRELLA_COLOR = 0xFF5B8DEF, SCARF_COLOR = 0xFFE5484D, DROP_COLOR = 0xFF7CC4FF;
    private static final int[] CONFETTI = {0xFFFFD166, 0xFF06D6A0, 0xFF5B8DEF, 0xFFEF476F, 0xFFF6F1E7};
    private static final String[] UMBRELLA = {
            "....XXXXX....",
            "..XXXXXXXXX..",
            ".XXXXXXXXXXX.",
            "......X......",
            "......X......",
    };

    private static final long WALK_MS = 80, THINK_MS = 120, LOOK_MS = 280, BLINK_MS = 120, HOP_MS = 60, SNORE_MS = 1200;
    private static final long POP_MS = 50, SAY_MS = 2500, SAY_AGAIN_MS = 20 * 1000;
    private static final int THINK_FRAMES = 18, POP_FRAMES = 3, PARTY_FRAMES = 28;
    private static final long AWAKE_AFTER_POKE_MS = 60 * 1000;  // less than the 2-minute screensaver
    private static final int RAIN_FROM = 51, RAIN_TO = 67, SHOWERS_FROM = 80, SHOWERS_TO = 82, STORM_FROM = 95;
    private static final int SNOW_FROM = 71, SNOW_TO = 77, SNOW_SHOWERS_FROM = 85, SNOW_SHOWERS_TO = 86;
    private static final int SCARF_UP_TO = 12, SUNGLASSES_FROM = 26;

    private static final int LOGO_ROWS = 7, LOGO_MARGIN = 4, LOGO_FLOOR = 2, JUMP_ROWS = 4, JUMP_FRAMES = 8, STOMP_FRAMES = 12, STOMPS = 3;
    private static final int CARRY_LIFT = 6, CARRY_TRIP = 12, CARRY_DROP = 4;
    /** How far the logo gives way when pushed, in pet pixels; it then springs back past its place. */
    private static final int[] PUSH_PATH = {0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 3, 2, 1, 0, -1, 0};
    private static final long PUSH_MS = 110, VISIT_AGAIN_MS = 15 * 1000;
    private static final long SHELTER_AGAIN_MS = 20 * 1000, FIREWORKS_AGAIN_MS = 20 * 1000, LOOK_AT_ARROW_MS = 1200;
    private static final int SHELTER_HOLD = 40, DANCE_FRAMES = 14, GIANT_FRAMES = 70, FIREWORK_FRAMES = 60;
    private static final float GIANT_SCALE = 2.5f;
    /** Up, up, down, down, left, right, left, right on the remote; rows hold {dx, dy}. */
    private static final int[][] SECRET = {{0, -1}, {0, -1}, {0, 1}, {0, 1}, {-1, 0}, {1, 0}, {-1, 0}, {1, 0}};
    private static final int XMAS_FROM_DAY = 20, XMAS_TO_DAY = 26;
    private static final int HAT_COLOR = 0xFFD62839, FUR_COLOR = 0xFFF6F1E7;

    private enum Mode { IDLE, WALK, THINK, LOOK, BLINK, HOP, SAY, SLEEP, PARTY, CLIMB, PUSH, CARRY, BED, WAKE, SHELTER, DANCE, GIANT, FIREWORKS }
    private enum Fiesta { NONE, XMAS, NEW_YEAR }
    private static final Mode[] TRICKS = {Mode.CLIMB, Mode.PUSH, Mode.CARRY};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::tick;
    private final Random random = new Random();
    /** Shuffled once and walked in order, so every word comes up before any repeats. */
    private final List<String> words;
    private final String hello, hooray, xmasHello, newYearHello;
    private int nextWord;
    private final Paint pixels = new Paint();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint logoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect logoRect = new Rect();
    private final int p;

    private Mode mode = Mode.IDLE;
    private Mode afterWalk;
    private int frame, x = -1, targetX, gaze, lift;
    private Bitmap logo;
    private int logoW, logoH, logoShift, logoUp, logoDip;
    private boolean nightTime, placed, greet, umbrella, scarf, sunglasses;
    private long awakeUntil, saidAt, visitedAt, shelteredAt, fireworksAt, lookUntil;
    private String word = "", saying;
    private Fiesta fiesta = Fiesta.NONE;
    private int lookX, lookY, secretStep;

    public Bichito(Context context) {
        super(context);
        p = Math.round(4 * getResources().getDisplayMetrics().density);
        hello = saying = Ui.t(context, "Hello!", "¡Hola!");
        hooray = Ui.t(context, "Yay!", "¡Bien!");
        xmasHello = Ui.t(context, "Merry Christmas!", "¡Feliz Navidad!");
        newYearHello = Ui.t(context, "Happy New Year!", "¡Feliz año!");
        boolean spanish = hello.equals("¡Hola!");
        words = new ArrayList<>(Arrays.asList(spanish ? WORDS_ES : WORDS));
        Collections.shuffle(words, random);
        text.setColor(BODY);
        text.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics()));
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        text.setShadowLayer(p, 0, 0, 0xCC000000);
    }

    /** Coming back to the home screen: awake, and it says hello first. */
    public void start() {
        handler.removeCallbacks(tick);
        awakeUntil = System.currentTimeMillis() + AWAKE_AFTER_POKE_MS;
        greet = true;
        mode = Mode.IDLE;
        frame = 0;
        fiesta = fiestaToday();
        // It may have been left mid-trick, on top of the logo.
        afterWalk = null;
        lift = logoShift = logoUp = logoDip = 0;
        if (placed) x = Math.max(0, logo != null ? Math.min(x, besideLogo()) : Math.min(x, getWidth() - COLS * p));
        handler.postDelayed(tick, 800);
    }

    public void stop() {
        handler.removeCallbacks(tick);
    }

    /** Stands in the corner it runs to, just above the floor. */
    public void setLogo(Bitmap bitmap) {
        logo = bitmap;
        logoH = LOGO_ROWS * p;
        logoW = logoH * bitmap.getWidth() / bitmap.getHeight();
        invalidate();
    }

    /** At night it sleeps unless someone is using the remote. */
    public void setNightTime(boolean night) {
        nightTime = night;
    }

    /** A key was pressed: wakes it up, and it hops if it wasn't busy. */
    void poke() {
        awakeUntil = System.currentTimeMillis() + AWAKE_AFTER_POKE_MS;
        if (mode == Mode.SLEEP || mode == Mode.IDLE) hop();
    }

    /** Doesn't interrupt anything it's doing with the logo. */
    void hop() {
        if (playing()) return;
        afterWalk = null;
        enter(mode == Mode.SLEEP && lift > 0 ? Mode.WAKE : Mode.HOP);
    }

    private boolean playing() {
        switch (mode) {
            case CLIMB: case PUSH: case CARRY: case BED: case WAKE: case SHELTER: case DANCE: case GIANT: case FIREWORKS:
                return true;
            default:
                return false;
        }
    }

    /** Something is opening: a quick dance, for as long as the launcher waits before leaving. */
    void dance() {
        if (playing()) return;
        afterWalk = null;
        if (lift > 0) lift = 0;
        enter(Mode.DANCE);
    }

    /** An arrow on the remote: it looks that way, and the right sequence does something silly. */
    void arrow(int dx, int dy) {
        lookX = dx;
        lookY = dy;
        lookUntil = System.currentTimeMillis() + LOOK_AT_ARROW_MS;
        invalidate();
        handler.postDelayed(this::invalidate, LOOK_AT_ARROW_MS);

        boolean next = SECRET[secretStep][0] == dx && SECRET[secretStep][1] == dy;
        secretStep = next ? secretStep + 1 : SECRET[0][0] == dx && SECRET[0][1] == dy ? 1 : 0;
        if (secretStep == SECRET.length) {
            secretStep = 0;
            if (!playing()) {
                afterWalk = null;
                enter(random.nextBoolean() ? Mode.GIANT : Mode.PARTY);
            }
        }
    }

    /** Lets adb preview something without waiting for it: am start ... --es demo gigante */
    void demo(String what) {
        handler.postDelayed(() -> {
            handler.removeCallbacks(tick);
            afterWalk = null;
            lift = logoShift = logoUp = logoDip = 0;
            if (logo != null) x = Math.min(x, besideLogo());
            switch (what) {
                case "lluvia": umbrella = true; shelter(); break;
                case "dormir": nightTime = true; awakeUntil = 0; enter(Mode.IDLE); break;
                case "gigante": enter(Mode.GIANT); break;
                case "bailar": enter(Mode.DANCE); break;
                case "navidad": fiesta = Fiesta.XMAS; greet = true; enter(Mode.IDLE); break;
                case "anio": fiesta = Fiesta.NEW_YEAR; greet = false; enter(Mode.FIREWORKS); break;
                default: enter(Mode.IDLE);
            }
        }, 1000);
    }

    private static Fiesta fiestaToday() {
        Calendar now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH), day = now.get(Calendar.DAY_OF_MONTH);
        if ((month == Calendar.DECEMBER && day == 31) || (month == Calendar.JANUARY && day == 1)) return Fiesta.NEW_YEAR;
        if (month == Calendar.DECEMBER && day >= XMAS_FROM_DAY && day <= XMAS_TO_DAY) return Fiesta.XMAS;
        return Fiesta.NONE;
    }

    private String hello() {
        switch (fiesta) {
            case XMAS: return xmasHello;
            case NEW_YEAR: return newYearHello;
            default: return hello;
        }
    }

    /** Umbrella for rain, scarf for the cold, sunglasses for a hot sunny day. Takes a WMO weather code. */
    void setWeather(int code, int degrees, boolean day) {
        boolean rain = (code >= RAIN_FROM && code <= RAIN_TO) || (code >= SHOWERS_FROM && code <= SHOWERS_TO) || code >= STORM_FROM;
        boolean snow = (code >= SNOW_FROM && code <= SNOW_TO) || (code >= SNOW_SHOWERS_FROM && code <= SNOW_SHOWERS_TO);
        umbrella = rain;
        scarf = snow || degrees <= SCARF_UP_TO;
        sunglasses = day && code <= 1 && degrees >= SUNGLASSES_FROM;
        invalidate();
    }

    private void enter(Mode next) {
        mode = next;
        frame = 0;
        handler.removeCallbacks(tick);
        tick();
    }

    private void tick() {
        switch (mode) {
            case WALK:
                frame++;
                x += Integer.signum(targetX - x) * p;
                if (Math.abs(targetX - x) >= p) {
                    redrawIn(WALK_MS);
                } else if (afterWalk != null) {
                    Mode next = afterWalk;
                    afterWalk = null;
                    enter(next);
                } else {
                    rest();
                }
                break;
            case CLIMB:
                climb();
                break;
            case PUSH:
                if (++frame >= PUSH_PATH.length) {
                    logoShift = 0;
                    x = besideLogo();
                    enter(Mode.HOP);
                    break;
                }
                logoShift = PUSH_PATH[frame] * p;
                x = besideLogo() + Math.max(0, logoShift);
                redrawIn(PUSH_MS);
                break;
            case CARRY:
                carry();
                break;
            case BED:
                if (++frame > JUMP_FRAMES) {
                    enter(Mode.SLEEP);
                    break;
                }
                jump(besideLogo(), underLogo(), 0, onLogo(), (float) frame / JUMP_FRAMES);
                redrawIn(HOP_MS);
                break;
            case WAKE:
                if (++frame > JUMP_FRAMES) {
                    lift = 0;
                    rest();
                    break;
                }
                jump(underLogo(), besideLogo(), onLogo(), 0, (float) frame / JUMP_FRAMES);
                redrawIn(HOP_MS);
                break;
            case SHELTER:
                shelterTick();
                break;
            case DANCE:
                if (++frame > DANCE_FRAMES) {
                    gaze = 0;
                    rest();
                    break;
                }
                gaze = frame / 2 % 2 == 0 ? -1 : 1;
                redrawIn(HOP_MS);
                break;
            case GIANT:
                if (++frame > GIANT_FRAMES) rest(); else redrawIn(HOP_MS);
                break;
            case FIREWORKS:
                if (++frame > FIREWORK_FRAMES) {
                    lookUntil = 0;
                    rest();
                    break;
                }
                lookY = -1;
                lookX = 0;
                lookUntil = System.currentTimeMillis() + HOP_MS * 2;
                redrawIn(HOP_MS);
                break;
            case THINK:
                if (++frame > THINK_FRAMES) rest(); else redrawIn(THINK_MS);
                break;
            case LOOK:
                gaze = frame == 0 ? -1 : frame == 1 ? 1 : 0;
                if (++frame > 3) rest(); else redrawIn(LOOK_MS);
                break;
            case BLINK:
                if (++frame > 1) rest(); else redrawIn(BLINK_MS);
                break;
            case HOP:
                if (++frame >= HOP_HEIGHT.length) rest(); else redrawIn(HOP_MS);
                break;
            case SAY:
                frame++;
                if (frame > POP_FRAMES + 1) rest(); else redrawIn(frame < POP_FRAMES ? POP_MS : SAY_MS);
                break;
            case SLEEP:
                if (!shouldSleep()) { enter(lift > 0 ? Mode.WAKE : Mode.HOP); break; }
                frame++;
                redrawIn(SNORE_MS);
                break;
            case PARTY:
                if (++frame > PARTY_FRAMES) {
                    saying = hooray;
                    enter(Mode.SAY);
                } else {
                    redrawIn(HOP_MS);
                }
                break;
            default:
                decide();
        }
    }

    private void redrawIn(long ms) {
        invalidate();
        handler.postDelayed(tick, ms);
    }

    /** A short rest, without drawing anything, until the next action. */
    private void rest() {
        mode = Mode.IDLE;
        frame = 0;
        gaze = 0;
        invalidate();
        handler.postDelayed(tick, 500 + random.nextInt(1500));
    }

    private boolean shouldSleep() {
        return nightTime && System.currentTimeMillis() >= awakeUntil;
    }

    private void decide() {
        if (shouldSleep()) {
            if (logo != null && lift == 0) {
                // Bedtime is on top of the logo.
                targetX = besideLogo();
                afterWalk = Mode.BED;
                gaze = Integer.signum(targetX - x);
                enter(Mode.WALK);
            } else {
                enter(Mode.SLEEP);
            }
            return;
        }
        long now = System.currentTimeMillis();
        int roll = random.nextInt(100);
        if (greet || (roll < 15 && now - saidAt >= SAY_AGAIN_MS)) {
            saying = hello();
            greet = false;
            saidAt = now;
            enter(Mode.SAY);
        } else if (fiesta == Fiesta.NEW_YEAR && now - fireworksAt >= FIREWORKS_AGAIN_MS) {
            fireworksAt = now;
            enter(Mode.FIREWORKS);
        } else if (umbrella && logo != null && roll < 45 && now - shelteredAt >= SHELTER_AGAIN_MS) {
            shelter();
        } else if (roll < 25) {
            hop();
        } else if (roll < 50) {
            word = words.get(nextWord++ % words.size()) + "…";
            enter(Mode.THINK);
        } else if (roll < 57) {
            enter(Mode.LOOK);
        } else if (roll < 62) {
            enter(Mode.BLINK);
        } else if (logo != null && roll < 75 && now - visitedAt >= VISIT_AGAIN_MS) {
            visitedAt = now;
            afterWalk = TRICKS[random.nextInt(TRICKS.length)];
            targetX = afterWalk == Mode.CARRY ? underLogo() : besideLogo();
            gaze = Integer.signum(targetX - x);
            enter(Mode.WALK);
        } else {
            int span = ((logo != null ? logoLeft() - p : getWidth()) - COLS * p) / p;
            // Often runs all the way to the corner.
            targetX = (random.nextInt(3) == 0 ? span : random.nextInt(span + 1)) * p;
            gaze = Integer.signum(targetX - x);
            enter(Mode.WALK);
        }
    }

    private void shelter() {
        shelteredAt = System.currentTimeMillis();
        targetX = underLogo();
        afterWalk = Mode.SHELTER;
        gaze = Integer.signum(targetX - x);
        enter(Mode.WALK);
    }

    /** Rain: it holds the logo over its head like a roof for a while, looking out both ways. */
    private void shelterTick() {
        frame++;
        int hold = CARRY_LIFT + SHELTER_HOLD, overHead = (ROWS - LOGO_FLOOR) * p;
        if (frame <= CARRY_LIFT) {
            logoUp = snap((float) overHead * frame / CARRY_LIFT);
        } else if (frame <= hold) {
            gaze = (frame - CARRY_LIFT) / 10 % 2 == 0 ? -1 : 1;
        } else if (frame <= hold + CARRY_DROP) {
            logoUp = overHead * (hold + CARRY_DROP - frame) / CARRY_DROP;
        } else {
            targetX = besideLogo();
            gaze = -1;
            enter(Mode.WALK);
            return;
        }
        redrawIn(WALK_MS);
    }

    /** Standing on the logo, its feet this far above the floor. */
    private int onLogo() {
        return logoH + LOGO_FLOOR * p;
    }

    private int logoLeft() {
        return getWidth() - logoW - LOGO_MARGIN * p;
    }

    private int besideLogo() {
        return (logoLeft() - COLS * p - p) / p * p;
    }

    private int underLogo() {
        return (logoLeft() + (logoW - COLS * p) / 2) / p * p;
    }

    /** Jumps up onto the logo, stomps on it looking both ways, and jumps back down. */
    private void climb() {
        frame++;
        int stomped = JUMP_FRAMES + STOMPS * STOMP_FRAMES, onTop = logoH + LOGO_FLOOR * p;
        if (frame <= JUMP_FRAMES) {
            jump(besideLogo(), underLogo(), 0, onTop, (float) frame / JUMP_FRAMES);
        } else if (frame <= stomped) {
            int step = (frame - JUMP_FRAMES) % STOMP_FRAMES;
            // The logo gives a little as it lands.
            logoDip = step == HOP_HEIGHT.length ? p / 2 : 0;
            lift = onTop - logoDip + (step < HOP_HEIGHT.length ? HOP_HEIGHT[step] * p : 0);
            gaze = (frame - JUMP_FRAMES) / STOMP_FRAMES % 2 == 0 ? 1 : -1;
        } else if (frame <= stomped + JUMP_FRAMES) {
            logoDip = 0;
            jump(underLogo(), besideLogo(), onTop, 0, (float) (frame - stomped) / JUMP_FRAMES);
        } else {
            lift = 0;
            rest();
            return;
        }
        redrawIn(HOP_MS);
    }

    private void jump(int fromX, int toX, int fromLift, int toLift, float t) {
        x = snap(fromX + (toX - fromX) * t);
        lift = snap(fromLift + (toLift - fromLift) * t + JUMP_ROWS * p * (float) Math.sin(Math.PI * t));
    }

    private int snap(float value) {
        return Math.round(value / p) * p;
    }

    /** Lifts the logo over its head, takes it for a walk, brings it back and drops it. */
    private void carry() {
        frame++;
        int walked = CARRY_LIFT + 2 * CARRY_TRIP, overHead = (ROWS - LOGO_FLOOR) * p;
        if (frame <= CARRY_LIFT) {
            logoUp = snap((float) overHead * frame / CARRY_LIFT);
        } else if (frame <= CARRY_LIFT + CARRY_TRIP) {
            gaze = -1;
            x -= p;
        } else if (frame <= walked) {
            gaze = 1;
            x += p;
        } else if (frame <= walked + CARRY_DROP) {
            logoUp = overHead * (walked + CARRY_DROP - frame) / CARRY_DROP;
        } else {
            logoShift = 0;
            targetX = besideLogo();
            gaze = -1;
            enter(Mode.WALK);
            return;
        }
        logoShift = x - underLogo();
        redrawIn(WALK_MS);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (!placed && w > 0) {
            x = (w - COLS * p) / 2 / p * p;
            placed = true;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        if (logo != null) {
            int logoX = logoLeft() + logoShift, logoY = getHeight() - logoH - LOGO_FLOOR * p - logoUp + logoDip;
            logoRect.set(logoX, logoY, logoX + logoW, logoY + logoH);
            c.drawBitmap(logo, null, logoRect, logoPaint);
        }
        if (mode == Mode.SHELTER) drawRain(c);
        if (mode == Mode.FIREWORKS) drawFireworks(c);

        if (mode == Mode.GIANT) {
            c.save();
            c.scale(GIANT_SCALE, GIANT_SCALE, x + COLS * p / 2f, getHeight());
            drawPet(c);
            c.restore();
        } else {
            drawPet(c);
        }
    }

    private void drawPet(Canvas c) {
        boolean asleep = mode == Mode.SLEEP;
        int top = getHeight() - ROWS * p - lift;
        if (asleep) top += 2 * p;
        if (mode == Mode.HOP) top -= HOP_HEIGHT[Math.min(frame, HOP_HEIGHT.length - 1)] * p;
        if (mode == Mode.PARTY || mode == Mode.DANCE || mode == Mode.GIANT) top -= HOP_HEIGHT[frame % HOP_HEIGHT.length] * p;
        int left = x;
        if (mode == Mode.DANCE) left += (frame / 2 % 2 == 0 ? -p : p);
        boolean looking = System.currentTimeMillis() < lookUntil && !asleep;
        int eyeX = looking ? lookX : gaze, eyeY = looking ? lookY : 0;

        pixels.setColor(BODY);
        for (int row = 0; row < TORSO.length; row++) {
            String line = TORSO[row];
            int first = line.indexOf('X'), last = line.lastIndexOf('X');
            c.drawRect(left + first * p, top + row * p, left + (last + 1) * p, top + (row + 1) * p, pixels);
        }
        if (!asleep) {
            for (int i = 0; i < LEGS.length; i++) {
                // Walking lifts every other leg, swapping each step.
                boolean walking = mode == Mode.WALK || mode == Mode.PUSH || mode == Mode.CARRY;
                boolean lifted = walking && i % 2 == frame % 2;
                int legRows = lifted ? 1 : 2;
                c.drawRect(left + LEGS[i] * p, top + LEG_ROW * p, left + (LEGS[i] + 1) * p, top + (LEG_ROW + legRows) * p, pixels);
            }
        }
        if (scarf) drawScarf(c, left, top);

        pixels.setColor(EYE);
        if (sunglasses && !asleep) {
            int rowTop = top + (EYE_ROW + eyeY) * p, shift = left + eyeX * p;
            c.drawRect(shift + 3 * p, rowTop, shift + 10 * p, rowTop + p, pixels);
            c.drawRect(shift + 3 * p, rowTop + p, shift + 6 * p, rowTop + 2 * p, pixels);
            c.drawRect(shift + 7 * p, rowTop + p, shift + 10 * p, rowTop + 2 * p, pixels);
        } else {
            for (int col : EYES) {
                int eye = left + (col + eyeX) * p;
                if (asleep) {
                    c.drawRect(eye - p / 2, top + (EYE_ROW + 1) * p, eye + p + p / 2, top + (EYE_ROW + 1) * p + p / 2, pixels);
                } else if (mode == Mode.BLINK && frame == 1) {
                    c.drawRect(eye, top + (EYE_ROW + 1) * p, eye + p, top + (EYE_ROW + 1) * p + p / 2, pixels);
                } else {
                    c.drawRect(eye, top + (EYE_ROW + eyeY) * p, eye + p, top + (EYE_ROW + eyeY + 2) * p, pixels);
                }
            }
        }

        // What sits above the pet goes above the umbrella too.
        int above = top;
        if (umbrella && !asleep && mode != Mode.SHELTER) {
            drawUmbrella(c, left, top);
            above -= UMBRELLA.length * p;
        } else if (fiesta == Fiesta.XMAS) {
            drawHat(c, left, top);
            above -= 5 * p;
        }
        if (mode == Mode.PARTY) drawConfetti(c);

        if (mode == Mode.SAY) {
            drawBubble(c, above);
            return;
        }
        if (mode == Mode.THINK) {
            drawCaption(c, SPINNER.charAt(frame % SPINNER.length()) + " ", word, above);
        } else if (asleep) {
            drawCaption(c, SNORES[frame % SNORES.length], "", above);
        }
    }

    private void drawScarf(Canvas c, int left, int top) {
        pixels.setColor(SCARF_COLOR);
        c.drawRect(left + 2 * p, top + 5 * p, left + 11 * p, top + 6 * p, pixels);
        c.drawRect(left + 8 * p, top + 6 * p, left + 10 * p, top + LEG_ROW * p + p / 2, pixels);
    }

    private void drawUmbrella(Canvas c, int left, int top) {
        pixels.setColor(UMBRELLA_COLOR);
        int up = top - UMBRELLA.length * p;
        for (int row = 0; row < UMBRELLA.length; row++) {
            String line = UMBRELLA[row];
            int first = line.indexOf('X'), last = line.lastIndexOf('X');
            c.drawRect(left + first * p, up + row * p, left + (last + 1) * p, up + (row + 1) * p, pixels);
        }
    }

    /** Santa's hat on top of the head, with a white pompom. */
    private void drawHat(Canvas c, int left, int top) {
        pixels.setColor(HAT_COLOR);
        for (int row = 1; row <= 4; row++) {
            c.drawRect(left + (row + 2) * p, top - row * p, left + (11 - row) * p, top - (row - 1) * p, pixels);
        }
        pixels.setColor(FUR_COLOR);
        c.drawRect(left + 2 * p, top - p, left + 11 * p, top, pixels);
        c.drawRect(left + 6 * p, top - 5 * p, left + 7 * p, top - 4 * p, pixels);
    }

    /** Drops falling around it and bouncing off the logo it holds up. */
    private void drawRain(Canvas c) {
        pixels.setColor(DROP_COLOR);
        int roofY = getHeight() - logoH - LOGO_FLOOR * p - logoUp;
        int from = logoLeft() - 12 * p;
        for (int i = 0; i < 16; i++) {
            int dropX = from + i * 2 * p;
            boolean underRoof = dropX > logoLeft() - p && dropX < logoLeft() + logoW + p;
            int floor = underRoof ? roofY : getHeight();
            int span = floor + 2 * p;
            int dropY = ((i * 37 + frame * 3 * p) % span);
            c.drawRect(dropX, dropY, dropX + p / 2, Math.min(floor, dropY + 2 * p), pixels);
        }
    }

    /** Three rockets, one after the other; everything comes from the frame, so it needs no state. */
    private void drawFireworks(Canvas c) {
        int rise = 8, burst = 16, gap = (FIREWORK_FRAMES - rise - burst) / 2;
        for (int r = 0; r < 3; r++) {
            int t = frame - r * gap;
            if (t < 0 || t > rise + burst) continue;
            int rocketX = getWidth() * (r + 1) / 4, burstY = -8 * p - (r % 2) * 8 * p;
            if (t <= rise) {
                pixels.setColor(FUR_COLOR);
                int y = getHeight() + (burstY - getHeight()) * t / rise;
                c.drawRect(rocketX, y, rocketX + p / 2, y + 2 * p, pixels);
                continue;
            }
            float radius = (t - rise) * p * 1.6f;
            pixels.setColor(CONFETTI[r % CONFETTI.length]);
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2 * i / 16;
                int px = rocketX + (int) (Math.cos(angle) * radius);
                int py = burstY + (int) (Math.sin(angle) * radius) + (t - rise) * p / 4;
                c.drawRect(px, py, px + p, py + p, pixels);
            }
        }
    }

    /** Falls from above the pet; positions come from the piece's index, so it needs no state. */
    private void drawConfetti(Canvas c) {
        int span = COLS * p * 2;
        for (int i = 0; i < CONFETTI.length * 3; i++) {
            pixels.setColor(CONFETTI[i % CONFETTI.length]);
            int px = x - COLS * p / 2 + (i * 53 + frame * (i % 3 - 1) * 2) % span;
            int py = (i * 29 + frame * p / 2 * (1 + i % 3)) % getHeight();
            c.drawRect(px, py, px + p, py + p, pixels);
        }
    }

    /** Text above the pet, kept inside the view: a coloured lead (spinner, snore) and a light word. */
    private void drawCaption(Canvas c, String lead, String rest, int petTop) {
        float leadW = text.measureText(lead), width = leadW + text.measureText(rest);
        float textX = Math.max(0, Math.min(getWidth() - width, x + (COLS * p - width) / 2));
        text.setColor(BODY);
        c.drawText(lead, textX, petTop - p, text);
        text.setColor(WORD);
        c.drawText(rest, textX + leadW, petTop - p, text);
    }

    /** Pixel speech bubble above the pet, kept inside the view; it pops open over a few frames. */
    private void drawBubble(Canvas c, int petTop) {
        int s = p / 2;
        int textW = saying.length() * (GLYPH_W + 1) * s - s, textH = GLYPH_H * s;
        int w = textW + 8 * s, h = textH + 6 * s;
        int head = x + COLS * p / 2;
        int left = Math.max(0, Math.min(getWidth() - w, head - w / 2));
        int bottom = petTop - 3 * s, up = bottom - h, right = left + w;

        c.save();
        float open = Math.min(1f, (float) frame / POP_FRAMES);
        c.scale(open, open, head, bottom);

        // Outline with notched corners, then the fill.
        pixels.setColor(EYE);
        c.drawRect(left + s, up, right - s, bottom, pixels);
        c.drawRect(left, up + s, right, bottom - s, pixels);
        pixels.setColor(BUBBLE);
        c.drawRect(left + s, up + s, right - s, bottom - s, pixels);

        // Tail pointing at the pet.
        int tail = Math.max(left + 2 * s, Math.min(right - 4 * s, head - s));
        pixels.setColor(EYE);
        c.drawRect(tail - s, bottom, tail + 3 * s, bottom + s, pixels);
        c.drawRect(tail, bottom + s, tail + 2 * s, bottom + 2 * s, pixels);
        pixels.setColor(BUBBLE);
        c.drawRect(tail, bottom - s, tail + 2 * s, bottom + s, pixels);

        pixels.setColor(EYE);
        int penX = left + 4 * s, penY = up + 3 * s;
        for (int i = 0; i < saying.length(); i++) {
            int glyph = LETTERS.indexOf(saying.charAt(i));
            if (glyph >= 0) {
                for (int row = 0; row < GLYPH_H; row++) {
                    for (int col = 0; col < GLYPH_W; col++) {
                        if (GLYPHS[glyph][row].charAt(col) != 'X') continue;
                        int px = penX + col * s, py = penY + row * s;
                        c.drawRect(px, py, px + s, py + s, pixels);
                    }
                }
            }
            penX += (GLYPH_W + 1) * s;
        }
        c.restore();
    }
}
