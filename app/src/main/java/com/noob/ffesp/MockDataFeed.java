// lang: Java | file: MockDataFeed.java | target: Android 12-14
// Data feed — non-root mode: giả lập realtime
// Root mode: thay bằng MemReader.start(espView, targetPkg)
package com.noob.ffesp;

import android.os.Handler;
import android.os.HandlerThread;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MockDataFeed {

    private static HandlerThread thread;
    private static Handler       handler;
    private static boolean       running = false;

    // Smooth enemy state — không nhảy random mỗi frame
    private static final int      MAX_ENEMIES = 4;
    private static EnemyState[]   states;
    private static final Random   rng = new Random();

    private static class EnemyState {
        float x, y, w, h, hp, vx, vy;
        int distance;
        boolean visible;

        EnemyState(float sw, float sh) {
            w = 50 + rng.nextFloat() * 40f;
            h = w * 2.6f;
            x = 60 + rng.nextFloat() * (sw - w - 120);
            y = 80 + rng.nextFloat() * (sh - h - 160);
            hp = 30 + rng.nextFloat() * 70f;
            distance = 20 + rng.nextInt(200);
            visible = true;
            vx = (rng.nextFloat() - 0.5f) * 3f;
            vy = (rng.nextFloat() - 0.5f) * 2f;
        }

        void tick(float sw, float sh) {
            x += vx; y += vy;
            // bounce off edges
            if (x < 40 || x + w > sw - 40) vx = -vx;
            if (y < 60 || y + h > sh - 120) vy = -vy;
            x = Math.max(40, Math.min(sw - w - 40, x));
            y = Math.max(60, Math.min(sh - h - 120, y));
            // HP slowly drops then resets
            hp -= 0.15f;
            if (hp < 5) hp = 100f;
            // distance fluctuates
            distance += (int)((rng.nextFloat() - 0.5f) * 4);
            distance = Math.max(5, Math.min(300, distance));
        }
    }

    public static void start(ESPView view) {
        thread  = new HandlerThread("DataFeed");
        thread.start();
        handler = new Handler(thread.getLooper());
        running = true;
        handler.postDelayed(() -> initStates(view), 200);
    }

    private static void initStates(ESPView view) {
        // Tunggu sampai view punya ukuran
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            handler.postDelayed(() -> initStates(view), 100);
            return;
        }
        float sw = view.getWidth(), sh = view.getHeight();
        states = new EnemyState[MAX_ENEMIES];
        for (int i = 0; i < MAX_ENEMIES; i++) states[i] = new EnemyState(sw, sh);
        scheduleTick(view);
    }

    private static void scheduleTick(ESPView view) {
        if (!running) return;
        handler.postDelayed(() -> {
            if (states == null) { scheduleTick(view); return; }
            float sw = view.getWidth(), sh = view.getHeight();
            List<ESPView.Enemy> list = new ArrayList<>();
            for (EnemyState s : states) {
                s.tick(sw, sh);
                ESPView.Enemy e = new ESPView.Enemy();
                e.x = s.x; e.y = s.y; e.w = s.w; e.h = s.h;
                e.footX = s.x + s.w / 2f;
                e.footY = s.y + s.h;
                e.hp = s.hp;
                e.distance = s.distance;
                e.visible = s.visible;
                list.add(e);
            }
            view.setEnemies(list);
            scheduleTick(view);
        }, 16); // 60Hz
    }

    public static void stop() {
        running = false;
        states  = null;
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (thread  != null) thread.quitSafely();
    }
}
