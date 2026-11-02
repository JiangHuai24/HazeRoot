package com.hazeroot.scriptrunner.core.display;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * RefreshRateCommands 单元测试。
 *
 * 覆盖点（对应调研抓到的「照抄就踩」坑）：
 *   1. 三级回退命令格式正确
 *   2. 小米三键齐备（否则被 LTPO 吃掉）
 *   3. sfModeId 解析（不能一律 modeId-1）
 *   4. 回读帧率解析
 *   5. 恢复命令覆盖所有写入的 key
 */
public class RefreshRateCommandsTest {

    // ===== ① Android 14+ =====

    @Test
    public void setPreferredDisplayMode_formatCorrect() {
        String cmd = RefreshRateCommands.setPreferredDisplayMode(1080, 2400, 120);
        assertTrue(cmd.contains("cmd display set-user-preferred-display-mode"));
        assertTrue(cmd.contains("1080"));
        assertTrue(cmd.contains("2400"));
        assertTrue(cmd.contains("120"));
    }

    @Test
    public void clearPreferredDisplayMode_formatCorrect() {
        String cmd = RefreshRateCommands.clearPreferredDisplayMode();
        assertTrue(cmd.contains("clear-user-preferred-display-mode"));
    }

    // ===== ② 厂商 settings =====

    @Test
    public void setGenericRefreshRate_setsBothKeys() {
        String cmd = RefreshRateCommands.setGenericRefreshRate(120);
        assertTrue(cmd.contains("peak_refresh_rate"));
        assertTrue(cmd.contains("min_refresh_rate"));
        assertTrue(cmd.contains("120.0"));   // 必须是浮点，否则部分 ROM 不认
    }

    @Test
    public void setXiaomiRefreshRate_containsAllThreeKeys() {
        String cmd = RefreshRateCommands.setXiaomiRefreshRate(120);
        // 调研结论：小米/HyperOS 必须三键齐钉，缺一个就会被 LTPO 自适应吃掉
        assertTrue(cmd.contains("secure miui_refresh_rate"));
        assertTrue(cmd.contains("secure user_refresh_rate"));
        assertTrue(cmd.contains("system plugin_refresh_rate"));
    }

    // ===== ③ SF 事务码 =====

    @Test
    public void setSfRefreshRate_usesTokenThenIndex() {
        String cmd = RefreshRateCommands.setSfRefreshRate(5);
        // dfps 记录的序列：先 1036 token=1，再 1035 index
        assertTrue(cmd.contains("SurfaceFlinger 1036 i32 1"));
        assertTrue(cmd.contains("SurfaceFlinger 1035 i32 5"));
    }

    // ===== 恢复 =====

    @Test
    public void restoreAll_coversEveryWrittenKey() {
        String cmd = RefreshRateCommands.restoreAll();
        assertTrue(cmd.contains("clear-user-preferred-display-mode"));
        assertTrue(cmd.contains("peak_refresh_rate"));
        assertTrue(cmd.contains("min_refresh_rate"));
        assertTrue(cmd.contains("miui_refresh_rate"));
        assertTrue(cmd.contains("user_refresh_rate"));
        assertTrue(cmd.contains("plugin_refresh_rate"));
    }

    // ===== 回读校验 =====

    @Test
    public void parseRenderedFps_extractsFloat() {
        String out = "renderFrameRate=120.0\nmActiveModeId=3\n";
        assertEquals(120.0f, RefreshRateCommands.parseRenderedFps(out), 0.01f);
    }

    @Test
    public void parseRenderedFps_noMatch_returnsMinusOne() {
        assertEquals(-1f, RefreshRateCommands.parseRenderedFps("nothing here"), 0.01f);
        assertEquals(-1f, RefreshRateCommands.parseRenderedFps(null), 0.01f);
        assertEquals(-1f, RefreshRateCommands.parseRenderedFps(""), 0.01f);
    }

    // ===== sfModeId 映射解析（核心坑）=====

    @Test
    public void parseModeIndexMap_parsesPairs() {
        String dump =
                "mModeId=1 sfModeId=0\n" +
                "mModeId=3 sfModeId=5\n" +
                "mModeId=4 sfModeId=6\n";
        int[][] map = RefreshRateCommands.parseModeIndexMap(dump);
        assertEquals(3, map.length);
    }

    @Test
    public void findSfIndex_returnsRealMappingNotModeIdMinusOne() {
        // 这是本项目明确要避开的坑：真实映射是 1->0、3->5、4->6，
        // 绝不等于 modeId-1。
        String dump =
                "mModeId=1 sfModeId=0\n" +
                "mModeId=3 sfModeId=5\n" +
                "mModeId=4 sfModeId=6\n";
        int[][] map = RefreshRateCommands.parseModeIndexMap(dump);

        assertEquals(0, RefreshRateCommands.findSfIndex(map, 1));
        assertEquals(5, RefreshRateCommands.findSfIndex(map, 3));
        assertEquals(6, RefreshRateCommands.findSfIndex(map, 4));

        // 反证：如果按 modeId-1 猜，3 会得到 2，是错的
        assertEquals(-1, RefreshRateCommands.findSfIndex(map, 99));
    }

    @Test
    public void parseModeIndexMap_emptyOrNull_returnsEmpty() {
        assertEquals(0, RefreshRateCommands.parseModeIndexMap(null).length);
        assertEquals(0, RefreshRateCommands.parseModeIndexMap("").length);
        assertEquals(0, RefreshRateCommands.parseModeIndexMap("no ids here").length);
    }

    @Test
    public void parseModeIndexMap_lineWithoutBothIds_isIgnored() {
        String dump =
                "mModeId=1\n" +          // 缺 sfModeId
                "sfModeId=2\n" +         // 缺 mModeId
                "mModeId=3 sfModeId=5\n";
        int[][] map = RefreshRateCommands.parseModeIndexMap(dump);
        assertEquals(1, map.length);
        assertEquals(3, map[0][0]);
        assertEquals(5, map[0][1]);
    }

    @Test
    public void buildVerifyCommand_containsAllThreeVerifyPaths() {
        String cmd = RefreshRateCommands.buildVerifyCommand();
        assertNotNull(cmd);
        assertTrue(cmd.contains("renderFrameRate"));
        assertTrue(cmd.contains("dumpsys SurfaceFlinger"));
        assertTrue(cmd.contains("dynamic_fps"));
    }
}
