package com.example.evently.ui.views;

import android.app.Activity;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/** One application boundary for system bars, cutouts and the keyboard. */
public final class ActivityInsets {
    private ActivityInsets() { }
    public static void apply(Activity activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        boolean dark = (activity.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(!dark);
        WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightNavigationBars(!dark);
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime();
            Insets insets = windowInsets.getInsets(types);
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            // Children with fitsSystemWindows must not apply these same edges a second time.
            return new WindowInsetsCompat.Builder(windowInsets).setInsets(types, Insets.NONE).build();
        });
        ViewCompat.requestApplyInsets(content);
    }
}
