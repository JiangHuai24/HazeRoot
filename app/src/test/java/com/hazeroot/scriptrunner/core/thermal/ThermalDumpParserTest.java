package com.hazeroot.scriptrunner.core.thermal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * ThermalDumpParser / ThermalSnapshot 单元测试。
 *
 * 覆盖点（对应 P0 修复与温控功能的验收标准）：
 *   1. 温度单位自适应归一化（2-3 位数=℃，5 位数=毫℃）
 *   2. 结构化行解析（ZONE / COOL / CPU / GPU / BATT）
 *   3. dumpsys thermalservice 的 Thermal Status 解析
 *   4. 节流判定（cooling cur_state>0 或 thermalStatus>=2）
 *   5. 脏数据不崩（缺列、非数字、null）
 */
public class ThermalDumpParserTest {

    // ===== 温度归一化 =====

    @Test
    public void normalizeTemp_celsius2or3digits_isConvertedToMilli() {
        assertEquals(37_000, ThermalDumpParser.normalizeTemp(37));
        assertEquals(42_000, ThermalDumpParser.normalizeTemp(42));
        assertEquals(105_000, ThermalDumpParser.normalizeTemp(105));
    }

    @Test
    public void normalizeTemp_milliCelsius_passesThrough() {
        assertEquals(37_000, ThermalDumpParser.normalizeTemp(37_000));
        assertEquals(42_500, ThermalDumpParser.normalizeTemp(42_500));
    }

    @Test
    public void normalizeTemp_negativeCelsius_isConverted() {
        // 低温场景（如电池 -5℃ = -50 deciC 之类）应保持符号
        assertEquals(-5_000, ThermalDumpParser.normalizeTemp(-5));
    }

    @Test
    public void normalizeTemp_sentinelValue_isRejected() {
        // ★ 真机 bug：高通平台未接传感器的 -273000（-273℃ 哨兵）必须被过滤成 0，
        //   否则会被当成「最热温区」，UI 显示成乱值（实测显示 93.0℃）。
        assertEquals(0, ThermalDumpParser.normalizeTemp(-273_000));
        assertEquals(0, ThermalDumpParser.normalizeTemp(-127_000));
    }

    @Test
    public void normalizeTemp_outOfPhysicalRange_isRejected() {
        // 手机物理上不可能超过 150℃
        assertEquals(0, ThermalDumpParser.normalizeTemp(200_000));
        assertEquals(0, ThermalDumpParser.normalizeTemp(-50_000));
    }

    @Test
    public void hottest_skipsInvalidSensors() {
        // 真机实测数据片段：大量 -273000 哨兵 + 一个真实最高温 64.6℃
        String raw =
                "ZONE|z0|mmw3|-273000\n" +
                "ZONE|z1|cpu-1-9|64600\n" +
                "ZONE|z2|battery|32500\n" +
                "----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertNotNull(s.hottest());
        assertEquals("cpu-1-9", s.hottest().type);
        assertEquals(64.6f, s.hottest().tempC, 0.01f);
    }

    @Test
    public void normalizeTemp_zero_staysZero() {
        assertEquals(0, ThermalDumpParser.normalizeTemp(0));
    }

    // ===== 完整解析 =====

    @Test
    public void parse_fullSnapshot_allFieldsParsed() {
        String raw =
                "ZONE|thermal_zone0|cpu_therm|37000\n" +
                "ZONE|thermal_zone1|battery|35000\n" +
                "ZONE|thermal_zone2|gpu|39000\n" +
                "COOL|thermal-cpufreq-0|cpufreq|3|10\n" +
                "COOL|thermal-devfreq-0|devfreq|0|5\n" +
                "CPU|policy0|1234000|1804800|1804800\n" +
                "CPU|policy4|900000|1516800|1804800\n" +
                "GPU|315000000|587000000\n" +
                "BATT|345|87\n" +
                "----\n" +
                "Thermal Status: 2\n";

        ThermalSnapshot s = ThermalDumpParser.parse(raw);

        assertEquals(3, s.zones.size());
        assertEquals(2, s.coolings.size());
        assertEquals(2, s.cpuPolicies.size());
        assertEquals(315000000L, s.gpuCurFreq);
        assertEquals(587000000L, s.gpuMaxFreq);
        assertEquals(345, s.batteryTempDeciC);
        assertEquals(87, s.batteryCapacity);
        assertEquals(2, s.thermalStatus);
    }

