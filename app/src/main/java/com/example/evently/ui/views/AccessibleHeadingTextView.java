package com.example.evently.ui.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import com.google.android.material.textview.MaterialTextView;

/** A semantic heading whose accessibility role also works on API 24–27. */
public class AccessibleHeadingTextView extends MaterialTextView {
    public AccessibleHeadingTextView(@NonNull Context context) {
        this(context, null);
    }

    public AccessibleHeadingTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AccessibleHeadingTextView(@NonNull Context context, @Nullable AttributeSet attrs,
                                     int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ViewCompat.setAccessibilityHeading(this, true);
    }
}
