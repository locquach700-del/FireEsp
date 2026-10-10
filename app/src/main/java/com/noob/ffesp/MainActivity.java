// lang: Java | file: MainActivity.java | target: Android 12-14
package com.noob.ffesp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_OVERLAY = 1001;
    private static final String PREF = "esp_pref";
    private static final String KEY_PKG = "target_pkg";

    // FF known packages
    private static final String[] FF_PACKAGES = {
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.garena.game.fftw",
        "com.garena.game.ffvn",
        "com.dts.freefireth.vn",
    };

    private TextView tvStatus, tvAppName;
    private View btnStart, btnStop, btnPickApp;
    private View dotStatus;
    private String targetPkg = null;
    private SharedPreferences prefs;
    private Handler pulseHandler = new Handler();
    private boolean espRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);
        targetPkg = prefs.getString(KEY_PKG, null);
        buildUI();
        autoDetectFF();
    }

    private void buildUI() {
        // Root scroll
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A14);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(32));
        scroll.addView(root);

        // ── Header ────────────────────────────────────────────────────────
        TextView logo = new TextView(this);
        logo.setText("ESP");
        logo.setTextSize(64f);
        logo.setTextColor(0xFFE63946);
        logo.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setShadowLayer(20f, 0, 0, 0xAAE63946);
        root.addView(logo, matchWrap());

        TextView sub = new TextView(this);
        sub.setText("FREE FIRE OVERLAY  ·  OB55");
        sub.setTextSize(11f);
        sub.setTextColor(0xFF555577);
        sub.setGravity(Gravity.CENTER);
        sub.setLetterSpacing(0.15f);
        root.addView(sub, matchWrap());

        root.addView(spacer(dp(32)));

        // ── Status card ───────────────────────────────────────────────────
        LinearLayout statusCard = card();
        statusCard.setOrientation(LinearLayout.HORIZONTAL);
        statusCard.setGravity(Gravity.CENTER_VERTICAL);

        dotStatus = new View(this);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(10), dp(10));
        dotLp.setMarginEnd(dp(12));
        dotStatus.setBackgroundDrawable(circle(0xFFFF4444));
        statusCard.addView(dotStatus, dotLp);

        LinearLayout statusCol = new LinearLayout(this);
        statusCol.setOrientation(LinearLayout.VERTICAL);

        tvStatus = new TextView(this);
        tvStatus.setText("ESP TẮT");
        tvStatus.setTextColor(0xFFFF4444);
        tvStatus.setTextSize(15f);
        tvStatus.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        statusCol.addView(tvStatus);

        tvAppName = new TextView(this);
        tvAppName.setText(targetPkg != null ? "App: " + getAppName(targetPkg) : "Chưa chọn ứng dụng");
        tvAppName.setTextColor(0xFF555577);
        tvAppName.setTextSize(12f);
        statusCol.addView(tvAppName);

        statusCard.addView(statusCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(statusCard, matchWrap());
        root.addView(spacer(dp(12)));

        // ── App picker card ───────────────────────────────────────────────
        LinearLayout appCard = card();
        appCard.setOrientation(LinearLayout.VERTICAL);

        TextView appLabel = new TextView(this);
        appLabel.setText("ỨNG DỤNG MỤC TIÊU");
        appLabel.setTextColor(0xFF555577);
        appLabel.setTextSize(10f);
        appLabel.setLetterSpacing(0.15f);
        appCard.addView(appLabel);
        appCard.addView(spacer(dp(12)));

        btnPickApp = accentBtn("  CHỌN ỨNG DỤNG", 0xFF1E1E3A, 0xFF6C63FF);
        btnPickApp.setOnClickListener(v -> showAppPicker());
        appCard.addView(btnPickApp, matchWrap());

        root.addView(appCard, matchWrap());
        root.addView(spacer(dp(12)));

        // ── Feature toggles card ──────────────────────────────────────────
        LinearLayout featCard = card();
        featCard.setOrientation(LinearLayout.VERTICAL);

        TextView featLabel = new TextView(this);
        featLabel.setText("TÍNH NĂNG");
        featLabel.setTextColor(0xFF555577);
        featLabel.setTextSize(10f);
        featLabel.setLetterSpacing(0.15f);
        featCard.addView(featLabel);
        featCard.addView(spacer(dp(12)));

        String[][] feats = {
            {"ESP Box", "Hộp viền quanh địch"},
            {"Snapline", "Đường kẻ từ màn hình đến địch"},
            {"HP Bar", "Thanh máu màu theo %"},
            {"Distance", "Khoảng cách đến địch"},
        };
        for (String[] f : feats) {
            featCard.addView(featureRow(f[0], f[1]));
        }

        root.addView(featCard, matchWrap());
        root.addView(spacer(dp(24)));

        // ── START button ──────────────────────────────────────────────────
        btnStart = accentBtn("▶   BẬT ESP", 0xFFE63946, 0xFFB5222E);
        btnStart.setOnClickListener(v -> onStartPressed());
        root.addView(btnStart, matchH(dp(56)));
        root.addView(spacer(dp(12)));

        // ── STOP button ───────────────────────────────────────────────────
        btnStop = accentBtn("■   TẮT ESP", 0xFF1A1A2E, 0xFF2A2A4E);
        btnStop.setEnabled(false);
        btnStop.setAlpha(0.4f);
        btnStop.setOnClickListener(v -> stopESP());
        root.addView(btnStop, matchH(dp(48)));

        root.addView(spacer(dp(32)));

        // ── Footer ────────────────────────────────────────────────────────
        TextView footer = new TextView(this);
        footer.setText("Overlay chạy trên màn hình game · Tap nút nổi để toggle");
        footer.setTextColor(0xFF333355);
        footer.setTextSize(11f);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, matchWrap());

        setContentView(scroll);
    }

    // ── App picker ────────────────────────────────────────────────────────

    private void showAppPicker() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(0);

        // Ưu tiên: FF packages trước, sau đó toàn bộ game
        List<String[]> items = new ArrayList<>();

        // Check FF packages trước
        for (String pkg : FF_PACKAGES) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                items.add(0, new String[]{ pm.getApplicationLabel(ai).toString(), pkg });
            } catch (Exception ignored) {}
        }

        // Thêm tất cả app còn lại
        for (ApplicationInfo ai : installed) {
            boolean isFF = false;
            for (String fp : FF_PACKAGES) if (fp.equals(ai.packageName)) { isFF = true; break; }
            if (!isFF) {
                String label = pm.getApplicationLabel(ai).toString();
                items.add(new String[]{ label, ai.packageName });
            }
        }

        String[] labels = new String[items.size()];
        for (int i = 0; i < items.size(); i++) labels[i] = items.get(i)[0];

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Chọn ứng dụng mục tiêu");
        b.setItems(labels, (d, which) -> {
            targetPkg = items.get(which)[1];
            prefs.edit().putString(KEY_PKG, targetPkg).apply();
            tvAppName.setText("App: " + items.get(which)[0]);
        });
        b.show();
    }

    private String getAppName(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) { return pkg; }
    }

    // ── ESP control ───────────────────────────────────────────────────────

    private void onStartPressed() {
        if (targetPkg == null) {
            Toast.makeText(this, "Chọn ứng dụng mục tiêu trước", Toast.LENGTH_SHORT).show();
            showAppPicker();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
            Toast.makeText(this, "Bật 'Hiển thị trên ứng dụng khác' rồi quay lại", Toast.LENGTH_LONG).show();
            return;
        }
        launchESP();
    }

    private void launchESP() {
        // Mở app mục tiêu
        if (targetPkg != null) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(targetPkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
            }
        }
        // Start ESP service
        Intent svc = new Intent(this, ESPOverlayService.class);
        svc.putExtra("target_pkg", targetPkg);
        startForegroundService(svc);
        setESPState(true);
    }

    private void stopESP() {
        stopService(new Intent(this, ESPOverlayService.class));
        setESPState(false);
    }

    private void setESPState(boolean on) {
        espRunning = on;
        tvStatus.setText(on ? "ESP ĐANG CHẠY" : "ESP TẮT");
        tvStatus.setTextColor(on ? 0xFF00FF88 : 0xFFFF4444);
        dotStatus.setBackgroundDrawable(circle(on ? 0xFF00FF88 : 0xFFFF4444));
        btnStart.setEnabled(!on);
        btnStart.setAlpha(on ? 0.4f : 1f);
        btnStop.setEnabled(on);
        btnStop.setAlpha(on ? 1f : 0.4f);
        if (on) startPulse(); else stopPulse();
    }

    // ── Pulse animation on dot ────────────────────────────────────────────

    private Runnable pulseRunnable;
    private boolean pulseUp = false;

    private void startPulse() {
        pulseRunnable = new Runnable() {
            @Override public void run() {
                if (!espRunning) return;
                dotStatus.animate().scaleX(pulseUp ? 1.6f : 1f)
                    .scaleY(pulseUp ? 1.6f : 1f)
                    .alpha(pulseUp ? 0.5f : 1f)
                    .setDuration(600).start();
                pulseUp = !pulseUp;
                pulseHandler.postDelayed(this, 600);
            }
        };
        pulseHandler.post(pulseRunnable);
    }

    private void stopPulse() {
        if (pulseRunnable != null) pulseHandler.removeCallbacks(pulseRunnable);
        dotStatus.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(200).start();
    }

    // ── Auto detect FF ────────────────────────────────────────────────────

    private void autoDetectFF() {
        if (targetPkg != null) return;
        for (String pkg : FF_PACKAGES) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                targetPkg = pkg;
                prefs.edit().putString(KEY_PKG, pkg).apply();
                tvAppName.setText("App: " + getAppName(pkg));
                return;
            } catch (Exception ignored) {}
        }
    }

    // ── UI helpers ─────────────────────────────────────────────────────────

    private LinearLayout card() {
        LinearLayout ll = new LinearLayout(this);
        ll.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF12121F);
        bg.setCornerRadius(dp(14));
        bg.setStroke(1, 0xFF1E1E3A);
        ll.setBackgroundDrawable(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        ll.setLayoutParams(lp);
        return ll;
    }

    private View accentBtn(String text, int bgColor, int strokeColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15f);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), strokeColor);
        tv.setBackgroundDrawable(bg);
        tv.setPadding(dp(16), dp(0), dp(16), dp(0));
        return tv;
    }

    private View featureRow(String title, String desc) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        View dot = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(6), dp(6));
        dlp.setMarginEnd(dp(10));
        dot.setBackgroundDrawable(circle(0xFF6C63FF));
        top.addView(dot, dlp);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFDDDDDD);
        t.setTextSize(14f);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        top.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView check = new TextView(this);
        check.setText("✓");
        check.setTextColor(0xFF00FF88);
        check.setTextSize(14f);
        top.addView(check);

        row.addView(top);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(0xFF444466);
        d.setTextSize(12f);
        d.setPadding(dp(16), 0, 0, 0);
        row.addView(d);

        return row;
    }

    private android.graphics.drawable.Drawable circle(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams matchH(int h) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) launchESP();
            else Toast.makeText(this, "Cần quyền Overlay để chạy ESP", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (espRunning && !ESPOverlayService.isRunning) setESPState(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPulse();
    }
}
