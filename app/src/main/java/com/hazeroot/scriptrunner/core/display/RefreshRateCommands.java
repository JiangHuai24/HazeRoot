package com.hazeroot.scriptrunner.core.display;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 刷新率锁定命令构造器（纯 Java，只生成命令字符串）。
 *
 * 三级回退策略（借鉴 RefreshRateSwitcher (MIT) 的设计思路，代码独立实现）：
 *   ① Android 14+ 首选：cmd display set-user-preferred-display-mode W H FPS 0
 *   ② 厂商 settings：小米/HyperOS 必须补三键，否则被 LTPO 自适应吃掉
 *      另加通用 peak/min_refresh_rate
 *   ③ 兜底：service call SurfaceFlinger 1035 i32 <sfIndex>
 *         索引必须从 dumpsys display 的 sfModeId 解析，**不能一律 modeId-1**
 *
 * 事实层来源：AOSP SurfaceFlinger / DisplayManager 的公开接口语义。
 */
public final class RefreshRateCommands {

    /** 解析 dumpsys display 中 modeId -> sfModeId 的映射 */
    private static final Pattern MODE_ID = Pattern.compile("mModeId=(\\d+)");
    private static final Pattern SF_MODE = Pattern.compile("sfModeId=(\\d+)");

    /** 解析 dumpsys display 里当前渲染帧率（用于回读校验） */
    private static final Pattern RENDER_FPS = Pattern.compile("renderFrameRate=([0-9.]+)");

    private RefreshRateCommands() {
    }

    // ===== ① Android 14+ 首选 =====

    public static String setPreferredDisplayMode(int width, int height, int fps) {
        return "cmd display set-user-preferred-display-mode " + width + " " + height + " " + fps + " 0";
    }

    public static String clearPreferredDisplayMode() {
        return "cmd display clear-user-preferred-display-mode 0";
    }

    // ===== ② 厂商 settings =====

    /** 通用：peak / min refresh rate */
    public static String setGenericRefreshRate(int fps) {
        return "settings put system peak_refresh_rate " + fps + ".0; "
             + "settings put system min_refresh_rate " + fps + ".0";
    }

    /**
     * 小米 / HyperOS：必须同时钉三个 key，否则 LTPO 会自适应回落到低刷。
     *   secure miui_refresh_rate / secure user_refresh_rate / system plugin_refresh_rate
     */
    public static String setXiaomiRefreshRate(int fps) {
        return "settings put secure miui_refresh_rate " + fps + "; "
             + "settings put secure user_refresh_rate " + fps + "; "
             + "settings put system plugin_refresh_rate " + fps;
    }

    // ===== ③ 兜底：SF 事务码 =====

    /**
     * 用 SurfaceFlinger 事务码锁定刷新率。
     * 按 dfps (Apache-2.0) 记录的序列：1036 token -> 1035 index
     * 索引必须来自 dumpsys display 解析，不能猜。
     */
    public static String setSfRefreshRate(int sfIndex) {
        return "service call SurfaceFlinger 1036 i32 1; "
             + "service call SurfaceFlinger 1035 i32 " + sfIndex;
    }

    // ===== 恢复 =====

    public static String restoreAll() {
        return "cmd display clear-user-preferred-display-mode 0; "
             + "settings delete system peak_refresh_rate; "
             + "settings delete system min_refresh_rate; "
             + "settings delete secure miui_refresh_rate; "
             + "settings delete secure user_refresh_rate; "
             + "settings delete system plugin_refresh_rate";
    }

    // ===== 回读校验 =====

    /**
     * 构造回读命令：拿到当前实际渲染帧率与显示模式，用于验证「锁定是否真的生效」。
     * 不做回读校验就无法区分「写成功」和「被系统/厂商策略覆盖」。
     */
    public static String buildVerifyCommand() {
        return "dumpsys display 2>/dev/null | grep -m3 -E 'renderFrameRate|mActiveModeId|sfModeId'; "
             + "dumpsys SurfaceFlinger 2>/dev/null | grep -m1 -E 'refresh-rate|activeMode'; "
             + "cat /sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/dynamic_fps 2>/dev/null";
    }

    /** 从回读文本里解析当前渲染帧率，解析不到返回 -1 */
    public static float parseRenderedFps(String verifyOutput) {
        if (verifyOutput == null) {
            return -1f;
        }
        Matcher m = RENDER_FPS.matcher(verifyOutput);
        if (m.find()) {
            try {
                return Float.parseFloat(m.group(1));
            } catch (Exception ignored) {
                return -1f;
            }
        }
        return -1f;
    }

    /**
     * 从 dumpsys display 文本中解析「modeId -> sfModeId」映射。
     * 返回 int[]{modeId, sfModeId} 数组对；解析失败返回空数组。
     *
     * 这是本模块最容易踩的坑：很多实现直接用 modeId-1 当 sfIndex，
     * 但实测（小米 17 Pro）映射是 1->0、3->5、4->6，不成立。
     */
    public static int[][] parseModeIndexMap(String dump) {
        if (dump == null || dump.length() == 0) {
            return new int[0][0];
        }
        java.util.List<int[]> pairs = new java.util.ArrayList<>();
        // 按行扫描：同一行里同时出现 mModeId 与 sfModeId 视为一对
        for (String line : dump.split("\n")) {
            Matcher mid = MODE_ID.matcher(line);
            Matcher sfm = SF_MODE.matcher(line);
            if (mid.find() && sfm.find()) {
                try {
                    pairs.add(new int[]{Integer.parseInt(mid.group(1)), Integer.parseInt(sfm.group(1))});
                } catch (Exception ignored) {
                }
            }
        }
        return pairs.toArray(new int[0][0]);
    }

    /** 按 modeId 查 sfIndex，查不到返回 -1 */
    public static int findSfIndex(int[][] map, int modeId) {
        for (int[] p : map) {
            if (p.length >= 2 && p[0] == modeId) {
                return p[1];
            }
        }
        return -1;
    }
}
