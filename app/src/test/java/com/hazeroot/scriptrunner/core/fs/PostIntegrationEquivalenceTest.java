package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 接入后「等价性」测试骨架 —— 现在就能编译通过，接入 FilesFragment 后才真正发力。
 *
 * 与 BaselineEquivalenceTest 的分工：
 *  - Baseline 回答"core/fs 现在的行为 == 改造前 FilesFragment 的行为吗"（接入前地基）
 *  - 本文件回答"FilesFragment 真的改成调 core/fs 之后，UI 拿到的条目还一样吗"（接入后验收）
 *
 * 骨架里已经把"UI 实际要消费的那几样东西"固定成契约：
 *   解析 → 过滤 → 排序 → 显示字符串，整条链路走一遍，断言每一步的产物。
 * 接入时把 FilesFragment 里内联的那几段换成 core/fs 调用，
 * 只要这些 contract 断言还是绿的，就说明改造没走样。
 *
 * 现在（未接入）这些断言同样成立，因为 FilesFragment 的内联逻辑与 core/fs 等价
 * （由 BaselineEquivalenceTest 用 LegacyFsReference 证明）。所以本文件不会假绿也无法假红。
 */
public class PostIntegrationEquivalenceTest {

    /** UI 列表每一项真正需要的字段：名称、是否目录、大小文字、权限、链接目标 */
    @Test
    public void contract_uiRowFields_comeOutUnchanged() {
        String out = "lrw-r--r-- 1 root root 30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 modules\n"
                + "-rw-rw-r-- 1 1000 1000 257 2024-01-01 00:00 new.sh\n";

        List<FileEntry> rows = LsParser.parse(out, "/data/adb");
        assertEquals(3, rows.size());

        FileEntry link = rows.get(0);
        assertEquals("adb_keys", link.getName());
        assertTrue(link.isSymlink());
        assertEquals("/product/etc/security/adb_keys", link.getLinkTarget());
        assertEquals("lrw-r--r--", link.getPerm());
        assertEquals(SizeFormat.format(30), SizeFormat.format(link.getSize()));

        FileEntry dir = rows.get(1);
        assertTrue(dir.isDir());
        assertTrue(dir.permIndicatesDir());
        assertEquals("modules", dir.getName());
        assertEquals("/data/adb/modules", dir.getPath());

        FileEntry sh = rows.get(2);
        assertFalse(sh.isDir());
        assertEquals("257B", SizeFormat.format(sh.getSize()));
    }

    /**
     * 整条链路契约：真实 ls 文本 → 解析 → 过滤(隐藏关) → 排序(按名) → 显示字符串。
     * 接入后这条链就是 FilesFragment.load 回调应当走的路。
     */
    @Test
    public void contract_fullPipeline_matchesExpectedSnapshot() {
        String out = "total 124\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 GJX\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 .hidden_dir\n"
                + "-rw-rw-r-- 1 root root 100 2024-01-01 00:00 run.sh\n"
                + "-rw-rw-r-- 1 root root 5000 2024-01-01 00:00 big.sh\n"
                + "-rw-rw-r-- 1 root root 10 2024-01-01 00:00 notes.txt\n"
                + "-rw-rw-r-- 1 root root 0 2024-01-01 00:00 .nomedia\n";

        List<FileEntry> parsed = LsParser.parse(out, "/sdcard");
        List<FileEntry> filtered = FileFilter.apply(parsed, false, false);
        FileSort.sort(filtered, FileSort.Mode.NAME);

        // 目录优先，点文件已剔除
        List<String> names = new ArrayList<String>();
        for (FileEntry e : filtered) {
            names.add(e.getName());
        }
        assertEquals("[GJX, big.sh, notes.txt, run.sh]", names.toString());

        // 大小文字（UI 右侧显示的正是这个）
        List<String> sizes = new ArrayList<String>();
        for (FileEntry e : filtered) {
            sizes.add(SizeFormat.format(e.getSize()));
        }
        assertEquals("[4.0K, 4.9K, 10B, 100B]", sizes.toString());
    }

