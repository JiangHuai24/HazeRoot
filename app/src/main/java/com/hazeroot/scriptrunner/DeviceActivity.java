package com.hazeroot.scriptrunner;

import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hazeroot.scriptrunner.core.display.RefreshRateCommands;
import com.hazeroot.scriptrunner.core.thermal.ThermalCommands;
import com.hazeroot.scriptrunner.core.thermal.ThermalDumpParser;
import com.hazeroot.scriptrunner.core.thermal.ThermalSnapshot;

import java.util.Locale;
import java.util.TreeSet;

/**
 * 设备页：设备信息 + 温度/限频 + 温控开关 + 屏幕刷新率。
 *
 * 设计原则（按开源调研结论）：
 *   - 只做低风险能力（L1 热节流解除）+ 频率回读校验；L2/L3 高风险项明确不开放
 *   - 所有写操作必须「回读校验」，不信 exit code
 *   - 所有 su 调用都在后台线程
 */
public final class DeviceActivity extends AppCompatActivity {

    private TextView tvDeviceInfo;
    private TextView tvThermalState;
    private TextView tvThermalDetail;
    private TextView tvRefreshCurrent;
    private LinearLayout refreshButtons;

    private ThermalSnapshot snapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        tvThermalState = findViewById(R.id.tvThermalState);
        tvThermalDetail = findViewById(R.id.tvThermalDetail);
        tvRefreshCurrent = findViewById(R.id.tvRefreshCurrent);
        refreshButtons = findViewById(R.id.refreshButtons);

        View back = findViewById(R.id.btnBack);
        if (back != null) {
            back.setOnClickListener(v -> finish());
        }
        View refresh = findViewById(R.id.btnRefresh);
        if (refresh != null) {
            refresh.setOnClickListener(v -> loadAll());
        }
        View off = findViewById(R.id.btnThermalOff);
        if (off != null) {
            off.setOnClickListener(v -> runThermalCommand(ThermalCommands.overrideStatusNone(),
                    getString(R.string.thermal_l1_off)));
        }
        View reset = findViewById(R.id.btnThermalReset);
        if (reset != null) {
            reset.setOnClickListener(v -> runThermalCommand(ThermalCommands.resetThermalStatus(),
                    getString(R.string.thermal_l1_reset)));
        }
        View refreshReset = findViewById(R.id.btnRefreshReset);
        if (refreshReset != null) {
            refreshReset.setOnClickListener(v -> runThermalCommand(RefreshRateCommands.restoreAll(),
                    getString(R.string.refresh_reset)));
        }

