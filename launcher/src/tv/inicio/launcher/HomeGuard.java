package tv.inicio.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Google TV re-enables its own launcher on every boot and it outranks any other
 * home app, so this service watches only that launcher's home activity and
 * replaces it with ours as soon as it comes to the front.
 */
public class HomeGuard extends AccessibilityService {
    private static final String GOOGLE_HOME = "com.google.android.apps.tv.launcherx.home.HomeActivity";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence cls = event.getClassName();
        if (cls == null || !GOOGLE_HOME.contentEquals(cls)) return;
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setClass(this, InicioActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }

    @Override
    public void onInterrupt() {
    }
}
