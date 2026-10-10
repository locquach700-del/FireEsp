// lang: Java | file: ESPControlButton.java | target: Android 12-14
// Nút toggle nổi — kéo thả mượt với animation spring
package com.noob.ffesp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ESPControlButton {

    private final Context ctx;
    private final ESPView espView;
    private final WindowManager wm;

    private LinearLayout btnView;
    private TextView labelTop, labelBot;
    private WindowManager.LayoutParams params;

    private int   initX, initY;
    private float initTouchX, initTouchY;
    private long  downTime;
    private boolean dragged;
    private boolean espOn = true;

    public ESPControlButton(Context ctx, ESPView espView, WindowManager wm) {
        this.ctx     = ctx;
        this.espView = espView;
        this.wm      = wm;
    }

    public void attach() {
        btnView = new LinearLayout(ctx);
        btnView.setOrientation(LinearLayout.VERTICAL);
        btnView.setGravity(Gravity.CENTER);
        btnView.setPadding(dp(14), dp(10), dp(14), dp(10));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE6111122);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0xFF6C63FF);
        btnView.setBackgroundDrawable(bg);

        labelTop = new TextView(ctx);
        labelTop.setText("ESP");
        labelTop.setTextColor(0xFF6C63FF);
        labelTop.setTextSize(10f);
        labelTop.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelTop.setGravity(Gravity.CENTER);
        labelTop.setLetterSpacing(0.15f);

        labelBot = new TextView(ctx);
        labelBot.setText("ON");
        labelBot.setTextColor(0xFF00FF88);
        labelBot.setTextSize(13f);
        labelBot.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelBot.setGravity(Gravity.CENTER);

        btnView.addView(labelTop);
        btnView.addView(labelBot);

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = dp(16);
        params.y = dp(200);

        btnView.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initX      = params.x;
                    initY      = params.y;
                    initTouchX = e.getRawX();
                    initTouchY = e.getRawY();
                    downTime   = System.currentTimeMillis();
                    dragged    = false;
                    // press scale down
                    btnView.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - initTouchX);
                    int dy = (int)(e.getRawY() - initTouchY);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) dragged = true;
                    if (dragged) {
                        params.x = initX + dx;
                        params.y = initY + dy;
                        wm.updateViewLayout(btnView, params);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    // spring back scale
                    btnView.animate().scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start();
                    if (!dragged && System.currentTimeMillis() - downTime < 350) {
                        toggle();
                    }
                    return true;
            }
            return false;
        });

        wm.addView(btnView, params);

        // entrance animation
        btnView.setAlpha(0f);
        btnView.setScaleX(0.6f);
        btnView.setScaleY(0.6f);
        btnView.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400)
            .setInterpolator(new OvershootInterpolator(1.5f))
            .start();
    }

    private void toggle() {
        espOn = !espOn;
        espView.setESPEnabled(espOn);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        if (espOn) {
            bg.setColor(0xE6111122);
            bg.setStroke(dp(1), 0xFF6C63FF);
            labelBot.setText("ON");
            labelBot.setTextColor(0xFF00FF88);
            labelTop.setTextColor(0xFF6C63FF);
        } else {
            bg.setColor(0xE6220011);
            bg.setStroke(dp(1), 0xFFE63946);
            labelBot.setText("OFF");
            labelBot.setTextColor(0xFFE63946);
            labelTop.setTextColor(0xFFE63946);
        }
        btnView.setBackgroundDrawable(bg);

        // pulse anim
        btnView.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100)
            .withEndAction(() ->
                btnView.animate().scaleX(1f).scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(new OvershootInterpolator(2f))
                    .start()
            ).start();
    }

    public void detach() {
        if (btnView != null) {
            btnView.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f)
                .setDuration(200)
                .withEndAction(() -> {
                    try { wm.removeView(btnView); } catch (Exception ignored) {}
                }).start();
            btnView = null;
        }
    }

    private int dp(int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
