// lang: Java | file: ESPOverlayService.java | target: Android 12-14
// Anti-ban: notification ẩn, label giả, không foreground visible
package com.noob.ffesp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.WindowManager;

public class ESPOverlayService extends Service {

    public static boolean isRunning = false;
    private static final String CH_ID = "sys_overlay_ch";

    private WindowManager    wm;
    private ESPView          espView;
    private ESPControlButton controlBtn;
    private String           targetPkg;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        createHiddenChannel();
        startForeground(88, buildStealthNotif());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) targetPkg = intent.getStringExtra("target_pkg");

        // ESP canvas — full screen, không chặn touch
        espView = new ESPView(this);
        WindowManager.LayoutParams espParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE  |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE  |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );
        wm.addView(espView, espParams);

        // Nút toggle kéo thả
        controlBtn = new ESPControlButton(this, espView, wm);
        controlBtn.attach();

        // Start render + data (MockDataFeed cho non-root, MemReader cho root)
        espView.startRendering();
        MockDataFeed.start(espView);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        MockDataFeed.stop();
        if (espView != null) {
            espView.stopRendering();
            try { wm.removeView(espView); } catch (Exception ignored) {}
        }
        if (controlBtn != null) controlBtn.detach();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    // ── Stealth notification — anti-ban ──────────────────────────────────
    // Hiện như system service thông thường, không mention ESP/cheat
    private void createHiddenChannel() {
        NotificationChannel ch = new NotificationChannel(
            CH_ID, "System Overlay Service", NotificationManager.IMPORTANCE_MIN);
        ch.setSound(null, null);
        ch.setShowBadge(false);
        ch.enableLights(false);
        ch.enableVibration(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    private Notification buildStealthNotif() {
        return new Notification.Builder(this, CH_ID)
            .setContentTitle("Display Service")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .build();
    }
}
