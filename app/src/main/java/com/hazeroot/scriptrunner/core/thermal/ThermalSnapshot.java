package com.hazeroot.scriptrunner.core.thermal;

import java.util.ArrayList;
import java.util.List;

/**
 * 温控快照：一次 su 采集到的设备热状态。
 *
 * 设计来源（仅借鉴思路，代码为本项目独立实现）：
 *   - FrameX-Android (MIT) 的 dumpsys thermalservice 解析策略
 *   - KernelAdiutor / SmartPack (GPL) 仅借用 sysfs **路径字符串**这一层事实（路径不受版权保护）
 *
 * 均为「事实与思想」，未复制任何 GPL 代码片段。
 */
public final class ThermalSnapshot {

    /** 内核温区 /sys/class/thermal/thermal_zone* */
    public static final class Zone {
        public final String id;        // thermal_zone0
        public final String type;      // cpu_therm / battery / quiet_therm
        public final int tempMilliC;   // 已归一化为毫摄氏度
        public final float tempC;

        public Zone(String id, String type, int tempMilliC) {
            this.id = id;
            this.type = type;
            this.tempMilliC = tempMilliC;
            this.tempC = tempMilliC / 1000f;
        }
    }

    /** 散热设备 /sys/class/thermal/cooling_device* —— cur_state>0 代表正在介入（被限频/降档） */
    public static final class Cooling {
        public final String id;
        public final String type;
        public final int curState;
        public final int maxState;

        public Cooling(String id, String type, int curState, int maxState) {
            this.id = id;
            this.type = type;
            this.curState = curState;
            this.maxState = maxState;
        }

        public boolean isActive() {
            return curState > 0;
        }
    }

    /** CPU 集群频率 /sys/devices/system/cpu/cpufreq/policy* */
    public static final class CpuPolicy {
        public final String id;        // policy0
        public final long curFreq;     // kHz
        public final long maxFreq;     // 当前上限 kHz
        public final long hwMaxFreq;   // 硬件上限 kHz

        public CpuPolicy(String id, long curFreq, long maxFreq, long hwMaxFreq) {
            this.id = id;
            this.curFreq = curFreq;
            this.maxFreq = maxFreq;
            this.hwMaxFreq = hwMaxFreq;
        }

        /** 当前上限相对硬件上限的百分比（0-100），无法计算返回 -1 */
        public int maxPercent() {
            if (hwMaxFreq <= 0) {
                return -1;
            }
            return (int) Math.round(maxFreq * 100.0 / hwMaxFreq);
        }
    }

    public final List<Zone> zones = new ArrayList<>();
    public final List<Cooling> coolings = new ArrayList<>();
    public final List<CpuPolicy> cpuPolicies = new ArrayList<>();

    public long gpuCurFreq;
    public long gpuMaxFreq;
    public int batteryTempDeciC;   // 0.1℃
    public int batteryCapacity;    // %
    public int thermalStatus = -1; // dumpsys Thermal Status: 0..6，-1 表示未取到
    public String rawDump = "";

    /** 电池温度（℃） */
    public float batteryTempC() {
        return batteryTempDeciC / 10f;
    }

    /** 当前正在介入的散热设备 */
    public List<Cooling> activeCoolings() {
        List<Cooling> out = new ArrayList<>();
        for (Cooling c : coolings) {
            if (c.isActive()) {
                out.add(c);
            }
        }
        return out;
    }

    /** 是否正在被限频（任一 cooling_device 介入 或 热状态 >= 2） */
    public boolean isThrottling() {
        if (thermalStatus >= 2) {
            return true;
        }
        return !activeCoolings().isEmpty();
    }

    /** 最热的温区，跳过无效传感器；取不到返回 null */
    public Zone hottest() {
        Zone best = null;
        for (Zone z : zones) {
            if (z.tempMilliC <= 0) {
                continue;          // 0 = 无效/未接传感器
            }
            if (best == null || z.tempMilliC > best.tempMilliC) {
                best = z;
            }
        }
        return best;
    }

    /**
     * 按关键词找温度（如 cpu / battery / gpu），找不到返回 -1。
     * 采用「先精确 type 匹配，再忽略大小写包含匹配」两级策略，跳过无效传感器。
     */
    public float tempByKeyword(String keyword) {
        if (keyword == null || keyword.length() == 0) {
            return -1f;
        }
        String k = keyword.toLowerCase(java.util.Locale.ROOT);
        for (Zone z : zones) {
            if (z.tempMilliC > 0 && z.type != null && z.type.toLowerCase(java.util.Locale.ROOT).equals(k)) {
                return z.tempC;
            }
        }
        for (Zone z : zones) {
            if (z.tempMilliC > 0 && z.type != null && z.type.toLowerCase(java.util.Locale.ROOT).contains(k)) {
                return z.tempC;
            }
        }
        return -1f;
    }
}
