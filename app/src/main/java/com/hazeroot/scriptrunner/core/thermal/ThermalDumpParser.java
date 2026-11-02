package com.hazeroot.scriptrunner.core.thermal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 温控数据解析器：把「一次 su 采集到的原始文本」解析为 ThermalSnapshot。
 *
 * 纯 Java，无 android.* 依赖，可单元测试。
 *
 * 采集格式约定（由 ThermalCommandRepository 生成的 shell 脚本输出，见 buildCollectCommand）：
 *   ZONE|thermal_zone0|cpu_therm|37000
 *   COOL|thermal-cpufreq-0|cpufreq|3|10
 *   CPU|policy0|1234000|1804800|1804800
 *   GPU|<cur>|<max>
 *   BATT|<tempDeciC>|<capacity>
 *   TSTATE|<temp_state>
 *   ---- 之后为 dumpsys thermalservice 原文
 *
 * 设计借鉴（思路层面，代码独立实现）：
 *   - FrameX-Android (MIT)：dumpsys thermalservice 的 Thermal Status 段解析策略
 *   - KernelAdiutor (GPL)：温度单位「2 位数则视为 ℃、5 位数则视为毫摄氏度」的自适应纠偏思路
 */
public final class ThermalDumpParser {

    private static final Pattern THERMAL_STATUS = Pattern.compile("Thermal Status:\\s*(\\d+)");

    private ThermalDumpParser() {
    }

    /** 生成采集命令：一次 su 拿到全部数据，避免开多个进程 */
    public static String buildCollectCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("for z in /sys/class/thermal/thermal_zone*; do ")
          .append("[ -e \"$z/type\" ] && echo \"ZONE|${z##*/}|$(cat $z/type 2>/dev/null)|$(cat $z/temp 2>/dev/null)\"; ")
          .append("done; ");
        sb.append("for c in /sys/class/thermal/cooling_device*; do ")
          .append("[ -e \"$c/type\" ] && echo \"COOL|${c##*/}|$(cat $c/type 2>/dev/null)|$(cat $c/cur_state 2>/dev/null)|$(cat $c/max_state 2>/dev/null)\"; ")
          .append("done; ");
        sb.append("for p in /sys/devices/system/cpu/cpufreq/policy*; do ")
          .append("echo \"CPU|${p##*/}|$(cat $p/scaling_cur_freq 2>/dev/null)|$(cat $p/scaling_max_freq 2>/dev/null)|$(cat $p/cpuinfo_max_freq 2>/dev/null)\"; ")
          .append("done; ");
        sb.append("echo \"GPU|$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq 2>/dev/null)|$(cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null)\"; ");
        sb.append("echo \"BATT|$(cat /sys/class/power_supply/battery/temp 2>/dev/null)|$(cat /sys/class/power_supply/battery/capacity 2>/dev/null)\"; ");
        sb.append("echo \"TSTATE|$(cat /sys/class/thermal/thermal_message/temp_state 2>/dev/null)\"; ");
        sb.append("echo '----'; ");
        sb.append("dumpsys thermalservice 2>/dev/null | head -120");
        return sb.toString();
    }

    /** 解析采集结果 */
    public static ThermalSnapshot parse(String raw) {
        ThermalSnapshot s = new ThermalSnapshot();
        if (raw == null) {
            return s;
        }
        s.rawDump = raw;
        int dumpStart = raw.indexOf("----");
        String structured = dumpStart >= 0 ? raw.substring(0, dumpStart) : raw;
        String dumpsys = dumpStart >= 0 ? raw.substring(dumpStart) : "";

        for (String line : structured.split("\n")) {
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            String[] parts = line.split("\\|", -1);
            try {
                switch (parts[0]) {
                    case "ZONE":
                        if (parts.length >= 4) {
                            s.zones.add(new ThermalSnapshot.Zone(
                                    parts[1], parts[2], normalizeTemp(parseInt(parts[3], 0))));
                        }
                        break;
                    case "COOL":
                        if (parts.length >= 5) {
                            s.coolings.add(new ThermalSnapshot.Cooling(
                                    parts[1], parts[2], parseInt(parts[3], 0), parseInt(parts[4], 0)));
                        }
                        break;
                    case "CPU":
                        if (parts.length >= 5) {
                            s.cpuPolicies.add(new ThermalSnapshot.CpuPolicy(
                                    parts[1], parseLong(parts[2], 0), parseLong(parts[3], 0), parseLong(parts[4], 0)));
                        }
                        break;
                    case "GPU":
                        if (parts.length >= 3) {
                            s.gpuCurFreq = parseLong(parts[1], 0);
                            s.gpuMaxFreq = parseLong(parts[2], 0);
                        }
                        break;
                    case "BATT":
                        if (parts.length >= 3) {
                            s.batteryTempDeciC = parseInt(parts[1], 0);
                            s.batteryCapacity = parseInt(parts[2], 0);
                        }
                        break;
                    case "TSTATE":
                        // 小米 thermal_message/temp_state，形如 "0,0,0,0,0"，取第一个有效值仅作参考
                        break;
                    default:
                        break;
                }
            } catch (Exception ignored) {
                // 单行坏了不影响整体
            }
        }

        // dumpsys thermalservice：取 Thermal Status
        Matcher m = THERMAL_STATUS.matcher(dumpsys);
        if (m.find()) {
            s.thermalStatus = parseInt(m.group(1), -1);
        }
        return s;
    }

    /**
     * 温度归一化为毫摄氏度。
     * 借鉴 KernelAdiutor 的自适应思路：不同机型 /temp 可能是 2~3 位数（℃）或 5 位数（毫℃）。
     *   <= 200      → 视为 ℃，×1000
     *   <= 200000   → 视为毫℃，原样
     *   >  200000   → 异常值，按毫℃保留（后续展示层处理）
     */
    static int normalizeTemp(int raw) {
        if (raw == 0) {
            return 0;
        }
        int abs = Math.abs(raw);
        if (abs <= 200) {
            return raw * 1000;
        }
        return raw;
    }

    private static int parseInt(String s, int def) {
        if (s == null) {
            return def;
        }
        s = s.trim();
        if (s.length() == 0) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static long parseLong(String s, long def) {
        if (s == null) {
            return def;
        }
        s = s.trim();
        if (s.length() == 0) {
            return def;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return def;
        }
    }
}
