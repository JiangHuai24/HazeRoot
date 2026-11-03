package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * LsParser 单测。
 * 用真机实测的 ls -Al 输出作为样本（小米 fuxi / toybox）。
 */
public class LsParserTest {

    // ---- 真实样本（来自 /data/adb 的实际输出）----
    private static final String REAL_LS =
            "total 124\n"
            + "lrw-r--r--   1 root   root        30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys\n"
            + "drwxr-xr-x  84 root   root      1700 1970-01-14 05:59 apex\n"
            + "drwxr-xr-x   2 root   root      3452 2024-01-01 00:00 modules\n"
            + "drwx------   2 root   root      3452 2024-01-01 00:00 ksu\n"
            + "-rw-rw-r--   1 1000   1000       257 2024-01-01 00:00 new.sh\n";

    @Test
    public void parse_normalLines_producesEntries() {
        List<FileEntry> list = LsParser.parse(REAL_LS, "/data/adb");
        // total 行不匹配正则，被跳过 → 5 条
        assertEquals(5, list.size());
    }

    @Test
    public void parse_skipsTotalLine() {
        List<FileEntry> list = LsParser.parse("total 124\n", "/data/adb");
        assertTrue(list.isEmpty());
    }

    @Test
    public void parse_dirFlag_fromPermFirstChar() {
        List<FileEntry> list = LsParser.parse(REAL_LS, "/data/adb");
        FileEntry apex = find(list, "apex");
        assertNotNull(apex);
        assertTrue(apex.isDir());

        FileEntry sh = find(list, "new.sh");
        assertNotNull(sh);
        assertFalse(sh.isDir());
    }

    @Test
    public void parse_symlink_extractsTarget() {
        List<FileEntry> list = LsParser.parse(REAL_LS, "/data/adb");
        FileEntry link = find(list, "adb_keys");
        assertNotNull(link);
        assertEquals("/product/etc/security/adb_keys", link.getLinkTarget());
        assertTrue(link.isSymlink());
        // 权限首字符是 'l'，不是目录
        assertFalse(link.isDir());
    }

    @Test
    public void parse_size_parsedAsLong() {
        List<FileEntry> list = LsParser.parse(REAL_LS, "/data/adb");
        assertEquals(1700L, find(list, "apex").getSize());
        assertEquals(257L, find(list, "new.sh").getSize());
    }

    @Test
    public void parse_path_joinedWithDir() {
        List<FileEntry> list = LsParser.parse(REAL_LS, "/data/adb");
        assertEquals("/data/adb/apex", find(list, "apex").getPath());
    }

    @Test
    public void parse_dirWithoutTrailingSlash_stillJoinsCorrectly() {
        List<FileEntry> list = LsParser.parse(
                "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n", "/data/adb");
        assertEquals("/data/adb/sub", list.get(0).getPath());
    }

    @Test
    public void parse_dirWithTrailingSlash_noDoubleSlash() {
        List<FileEntry> list = LsParser.parse(
                "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n", "/data/adb/");
        assertEquals("/data/adb/sub", list.get(0).getPath());
    }

    @Test
    public void parse_dotAndDotdot_areDropped() {
        String out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 .\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 ..\n";
        assertTrue(LsParser.parse(out, "/x").isEmpty());
    }

    @Test
    public void parse_nullInput_returnsEmpty() {
        assertTrue(LsParser.parse(null, "/x").isEmpty());
    }

    @Test
    public void parse_emptyInput_returnsEmpty() {
        assertTrue(LsParser.parse("", "/x").isEmpty());
    }

    @Test
    public void parse_invalidLine_skipped() {
        assertTrue(LsParser.parse("garbage line here\n", "/x").isEmpty());
    }

    @Test
    public void parse_lineWithSpacesInName_keptWhole() {
        String out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 my folder name\n";
        List<FileEntry> list = LsParser.parse(out, "/x");
        assertEquals(1, list.size());
        assertEquals("my folder name", list.get(0).getName());
    }

    @Test
    public void parse_hiddenFile_flaggedHidden() {
        String out = "-rw-rw-r-- 1 root root 0 2024-01-01 00:00 .nomedia\n";
        List<FileEntry> list = LsParser.parse(out, "/x");
        assertEquals(1, list.size());
        assertTrue(list.get(0).isHidden());
    }

    @Test
    public void parse_crlfOutput_nameHasNoCarriageReturn() {
        // CRLF 输出（PC 端采集 / Windows 工具链）：若只按 \n 切，\r 会留在文件名末尾，
        // 导致后缀判断（.sh / .apk）全部失败、文件名显示异常。
        String out = "-rwxr-xr-x 1 root root 1024 2024-01-01 00:00 run.sh\r\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\r\n";
        List<FileEntry> list = LsParser.parse(out, "/x");
        assertEquals(2, list.size());
        FileEntry sh = find(list, "run.sh");
        assertNotNull("CRLF 下 run.sh 应被正确解析（名字不能带 \\r）", sh);
        assertFalse(sh.isDir());
        assertFalse(sh.getName().contains("\r"));
        FileEntry dir = find(list, "sub");
        assertNotNull(dir);
        assertTrue(dir.isDir());
    }

    private static FileEntry find(List<FileEntry> list, String name) {
        for (FileEntry e : list) {
            if (name.equals(e.getName())) {
                return e;
            }
        }
        return null;
    }
}
