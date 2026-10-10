// lang: Java | file: ESPView.java | target: Android 12-14
// ESP overlay canvas — box, corner, snapline, hp, distance
package com.noob.ffesp;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class ESPView extends View {

    public static class Enemy {
        public float x, y, w, h;
        public float footX, footY;
        public float hp;
        public int   distance;
        public boolean visible;
    }

    // Paints
    private final Paint pCorner  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pFill    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHpBg    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHpFg    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDotBg   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pDot     = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Enemy> enemies = new ArrayList<>();
    private final Object lock = new Object();
    private boolean espEnabled = true;

    private final Handler renderHandler = new Handler(Looper.getMainLooper());
    private boolean rendering = false;
    private static final long FRAME_MS = 16;

    // Tick for breathing glow effect
    private float glowPhase = 0f;

    public ESPView(Context ctx) {
        super(ctx);
        setWillNotDraw(false);
        initPaints();
    }

    private void initPaints() {
        pCorner.setStyle(Paint.Style.STROKE);
        pCorner.setStrokeWidth(3f);
        pCorner.setStrokeCap(Paint.Cap.ROUND);

        pFill.setStyle(Paint.Style.FILL);

        pLine.setStyle(Paint.Style.STROKE);
        pLine.setStrokeWidth(1f);
        pLine.setStrokeCap(Paint.Cap.ROUND);
        pLine.setPathEffect(new DashPathEffect(new float[]{8f, 6f}, 0));

        pHpBg.setColor(0xBB222222);
        pHpBg.setStyle(Paint.Style.FILL);

        pHpFg.setStyle(Paint.Style.FILL);

        pText.setColor(Color.WHITE);
        pText.setTextSize(20f);
        pText.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        pText.setShadowLayer(4f, 1f, 1f, Color.BLACK);

        pDotBg.setStyle(Paint.Style.FILL);
        pDot.setStyle(Paint.Style.FILL);
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setEnemies(List<Enemy> list) {
        synchronized (lock) { enemies.clear(); enemies.addAll(list); }
    }

    public void setESPEnabled(boolean v) { espEnabled = v; invalidate(); }
    public boolean isESPEnabled() { return espEnabled; }

    public void startRendering() {
        rendering = true;
        scheduleFrame();
    }

    public void stopRendering() {
        rendering = false;
        renderHandler.removeCallbacksAndMessages(null);
    }

    private void scheduleFrame() {
        if (!rendering) return;
        renderHandler.postDelayed(() -> { glowPhase += 0.06f; invalidate(); scheduleFrame(); }, FRAME_MS);
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (!espEnabled) return;

        float sw = getWidth(), sh = getHeight();
        float originX = sw / 2f, originY = sh;

        // Breathing glow alpha: 0.5 → 1.0
        float glow = 0.5f + 0.5f * (float)Math.sin(glowPhase);

        List<Enemy> snap;
        synchronized (lock) { snap = new ArrayList<>(enemies); }

        for (Enemy e : snap) {
            if (!e.visible) continue;
            float cLen = Math.min(e.w * 0.22f, 18f);

            // ── Snapline — dashed, màu theo khoảng cách ───────────────────
            int lineAlpha = (int)(120 + 80 * glow);
            if (e.distance < 50)       pLine.setColor(Color.argb(lineAlpha, 255, 80, 80));
            else if (e.distance < 120) pLine.setColor(Color.argb(lineAlpha, 255, 200, 0));
            else                        pLine.setColor(Color.argb(lineAlpha, 100, 200, 255));
            canvas.drawLine(originX, originY, e.footX, e.footY, pLine);

            // ── Box fill — rất mờ ──────────────────────────────────────────
            pFill.setColor(Color.argb(18, 255, 50, 50));
            canvas.drawRoundRect(new RectF(e.x, e.y, e.x+e.w, e.y+e.h), 4f, 4f, pFill);

            // ── Corner brackets — màu glow theo khoảng cách ───────────────
            int cAlpha = (int)(180 + 75 * glow);
            if (e.distance < 50)       pCorner.setColor(Color.argb(cAlpha, 255, 60, 60));
            else if (e.distance < 120) pCorner.setColor(Color.argb(cAlpha, 255, 210, 0));
            else                        pCorner.setColor(Color.argb(cAlpha, 80, 180, 255));

            // Top-left
            canvas.drawLine(e.x, e.y, e.x+cLen, e.y, pCorner);
            canvas.drawLine(e.x, e.y, e.x, e.y+cLen, pCorner);
            // Top-right
            canvas.drawLine(e.x+e.w, e.y, e.x+e.w-cLen, e.y, pCorner);
            canvas.drawLine(e.x+e.w, e.y, e.x+e.w, e.y+cLen, pCorner);
            // Bot-left
            canvas.drawLine(e.x, e.y+e.h, e.x+cLen, e.y+e.h, pCorner);
            canvas.drawLine(e.x, e.y+e.h, e.x, e.y+e.h-cLen, pCorner);
            // Bot-right
            canvas.drawLine(e.x+e.w, e.y+e.h, e.x+e.w-cLen, e.y+e.h, pCorner);
            canvas.drawLine(e.x+e.w, e.y+e.h, e.x+e.w, e.y+e.h-cLen, pCorner);

            // ── HP bar — trái box ─────────────────────────────────────────
            float barX  = e.x - 7f;
            float barW  = 5f;
            float fillH = e.h * (e.hp / 100f);
            canvas.drawRoundRect(new RectF(barX-barW, e.y, barX, e.y+e.h), 2f, 2f, pHpBg);
            int hpColor;
            if (e.hp > 60)      hpColor = 0xFF00E676;
            else if (e.hp > 30) hpColor = 0xFFFFD600;
            else                hpColor = 0xFFFF1744;
            pHpFg.setColor(hpColor);
            canvas.drawRoundRect(new RectF(barX-barW, e.y+e.h-fillH, barX, e.y+e.h), 2f, 2f, pHpFg);

            // HP number above bar
            pText.setTextSize(18f);
            pText.setColor(hpColor);
            canvas.drawText((int)e.hp + "", barX - barW - 2f, e.y - 4f, pText);

            // ── Distance tag — above box ──────────────────────────────────
            if (e.distance > 0) {
                String distStr = e.distance + "m";
                float tw = pText.measureText(distStr);
                float tx = e.x + e.w/2f - tw/2f;
                float ty = e.y - 6f;

                // Background pill
                RectF pill = new RectF(tx - 6f, ty - 20f, tx + tw + 6f, ty + 2f);
                pDotBg.setColor(0xBB000000);
                canvas.drawRoundRect(pill, 6f, 6f, pDotBg);

                pText.setTextSize(18f);
                pText.setColor(Color.WHITE);
                canvas.drawText(distStr, tx, ty, pText);
            }

            // ── Head dot ──────────────────────────────────────────────────
            pDot.setColor(Color.argb((int)(200 * glow), 255, 255, 255));
            canvas.drawCircle(e.x + e.w/2f, e.y + e.h * 0.05f, 4f, pDot);
        }
    }
}
