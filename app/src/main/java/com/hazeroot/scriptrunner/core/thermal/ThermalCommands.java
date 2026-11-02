package com.hazeroot.scriptrunner.core.thermal;

import java.util.List;

/**
 * 温控开关命令构造器（纯 Java，只生成命令字符串，不执行）。
 *
 * 三级风险分级（按调研结论从低到高，默认全关）：
 *   L1 低风险：cmd thermalservice override-status 0（框架热状态覆盖，可 reset 恢复）
 *   L2 中风险：CPU/GPU 频率上限直控（可写回硬件上限恢复）
 *   L3 高风险：小米 mi_thermald 配置覆写（本类暂不实现，见注释说明）
 *
 * 事实层来源（路径/命令字符串属事实，不受版权保护）：
 *   - AOSP ThermalManagerService：Context.THERMAL_SERVICE = "thermalservice"
 *     正确写法是 `cmd thermalservice override-status N`，**不是** `cmd thermal ...`（后者是常见错误）
 *   - sysfs cpufreq / kgsl 路径为 Linux 内核标准接口
 */
public final class ThermalCommands {

    private ThermalCommands() {
    }

    // ===== L1 低风险：框架热状态覆盖 =====

    /** 关闭热节流（把热状态强制为 NONE=0）。可用 resetThermalStatus 恢复。 */
    public static String overrideStatusNone() {
        return "cmd thermalservice override-status 0";
    }

    /** 恢复框架热状态（取消覆盖） */
    public static String resetThermalStatus() {
        return "cmd thermalservice reset";
    }

    // ===== L2 中风险：频率上限 =====

    /**
     * 构造「写 CPU 集群上限」命令。
     *
     * @param policyId  policy 编号（如 policy0）
     * @param freqKhz   目标上限 kHz
     */
    public static String setCpuMaxFreq(String policyId, long freqKhz) {
        String path = "/sys/devices/system/cpu/cpufreq/" + policyId + "/scaling_max_freq";
        return writeNode(path, String.valueOf(freqKhz));
    }

    /** 构造「写 GPU 上限」命令（高通 kgsl） */
    public static String setGpuMaxFreq(long freqHz) {
        return writeNode("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq", String.valueOf(freqHz));
    }

    /**
     * 恢复所有 CPU 集群到硬件上限。
     * 用采集到的 cpuinfo_max_freq 逐个写回，不硬编码任何频率。
     */
    public static String restoreCpuMaxFreq(List<ThermalSnapshot.CpuPolicy> policies) {
        if (policies == null || policies.isEmpty()) {
            return "true";
        }
        StringBuilder sb = new StringBuilder();
        for (ThermalSnapshot.CpuPolicy p : policies) {
            if (p.hwMaxFreq > 0) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(writeNode("/sys/devices/system/cpu/cpufreq/" + p.id + "/scaling_max_freq",
                        String.valueOf(p.hwMaxFreq)));
            }
        }
        return sb.length() > 0 ? sb.toString() : "true";
    }

    /** 恢复 GPU 到硬件上限 */
    public static String restoreGpuMaxFreq(long hwMaxFreq) {
        if (hwMaxFreq <= 0) {
            return "true";
        }
        return writeNode("/sys/class/kgsl/kgsl-3d0/devfreq/max_freq", String.valueOf(hwMaxFreq));
    }

    /**
     * 写 sysfs 节点的标准写法：
     *   ① 先 chmod 0664（很多机型默认 0444 只读，直接 echo 会失败）
     *   ② 回读校验（exit code 不可靠，必须 echo 出来比对）
     */
    private static String writeNode(String path, String value) {
        return "chmod 0664 " + path + " 2>/dev/null; "
             + "echo " + value + " > " + path + " 2>/dev/null; "
             + "cat " + path + " 2>/dev/null";
    }

    // ===== 恢复原厂（一键） =====

    /** 一键恢复：热状态 reset + CPU/GPU 频率回硬件上限 */
    public static String restoreAll(ThermalSnapshot snap) {
        StringBuilder sb = new StringBuilder();
        sb.append(resetThermalStatus()).append("; ");
        if (snap != null) {
            sb.append(restoreCpuMaxFreq(snap.cpuPolicies)).append("; ");
            sb.append(restoreGpuMaxFreq(snap.gpuMaxFreq));
        } else {
            sb.append("true");
        }
        return sb.toString();
    }

    // ===== L3 说明（暂不实现）=====
    //
    // 小米 mi_thermald 配置覆写（/data/vendor/thermal/config/）属于高风险操作：
    //   - 需要 AES-128-CBC 加解密（key = IV = ASCII "thermalopenssl.h"）
    //   - 写错会导致热失控（设备持续高温）或相机/充电被禁
    //   - 必须做开机自检防止用户遗忘
    // 按「不做容量不足的功能」原则，**本轮不实现**，仅在 UI 上以「暂不支持」明确告知，
    // 避免用户误以为可用。待 L1/L2 真机验证稳定后再评估。
}
