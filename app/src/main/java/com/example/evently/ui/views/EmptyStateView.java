package com.example.evently.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.example.evently.R;

/**
 * Reusable mascot-based state component (empty / error / success / loading hint).
 * <p>
 * Layout: 140dp centred mascot, bold titleMedium title, bodyMedium body, optional CTA.
 * Animacija: lagani fade-in (200ms). Maksimalno jedna maskota po ekranu.
 */
public class EmptyStateView extends FrameLayout {

    private static final int MASCOT_SIZE_DP = 140;
    private static final long FADE_IN_MS = 200L;

    private ImageView ivMascot;
    private TextView tvTitle;
    private TextView tvBody;
    private AppCompatButton btnAction;

    public EmptyStateView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_empty_state, this, true);
        ivMascot = findViewById(R.id.ivEmptyStateMascot);
        tvTitle = findViewById(R.id.tvEmptyStateTitle);
        tvBody = findViewById(R.id.tvEmptyStateBody);
        btnAction = findViewById(R.id.btnEmptyStateAction);

        setVisibility(GONE);
    }

    /** Prikaži empty/error/success stanje bez CTA. */
    public void show(int mascotRes, @NonNull String title, @Nullable String body) {
        show(mascotRes, title, body, 0, null);
    }

    /** Prikaži stanje sa CTA dugmetom (actionTextRes == 0 skriva dugme). */
    public void show(int mascotRes, @NonNull String title, @Nullable String body,
                     int actionTextRes, @Nullable OnClickListener actionListener) {
        ivMascot.setImageResource(mascotRes);
        ivMascot.setVisibility(mascotRes == 0 ? GONE : VISIBLE);
        tvTitle.setText(title);
        if (body != null && !body.isEmpty()) {
            tvBody.setText(body);
            tvBody.setVisibility(VISIBLE);
        } else {
            tvBody.setVisibility(GONE);
        }

        if (actionTextRes != 0) {
            btnAction.setText(actionTextRes);
            btnAction.setVisibility(VISIBLE);
            btnAction.setOnClickListener(actionListener);
        } else {
            btnAction.setVisibility(GONE);
        }

        setVisibility(VISIBLE);
        startFadeIn();
    }

    public void hide() {
        clearAnimation();
        setVisibility(GONE);
    }

    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    private void startFadeIn() {
        clearAnimation();
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(FADE_IN_MS);
        fadeIn.setFillAfter(false);
        startAnimation(fadeIn);
    }
}