        loadAll();
    }

    private void loadAll() {
        showDeviceInfo();
        loadThermal();
        loadRefreshRates();
    }

    // ===== 设备信息（纯本地 API，无需 root） =====

    private void showDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("品牌    ").append(Build.BRAND).append('\n');
        sb.append("型号    ").append(Build.MODEL).append('\n');
        sb.append("设备    ").append(Build.DEVICE).append('\n');
        sb.append("主板    ").append(Build.BOARD).append('\n');
        sb.append("硬件    ").append(Build.HARDWARE).append('\n');
        sb.append("Android ").append(Build.VERSION.RELEASE)
          .append("  (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("ABI     ").append(Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0] : "?").append('\n');
        sb.append("内核    ").append(System.getProperty("os.version", "?")).append('\n');
        sb.append("指纹    ").append(Build.FINGERPRINT);
        tvDeviceInfo.setText(sb.toString());
    }

    // ===== 温控（后台 su 采集 + 解析） =====

    private void loadThermal() {
        tvThermalState.setText(R.string.thermal_reading);
        tvThermalDetail.setText("");
        new Thread(() -> {
            String raw = Engine.INSTANCE.exec(ThermalDumpParser.buildCollectCommand());
            final ThermalSnapshot snap = ThermalDumpParser.parse(raw);
            runOnUiThread(() -> {
                snapshot = snap;
                renderThermal(snap);
            });
        }).start();
    }

    private void renderThermal(ThermalSnapshot s) {
        if (s == null) {
            tvThermalState.setText("读取失败");
            return;
        }
        ThermalSnapshot.Zone hot = s.hottest();
        String state;
        if (s.thermalStatus >= 3) {
            state = "热状态 " + s.thermalStatus + " · 严重";
        } else if (s.isThrottling()) {
            state = "正在限频";
        } else {
            state = "正常";
        }
        String head = state;
        if (hot != null) {
            head += String.format(Locale.ROOT, "  ·  最高 %.1f℃", hot.tempC);
        }
        tvThermalState.setText(head);

        StringBuilder sb = new StringBuilder();
        sb.append("电池    ").append(String.format(Locale.ROOT, "%.1f℃", s.batteryTempC()))
          .append("  ").append(s.batteryCapacity).append("%\n");
        sb.append("热状态  ").append(s.thermalStatus >= 0 ? String.valueOf(s.thermalStatus) : "未取到").append('\n');

        int active = s.activeCoolings().size();
        sb.append("限频设备 ").append(active).append(" / ").append(s.coolings.size()).append(" 个介入\n");

        float cpuT = s.tempByKeyword("cpu");
        if (cpuT > 0) {
            sb.append("CPU     ").append(String.format(Locale.ROOT, "%.1f℃", cpuT)).append('\n');
        }
        float gpuT = s.tempByKeyword("gpu");
        if (gpuT > 0) {
            sb.append("GPU     ").append(String.format(Locale.ROOT, "%.1f℃", gpuT)).append('\n');
        }
        for (ThermalSnapshot.CpuPolicy p : s.cpuPolicies) {
            int pct = p.maxPercent();
            sb.append(p.id).append("  上限 ")
              .append(p.maxFreq / 1000).append("MHz");
            if (pct >= 0) {
                sb.append("  (").append(pct).append("%)");
            }
            sb.append('\n');
        }
        if (s.gpuMaxFreq > 0) {
            sb.append("GPU 上限 ").append(s.gpuMaxFreq / 1000000).append("MHz\n");
        }
        for (ThermalSnapshot.Cooling c : s.activeCoolings()) {
            sb.append("· ").append(c.type).append("  ").append(c.curState).append('/').append(c.maxState).append('\n');
        }
        tvThermalDetail.setText(sb.toString().trim());
    }

    // ===== 刷新率 =====

    private void loadRefreshRates() {
        refreshButtons.removeAllViews();
        Display display = resolveDefaultDisplay();
        if (display == null) {
            tvRefreshCurrent.setText("无法获取显示信息");
            return;
        }
        Display.Mode cur = display.getMode();
        float curHz = cur != null ? cur.getRefreshRate() : 0f;

        // 按整数 Hz 去重（同一 Hz 可能有多个 mode）
        TreeSet<Integer> hzSet = new TreeSet<>();
        final Display.Mode[] modes = display.getSupportedModes();
        for (Display.Mode m : modes) {
            hzSet.add(Math.round(m.getRefreshRate()));
        }
        // 保证当前刷新率也在列表里
        hzSet.add(Math.round(curHz));

        tvRefreshCurrent.setText(String.format(Locale.ROOT, "当前 %.0f Hz  ·  支持 %d 档", curHz, hzSet.size()));

        final int w = cur != null ? cur.getPhysicalWidth() : 0;
        final int h = cur != null ? cur.getPhysicalHeight() : 0;

        for (final int hz : hzSet) {
            TextView btn = new TextView(this);
            btn.setText(hz + "Hz");
            btn.setTextSize(13f);
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setTextColor(0xFF1C1E24);
            btn.setBackgroundResource(Math.round(curHz) == hz
                    ? R.drawable.btn_accent_bg : R.drawable.btn_ghost_bg);
            if (Math.round(curHz) == hz) {
                btn.setTextColor(0xFFFFFFFF);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, dp(40), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> lockRefreshRate(w, h, hz));
            refreshButtons.addView(btn);
        }
    }

    /** 锁定刷新率：三级回退 + 回读校验 */
    private void lockRefreshRate(final int w, final int h, final int hz) {
        toast("正在锁定 " + hz + "Hz…");
        new Thread(() -> {
            StringBuilder cmd = new StringBuilder();
            // ① Android 14+ / 通用
            cmd.append(RefreshRateCommands.setPreferredDisplayMode(w, h, hz)).append("; ");
            // ② 通用 settings
            cmd.append(RefreshRateCommands.setGenericRefreshRate(hz)).append("; ");
            // ② 小米 / HyperOS 补三键
            if (isXiaomi()) {
                cmd.append(RefreshRateCommands.setXiaomiRefreshRate(hz)).append("; ");
            }
            cmd.append("true");
            Engine.INSTANCE.exec(cmd.toString());

            // ③ 回读校验
            String verify = Engine.INSTANCE.exec(RefreshRateCommands.buildVerifyCommand());
            final float actual = RefreshRateCommands.parseRenderedFps(verify);
            final boolean ok = actual > 0 && Math.abs(actual - hz) <= 2f;

            runOnUiThread(() -> {
                if (actual > 0) {
                    toast(ok ? "已锁定 " + hz + "Hz（实测 " + Math.round(actual) + "Hz）"
                             : "已写入，实测 " + Math.round(actual) + "Hz（可能被系统策略接管）");
                } else {
                    toast("已写入，但读不到实测帧率，请手动确认");
                }
                loadRefreshRates();
            });
        }).start();
    }

    private boolean isXiaomi() {
        String b = Build.BRAND == null ? "" : Build.BRAND.toLowerCase(Locale.ROOT);
        String m = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        return b.contains("xiaomi") || b.contains("redmi") || b.contains("poco") || m.contains("xiaomi");
    }

    // ===== 通用执行（后台 + 回读提示） =====

    private void runThermalCommand(final String cmd, final String label) {
        toast("正在执行：" + label);
        new Thread(() -> {
            final String out = Engine.INSTANCE.exec(cmd);
            runOnUiThread(() -> {
                toast(out != null ? label + " 已执行" : label + " 执行失败（可能无 root）");
                loadThermal();
            });
        }).start();
    }

    /** 取默认 Display。
     *  注：不能命名为 getDisplay() —— 会与 Context.getDisplay()（API 30+）冲突。 */
    private Display resolveDefaultDisplay() {
        android.hardware.display.DisplayManager dm =
                (android.hardware.display.DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (dm != null) {
            Display d = dm.getDisplay(Display.DEFAULT_DISPLAY);
            if (d != null) {
                return d;
            }
        }
        return getWindowManager().getDefaultDisplay();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
