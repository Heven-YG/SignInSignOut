package com.example.chatapp.utilities;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helpers for edge-to-edge layouts, so content stays clear of the status bar, navigation
 * bar, display cutouts, and the on-screen keyboard.
 */
public final class InsetsUtils {

    /** Prevents instantiation of this utility class. */
    private InsetsUtils() {
    }

    /**
     * Pads {@code root} by the system bar and cutout insets, and pads the bottom by the larger
     * of the navigation bar and keyboard heights so focused fields remain reachable by
     * scrolling while the keyboard is open. The view's original padding is preserved.
     *
     * @param root the layout root that should avoid system UI
     */
    public static void applySystemBarAndImePadding(@NonNull View root) {
        final int startLeft = root.getPaddingLeft();
        final int startTop = root.getPaddingTop();
        final int startRight = root.getPaddingRight();
        final int startBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(
                    startLeft + bars.left,
                    startTop + bars.top,
                    startRight + bars.right,
                    startBottom + Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