    @Test
    public void parse_hottestZone_returned() {
        String raw = "ZONE|z0|cpu|37000\nZONE|z1|gpu|45000\nZONE|z2|battery|33000\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertNotNull(s.hottest());
        assertEquals("gpu", s.hottest().type);
        assertEquals(45.0f, s.hottest().tempC, 0.01f);
    }

    @Test
    public void parse_tempByKeyword_exactMatchWins() {
        String raw = "ZONE|z0|cpu_therm|37000\nZONE|z1|cpu|42000\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        // 精确 "cpu" 应优先于包含匹配的 "cpu_therm"
        assertEquals(42.0f, s.tempByKeyword("cpu"), 0.01f);
    }

    @Test
    public void parse_tempByKeyword_containsFallback() {
        String raw = "ZONE|z0|cpu_therm|37000\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertEquals(37.0f, s.tempByKeyword("cpu"), 0.01f);
    }

    @Test
    public void parse_tempByKeyword_notFound_returnsMinusOne() {
        String raw = "ZONE|z0|cpu|37000\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertEquals(-1f, s.tempByKeyword("npu"), 0.01f);
    }

    // ===== 节流判定 =====

    @Test
    public void isThrottling_coolingActive_isTrue() {
        String raw = "COOL|thermal-cpufreq-0|cpufreq|3|10\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertTrue(s.isThrottling());
    }

    @Test
    public void isThrottling_thermalStatusHigh_isTrue() {
        String raw = "----\nThermal Status: 3\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertTrue(s.isThrottling());
    }

    @Test
    public void isThrottling_allIdle_isFalse() {
        String raw = "COOL|thermal-cpufreq-0|cpufreq|0|10\n----\nThermal Status: 0\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertFalse(s.isThrottling());
    }

    @Test
    public void activeCoolings_onlyReturnsNonZero() {
        String raw = "COOL|a|cpufreq|3|10\nCOOL|b|devfreq|0|5\nCOOL|c|gpu|2|8\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertEquals(2, s.activeCoolings().size());
    }

    // ===== CPU 百分比 =====

    @Test
    public void cpuPolicy_maxPercent_computed() {
        ThermalSnapshot.CpuPolicy p = new ThermalSnapshot.CpuPolicy("policy0", 1000, 900_000, 1_800_000);
        assertEquals(50, p.maxPercent());
    }

    @Test
    public void cpuPolicy_maxPercent_zeroHw_returnsMinusOne() {
        ThermalSnapshot.CpuPolicy p = new ThermalSnapshot.CpuPolicy("policy0", 1000, 900_000, 0);
        assertEquals(-1, p.maxPercent());
    }

    // ===== 脏数据容错 =====

    @Test
    public void parse_nullInput_returnsEmptySnapshot() {
        ThermalSnapshot s = ThermalDumpParser.parse(null);
        assertNotNull(s);
        assertTrue(s.zones.isEmpty());
        assertEquals(-1, s.thermalStatus);
    }

    @Test
    public void parse_emptyInput_returnsEmptySnapshot() {
        ThermalSnapshot s = ThermalDumpParser.parse("");
        assertNotNull(s);
        assertTrue(s.zones.isEmpty());
    }

    @Test
    public void parse_malformedLines_doNotThrow() {
        String raw =
                "ZONE|only_two|\n" +
                "ZONE|\n" +
                "COOL|a|b\n" +
                "CPU|x|notanumber|y|z\n" +
                "GARBAGE\n" +
                "----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertNotNull(s);   // 不崩即可
    }

    @Test
    public void parse_noDumpsysSection_stillParsesStructured() {
        String raw = "ZONE|z0|cpu|37000\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertEquals(1, s.zones.size());
        assertEquals(-1, s.thermalStatus);
    }

    @Test
    public void parse_batteryTempC_conversion() {
        String raw = "BATT|345|87\n----\n";
        ThermalSnapshot s = ThermalDumpParser.parse(raw);
        assertEquals(34.5f, s.batteryTempC(), 0.01f);
    }

    // ===== 采集命令自检 =====

    @Test
    public void buildCollectCommand_containsKeyPathsAndSeparator() {
        String cmd = ThermalDumpParser.buildCollectCommand();
        assertNotNull(cmd);
        assertTrue(cmd.contains("/sys/class/thermal/thermal_zone"));
        assertTrue(cmd.contains("/sys/class/thermal/cooling_device"));
        assertTrue(cmd.contains("/sys/devices/system/cpu/cpufreq/policy"));
        assertTrue(cmd.contains("battery/temp"));
        assertTrue(cmd.contains("dumpsys thermalservice"));
        assertTrue(cmd.contains("----"));
    }
}