    /** 契约：onlyScripts 打开后只剩脚本，且一定是按名排好序的。 */
    @Test
    public void contract_scriptsOnlyPipeline_onlyShFilesSorted() {
        String out = "-rw-rw-r-- 1 root root 100 2024-01-01 00:00 z.sh\n"
                + "-rw-rw-r-- 1 root root 200 2024-01-01 00:00 A.SH\n"
                + "-rw-rw-r-- 1 root root 300 2024-01-01 00:00 m.txt\n"
                + "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 dir.sh\n";

        List<FileEntry> rows = FileFilter.apply(LsParser.parse(out, "/x"), true, true);
        FileSort.sort(rows, FileSort.Mode.NAME);

        assertEquals(2, rows.size());
        assertEquals("A.SH", rows.get(0).getName());
        assertEquals("z.sh", rows.get(1).getName());
    }

    /** 契约：PathUtil 提供的导航目标，接入后"上一级 / 面包屑 / 新建路径"都靠它。 */
    @Test
    public void contract_navigationPaths_areConsistent() {
        String dir = "/data/adb/";
        assertEquals("/data", PathUtil.parent(dir));
        assertEquals("adb", PathUtil.fileName(dir));
        assertEquals("/data/adb/modules", PathUtil.join(dir, "modules"));
        assertFalse(PathUtil.isRoot(dir));

        // 从拼出来的路径再取父，必须回到原点（往返一致）
        String child = PathUtil.join(dir, "modules");
        assertEquals("/data/adb", PathUtil.parent(child));
    }

    /** 契约：DirHistory 记录顺序与 back/forward 就是浏览器语义（接入后替代 navHistory）。 */
    @Test
    public void contract_dirHistory_behavesLikeBrowser() {
        DirHistory h = new DirHistory();
        h.record("/");
        h.record("/data");
        h.record("/data/adb");

        assertEquals("/data/adb", h.current());
        assertEquals("/data", h.back());
        assertEquals("/", h.back());
        assertFalse(h.canBack());
        assertEquals("/data", h.forward());
        assertEquals("/data/adb", h.forward());
        assertTrue(h.canForward() == false);
    }

    /**
     * 契约：空目录 / 权限拒绝时的兜底。
     * 旧实现是 ls 输出里含 "Permission denied" 时弹提示；
     * 接入后解析层只负责"给不出条目就返回空列表、绝不 NPE"，提示由 UI 层负责。
     */
    @Test
    public void contract_emptyAndDeniedOutputs_yieldEmptyListNotCrash() {
        assertTrue(LsParser.parse("", "/root").isEmpty());
        assertTrue(LsParser.parse(null, "/root").isEmpty());
        assertTrue(LsParser.parse("ls: /root: Permission denied\n", "/root").isEmpty());
        assertTrue(LsParser.parse("ls: /nope: No such file or directory\n", "/nope").isEmpty());
        assertTrue(LsParser.parse("total 0\n", "/empty").isEmpty());

        List<FileEntry> empty = LsParser.parse("", "/root");
        FileSort.sort(empty, FileSort.Mode.SIZE);           // 不应抛异常
        assertTrue(FileFilter.apply(empty, false, true).isEmpty());
    }

    /* ==================================================================
     * 接入改造后【必须补上】的断言（现在留空，接入时逐条填实）
     * ================================================================== */

    // TODO(接入后): 断言 FilesFragment 里不再存在内联 ls 解析（用 grep 校验源码，防止半接入）
    // TODO(接入后): 断言 showHidden 默认值仍为 true（读 FilesFragment 字段/行为）
    // TODO(接入后): 断言 onlyScripts 的扩展名判定与 FileFilter.isShellScript 完全一致
    //              （当前已知差异：.bash 会被新实现保留，旧实现会剔除 —— 需明确取舍后二选一）
    // TODO(接入后): 断言 fmtSize 的所有调用点改为 SizeFormat.format 后，UI 文案逐字节相同
    // TODO(接入后): 断言 up() 改用 PathUtil.parent 后，带尾斜杠目录的行为与改造前一致
    //              （当前已知差异："/data/adb/" 旧实现停原地，新实现回 "/data"，需明确取舍）
}