package com.hazeroot.scriptrunner.core.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 接入前「行为基线」测试 —— 在 FilesFragment 接入 core/fs 之前先把现有行为钉死。
 *
 * 设计原则：
 *  1. 每个断言都比对【两套实现】：core/fs（新）与 LegacyFsReference（旧实现逐行副本），
 *     而不是断言某个我拍脑袋写死的字符串。这样只要改造没改语义，测试就恒绿；
 *     改造一旦悄悄改了语义，立刻红。
 *  2. Ls 部分优先喂【本机真实执行出来的 ls 输出】，不是手搓字符串。
 *     真实输出顺带验证了 ls 的字段形态（链接数/owner/group/ISO 时间）确实能被正则吃下。
 *  3. 所有依赖本机环境的测试用 Assume 守卫：环境不具备时跳过而不是假阳性失败。
 *
 * 已知环境限制（本机无 Android 模拟器，也无管理员权限）：
 *  - Windows 上创建符号链接需要管理员权限，本机 ln -s / mklink / New-Item SymbolicLink 全部被拒。
 *    因此"真实符号链接"用例只能 Assume 跳过；符号链接语义本身由 toybox 真实样本用例覆盖，
 *    这更贴近 Android 现场（Android 的 ls 就是 toybox）。
 *  - Git for Windows 的 ls 默认输出 BSD 风格日期（"Nov  3 09:56"），
 *    解析器只认 ISO（yyyy-MM-dd HH:mm），所以必须显式 --time-style=long-iso。
 *    这一点本身就是回归风险：见 KnownDivergenceTest。
 */
public class BaselineEquivalenceTest {

    /** 本机 ls 的候选路径（Git for Windows 自带 GNU coreutils） */
    private static final String[] LS_CANDIDATES = {
            "C:\\Program Files\\Git\\usr\\bin\\ls.exe",
            "C:\\Program Files\\Git\\bin\\ls.exe",
            "C:\\Program Files (x86)\\Git\\usr\\bin\\ls.exe",
    };

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // ==================================================================
    // 一、LsParser：真实 ls 输出
    // ==================================================================

    /**
     * 风险：改造后行数/条目数悄悄变化（正则被"顺手优化"，或多吞少吞行）。
     * 真实数据来源：本机临时目录，内容由本测试自己建，数量/大小/类型全部可核。
     */
    @Test
    public void realLs_output_sameEntrySetAsLegacy() throws Exception {
        File dir = tmp.newFolder("real");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> now = LsParser.parse(out, dir.getAbsolutePath());
        List<FileEntry> old = LegacyFsReference.parse(out, dir.getAbsolutePath());

        assertEquals("新解析器与旧实现的条目数必须一致", old.size(), now.size());
        assertEquals(6, now.size());   // alpha.sh beta.sh notes.txt sub "dir with space" .hidden

        for (int i = 0; i < old.size(); i++) {
            assertSameEntry(old.get(i), now.get(i), "第 " + i + " 条");
        }
    }

    /**
     * 风险：isDir 判定来源被改（例如改成看名字后缀/看 stat），导致点进目录的行为变化。
     * 断言来源是权限首字符，并且与真实文件系统事实一致。
     */
    @Test
    public void realLs_isDir_matchesActualFileSystem() throws Exception {
        File dir = tmp.newFolder("dirs");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> now = LsParser.parse(out, dir.getAbsolutePath());

        FileEntry sub = find(now, "sub");
        assertNotNull(sub);
        assertTrue("sub 是真实目录", sub.isDir());
        assertEquals("d", sub.getPerm().substring(0, 1));

        FileEntry sh = find(now, "alpha.sh");
        assertNotNull(sh);
        assertFalse("alpha.sh 是真实文件", sh.isDir());
        assertFalse(sh.permIndicatesDir());
        assertEquals("-", sh.getPerm().substring(0, 1));
    }

    /**
     * 风险：size 被解析成 0 或串到别的字段，UI 上文件大小显示错（用户直接可见）。
     * 断言与真实字节数一致，不是断言"非 0"。
     */
    @Test
    public void realLs_size_matchesActualByteCount() throws Exception {
        File dir = tmp.newFolder("sizes");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> now = LsParser.parse(out, dir.getAbsolutePath());

        assertEquals("alpha.sh 内容 100 字节", 100L, find(now, "alpha.sh").getSize());
        assertEquals("beta.sh 内容 5000 字节", 5000L, find(now, "beta.sh").getSize());
        assertEquals("notes.txt 内容 10 字节", 10L, find(now, "notes.txt").getSize());
        assertEquals("空隐藏文件 0 字节", 0L, find(now, ".hidden").getSize());
    }

    /**
     * 风险：文件名含空格时被切成两半（正则 group3 是贪婪 (.+)，理应保留整段）。
     * 这条在真实输出上验证，因为 ls 的分隔由制表/多空格决定，手搓字符串容易假绿。
     */
    @Test
    public void realLs_nameWithSpaces_keptWhole() throws Exception {
        File dir = tmp.newFolder("spaces");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> now = LsParser.parse(out, dir.getAbsolutePath());

        FileEntry spaced = find(now, "dir with space");
        assertNotNull("含空格的目录名必须整段保留", spaced);
        assertTrue(spaced.isDir());
        assertEquals(dir.getAbsolutePath() + "/dir with space", spaced.getPath());
    }

    /**
     * 风险：真实路径拼接出双斜杠（"//name"），后续 Engine.q() 拼命令时可能被 shell 误解析。
     */
    @Test
    public void realLs_paths_haveNoDoubleSlash() throws Exception {
        File dir = tmp.newFolder("paths");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        for (FileEntry e : LsParser.parse(out, dir.getAbsolutePath())) {
            assertFalse("path 不应含双斜杠: " + e.getPath(), e.getPath().contains("//"));
        }
    }

    /**
     * 风险：真实输出里出现解析不了的行（例如 BSD 日期、5 位年份）→ 整目录显示为空。
     * 这条同时是环境自检：确认本机 ls 在 --time-style=long-iso 下确实全部可解析。
     */
    @Test
    public void realLs_everyNonTotalLine_isParsed() throws Exception {
        File dir = tmp.newFolder("strict");
        makeRealFixture(dir);

        String out = realLs(dir.getAbsolutePath());
        List<String> body = new ArrayList<String>();
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.length() == 0 || t.startsWith("total")) {
                continue;
            }
            body.add(line);
        }
        assertEquals("除 total 行外的每一行都必须解析成功", body.size(),
                LsParser.parse(out, dir.getAbsolutePath()).size());
    }

    // ==================================================================
    // 二、LsParser：符号链接语义（Android toybox 真实形态）
    // ==================================================================

    /**
     * 风险：符号链接被当成普通文件、或 target 被截断，用户点链接跳到错地方。
     * 样本取自真机 /data/adb 的实际输出（toybox，权限位首字符 l，iso 日期）。
     */
    @Test
    public void symlink_toyboxSample_targetExtractedLikeLegacy() {
        String out = "lrw-r--r--   1 root   root        30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys\n"
                + "drwxr-xr-x  84 root   root      1700 1970-01-14 05:59 apex\n";

        List<FileEntry> now = LsParser.parse(out, "/data/adb");
        List<FileEntry> old = LegacyFsReference.parse(out, "/data/adb");
        assertEquals(old.size(), now.size());
        assertSameEntry(old.get(0), now.get(0), "symlink");
        assertSameEntry(old.get(1), now.get(1), "目录");

        FileEntry link = now.get(0);
        assertEquals("adb_keys", link.getName());
        assertEquals("/product/etc/security/adb_keys", link.getLinkTarget());
        assertTrue(link.isSymlink());
        assertFalse("权限首字符 l 不是目录", link.isDir());
        assertTrue(link.getPerm().startsWith("l"));
    }

    /**
     * 风险（畸形）：名字本身含 " -> "，解析器按第一个 " -> " 切断，
     * 把名字后半段当成链接目标 —— 名字丢了、target 是垃圾。
     * 这是旧实现的真实缺陷，core/fs 逐字复刻了它。此处【钉住现状】而不是偷偷修掉：
     * 接入改造时若要修，必须单独一个 commit 并同步改这里，否则属于"悄悄改行为"。
     */
    @Test
    public void malformed_nameContainingArrow_sameAsLegacy_andPinsKnownFlaw() {
        String out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 a -> b\n";

        List<FileEntry> now = LsParser.parse(out, "/data/adb");
        List<FileEntry> old = LegacyFsReference.parse(out, "/data/adb");
        assertEquals(1, now.size());
        assertSameEntry(old.get(0), now.get(0), "畸形箭头");

        // 现状（缺陷）：真实名字 "a -> b" 被切成 name="a"、target="b"
        assertEquals("a", now.get(0).getName());
        assertEquals("b", now.get(0).getLinkTarget());
        assertTrue("被误判成符号链接", now.get(0).isSymlink());
        assertEquals("/data/adb/a", now.get(0).getPath());
    }

    /**
     * 风险（畸形）：target 为空（"link -> "）。旧实现给空串而不是 null，
     * 于是 isSymlink() 仍为 true。钉住，避免接入后被"优化"成 null 而改变 UI 判断分支。
     */
    @Test
    public void malformed_emptyTarget_keepsEmptyStringNotNull() {
        String out = "lrwxrwxrwx 1 root root 5 2024-01-01 00:00 link -> \n";

        List<FileEntry> now = LsParser.parse(out, "/x");
        List<FileEntry> old = LegacyFsReference.parse(out, "/x");
        assertSameEntry(old.get(0), now.get(0), "空 target");

        assertEquals("", now.get(0).getLinkTarget());
        assertNotNull("旧实现是空串不是 null", now.get(0).getLinkTarget());
        assertTrue(now.get(0).isSymlink());
    }

    /**
     * 风险：真实符号链接无法在本机创建（需管理员权限）。
     * 这里显式尝试并 Assume：能跑就跑，跑不了就明确跳过，绝不假绿。
     */
    @Test
    public void realSymlink_roundTrip_ifHostAllows() throws Exception {
        File dir = tmp.newFolder("link");
        File target = new File(dir, "target.txt");
        Files.write(target.toPath(), "hello".getBytes(StandardCharsets.UTF_8));
        File link = new File(dir, "link.txt");

        boolean created;
        try {
            Files.createSymbolicLink(link.toPath(), target.toPath());
            created = true;
        } catch (Exception e) {
            created = false;
        }
        Assume.assumeTrue("本机无管理员权限，无法创建真实符号链接（预期跳过）", created);

        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> now = LsParser.parse(out, dir.getAbsolutePath());
        FileEntry parsed = find(now, "link.txt");
        assertNotNull(parsed);
        assertTrue("真实符号链接必须被识别", parsed.isSymlink());
        assertNotNull(parsed.getLinkTarget());
    }

    // ==================================================================
    // 三、FileSort：与旧 comparator 顺序完全一致
    // ==================================================================

    /**
     * 风险：目录优先规则丢失 / 名称大小写敏感性变化 → 列表顺序和用户习惯不一样。
     * 断言两个序列【逐位相同】，而不是"包含这些元素"。
     */
    @Test
    public void sort_nameMode_exactSameOrderAsLegacy() {
        List<FileEntry> src = sortSample();

        List<FileEntry> now = new ArrayList<FileEntry>(src);
        FileSort.sort(now, FileSort.Mode.NAME);
        List<FileEntry> old = LegacyFsReference.filterAndSort(src, true, false, false);

        assertSameNameOrder(old, now, "NAME 模式");
    }

    /**
     * 风险：大小降序被改成升序，或同大小文件不再回落名称 → 排序不稳、刷新后顺序漂移。
     */
    @Test
    public void sort_sizeMode_exactSameOrderAsLegacy() {
        List<FileEntry> src = sortSample();

        List<FileEntry> now = new ArrayList<FileEntry>(src);
        FileSort.sort(now, FileSort.Mode.SIZE);
        List<FileEntry> old = LegacyFsReference.filterAndSort(src, true, false, true);

        assertSameNameOrder(old, now, "SIZE 模式");
    }

    /**
     * 风险：相等键（同名/同大小）时排序不稳定，两次刷新顺序不同。
     * 用一批完全同 size 的文件验证：结果必须与旧实现一致（名称升序）。
     */
    @Test
    public void sort_equalKeys_stableAndSameAsLegacy() {
        List<FileEntry> src = new ArrayList<FileEntry>();
        src.add(file("c.sh", 100));
        src.add(file("A.sh", 100));
        src.add(file("b.sh", 100));
        src.add(file(".sh", 100));

        List<FileEntry> now = new ArrayList<FileEntry>(src);
        FileSort.sort(now, FileSort.Mode.SIZE);
        List<FileEntry> old = LegacyFsReference.filterAndSort(src, true, false, true);

        assertSameNameOrder(old, now, "同 size");
        assertEquals("大小相同 → 名称升序（忽略大小写）", ".sh", now.get(0).getName());
        assertEquals("A.sh", now.get(1).getName());
        assertEquals("b.sh", now.get(2).getName());
        assertEquals("c.sh", now.get(3).getName());
    }

    /**
     * 风险：真实目录列表排序与旧实现不一致（真实数据里大小写混合、目录/文件混杂）。
     */
    @Test
    public void sort_onRealLsEntries_exactSameOrderAsLegacy() throws Exception {
        File dir = tmp.newFolder("sortreal");
        makeRealFixture(dir);
        String out = realLs(dir.getAbsolutePath());
        List<FileEntry> parsed = LsParser.parse(out, dir.getAbsolutePath());

        List<FileEntry> now = new ArrayList<FileEntry>(parsed);
        FileSort.sort(now, FileSort.Mode.NAME);
        List<FileEntry> old = LegacyFsReference.filterAndSort(parsed, true, false, false);

        assertSameNameOrder(old, now, "真实数据 NAME");
    }

    // ==================================================================
    // 四、FileFilter：隐藏 + 只留 .sh 的边界
    // ==================================================================

    /** 风险：显示隐藏文件开关语义反转（默认 true 被改成 false）→ 用户以为文件丢了。 */
    @Test
    public void filter_showHiddenTrue_keepsEverythingLikeLegacy() {
        List<FileEntry> src = filterSample();

        List<FileEntry> now = FileFilter.apply(src, true, false);
        List<FileEntry> old = LegacyFsReference.filterOnly(src, true, false);

        assertEquals("showHidden=true 不应丢任何条目", src.size(), now.size());
        assertSameNameOrder(old, now, "全保留");
    }

    /** 风险：隐藏判断改成看路径而不是看名字 → 点文件漏过滤。 */
    @Test
    public void filter_showHiddenFalse_dropsOnlyDotNames() {
        List<FileEntry> src = filterSample();

        List<FileEntry> now = FileFilter.apply(src, false, false);
        List<FileEntry> old = LegacyFsReference.filterOnly(src, false, false);

        assertSameNameOrder(old, now, "去隐藏");
        for (FileEntry e : now) {
            assertFalse("过滤后不应出现点文件: " + e.getName(), e.getName().startsWith("."));
        }
        assertEquals(4, now.size());
    }

    /** 风险：onlyScripts 把目录也留下，或大小写敏感（.SH 漏掉）→ 脚本页显示错。 */
    @Test
    public void filter_onlyScripts_keepsShFilesAndDropsDirs() {
        List<FileEntry> src = filterSample();

        List<FileEntry> now = FileFilter.apply(src, true, true);
        assertEquals(2, now.size());
        assertEquals("run.sh", now.get(0).getName());
        assertEquals("NEW.SH", now.get(1).getName());
        for (FileEntry e : now) {
            assertFalse("onlyScripts 模式下目录必须全丢", e.isDir());
            assertTrue(e.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".sh"));
        }
    }

    /** 风险：两个开关叠加时少过滤或多过滤（用户最常开的组合）。 */
    @Test
    public void filter_bothSwitches_intersectCorrectly() {
        List<FileEntry> src = filterSample();

        List<FileEntry> now = FileFilter.apply(src, false, true);
        // showHidden=false 只剔掉点文件；.nomedia 已不是 .sh 早已被 onlyScripts 剔除，
        // 所以剩下 run.sh 与 NEW.SH（非隐藏的 .SH 大写脚本）
        assertEquals(2, now.size());
        assertEquals("run.sh", now.get(0).getName());
        assertEquals("NEW.SH", now.get(1).getName());
    }

    /**
     * 风险（重要，会导致可见行为变化）：.bash 文件。
     * 旧 FilesFragment 的 onlyScripts 判定是 endsWith(".sh")；
     * core FileFilter.isShellScript 额外接受 .bash。
     * 直接换用 FileFilter → "只显示脚本"页会多出 .bash 文件，用户可见的变化。
     * 此处把它当【待决策差异】断言出来，接入时必须有明确结论。
     */
    @Test
    public void filter_bashExtension_isKnownDivergence_legacyWouldDrop() {
        List<FileEntry> src = new ArrayList<FileEntry>();
        src.add(file("a.bash", 10));
        src.add(file("b.sh", 10));

        List<FileEntry> now = FileFilter.apply(src, true, true);
        List<FileEntry> old = LegacyFsReference.filterOnly(src, true, true);

        assertEquals("新：.bash 也算脚本", 2, now.size());
        assertEquals("旧：只有 .sh", 1, old.size());
        assertEquals("b.sh", old.get(0).getName());
    }

    /** 风险：null / 空列表被 NPE（旧实现遍历时会 NPE，新实现必须安全）。 */
    @Test
    public void filter_nullAndEmpty_doNotThrow() {
        assertTrue(FileFilter.apply(null, true, false).isEmpty());
        assertTrue(FileFilter.apply(new ArrayList<FileEntry>(), true, true).isEmpty());
    }

    // ==================================================================
    // 五、SizeFormat：与 fmtSize 当前输出完全一致（UI 直接显示这个字符串）
    // ==================================================================

    /**
     * 风险：列表右侧的大小文字变了。这是用户一眼能看出的回归，必须逐个比对旧 fmtSize。
     * 覆盖 0 / 小于 1K / K / M / 超过 1G / 极端值。
     */
    @Test
    public void sizeFormat_matchesFmtSize_forEveryBoundary() {
        long[] values = {
                0L, 1L, 100L, 257L, 512L, 1023L,
                1024L, 1025L, 1536L, 3481L, 100000L, 1048575L,
                1048576L, 1048577L, 1572864L, 81579212L, 43830476L,
                1073741824L, 2147483648L, Long.MAX_VALUE,
                -1L, -1024L,
        };
        for (long v : values) {
            assertEquals("size=" + v + " 的显示必须与旧 fmtSize 一致",
                    LegacyFsReference.fmtSize(v), SizeFormat.format(v));
        }
    }

    /** 风险：Locale 差异。旧 fmtSize 不指定 Locale（跟随系统），新 SizeFormat 固定 ROOT。 */
    @Test
    public void sizeFormat_isLocaleIndependent_whileLegacyFmtSizeIsNot() {
        java.util.Locale saved = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("de-DE"));
            assertEquals("新实现不受 Locale 影响", "77.8M", SizeFormat.format(81579212L));
            // 旧实现会渲染成逗号小数点（这就是"换 locale 后 UI 变了"的根因）
            assertEquals("77,8M", LegacyFsReference.fmtSize(81579212L));
        } finally {
            java.util.Locale.setDefault(saved);
        }
    }

    // ==================================================================
    // 六、PathUtil：路径边界
    // ==================================================================

    /** 风险：上一级按钮点错地方（尾斜杠场景是真实会发生的，因为 ls 输出的 dir 可能带 /）。 */
    @Test
    public void pathUtil_parent_rootAndNormal_cases() {
        assertEquals("/", PathUtil.parent("/"));
        // 注意："/data" 的父级是 "/"，不是 "/data"（单段路径取父必然回到根）
        assertEquals("/", PathUtil.parent("/data"));
        assertEquals("/data", PathUtil.parent("/data/adb"));
    }

    /**
     * 风险（重要差异）：尾斜杠。旧 up() 不做 trimEnd，"/data/adb/" 会停原地；
     * PathUtil.parent 先 trimEnd 再取父，得到 "/data"。
     * 接入时换成 PathUtil.parent 会让"上一级"在带尾斜杠时多跳一层 —— 必须确认。
     */
    @Test
    public void pathUtil_parent_trailingSlash_differsFromLegacyUp_pinned() {
        assertEquals("/data", PathUtil.parent("/data/adb/"));
        assertEquals("/data/adb", LegacyFsReference.up("/data/adb/"));
        assertEquals("带尾斜杠时新旧不一致，属待决策项",
                LegacyFsReference.up("/data/adb/"), "/data/adb");
    }

    /** 风险：重复斜杠。旧拼接 trimEnd 后只补一个 /，PathUtil.join("//") 仍会产生双斜杠。 */
    @Test
    public void pathUtil_doubleSlash_behaviourPinned() {
        assertEquals("/", PathUtil.parent("//"));
        assertEquals("", PathUtil.fileName("//"));
        // 已知弱点：dir="//" 时 join 仍产出双斜杠（钉住，供接入时评估）
        assertEquals("//x", PathUtil.join("//", "x"));
    }

    /** 风险：LsParser 对 dir="//" 的拼接与旧实现不同（旧实现会归一成 /x，新实现给 //x）。 */
    @Test
    public void lsParser_doubleSlashDir_differsFromLegacy_pinned() {
        String out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n";
        assertEquals("/sub", LegacyFsReference.parse(out, "//").get(0).getPath());
        assertEquals("//sub", LsParser.parse(out, "//").get(0).getPath());
    }

    /** 风险：空串/null 目录导致 NPE 或路径缺前导斜杠。 */
    @Test
    public void pathUtil_emptyAndNull_areSafe() {
        assertEquals("/", PathUtil.parent(null));
        assertEquals("/", PathUtil.parent(""));
        assertEquals("", PathUtil.fileName(null));
        assertEquals("", PathUtil.fileName(""));
        assertEquals("/x", PathUtil.join("", "x"));
        assertEquals("/data/adb", PathUtil.join("/data/adb", null));
        assertTrue(PathUtil.isRoot("/"));
        assertFalse(PathUtil.isRoot(null));
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** 建一批"事实可核"的真实文件：大小/类型/空格名/隐藏名都覆盖 */
    private static void makeRealFixture(File dir) throws Exception {
        writeFile(dir, "alpha.sh", 100);
        writeFile(dir, "beta.sh", 5000);
        writeFile(dir, "notes.txt", 10);
        writeFile(dir, ".hidden", 0);
        assertTrue(new File(dir, "sub").mkdir());
        assertTrue(new File(dir, "dir with space").mkdir());
    }

    private static void writeFile(File dir, String name, int bytes) throws Exception {
        byte[] buf = new byte[bytes];
        Arrays.fill(buf, (byte) 'x');
        Files.write(new File(dir, name).toPath(), buf);
    }

    /** 执行本机真实 ls（ISO 时间格式，否则解析器一条都吃不下） */
    private static String realLs(String dir) throws Exception {
        String ls = findLs();
        Assume.assumeTrue("本机没有可用的 ls（Git for Windows），跳过真实输出用例", ls != null);

        ProcessBuilder pb = new ProcessBuilder(ls, "--time-style=long-iso", "-Al", dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        r.close();
        int code = p.waitFor();
        Assume.assumeTrue("ls 执行失败（exit " + code + "）：" + sb, code == 0);
        return sb.toString();
    }

    private static String findLs() {
        for (String p : LS_CANDIDATES) {
            if (new File(p).isFile()) {
                return p;
            }
        }
        return null;
    }

    private static List<FileEntry> sortSample() {
        List<FileEntry> l = new ArrayList<FileEntry>();
        l.add(file("b.sh", 100));
        l.add(dir("Zeta"));
        l.add(file("a.sh", 500));
        l.add(dir("alpha"));
        l.add(file("c.sh", 20));
        l.add(file("A.sh", 500));
        return l;
    }

    private static List<FileEntry> filterSample() {
        List<FileEntry> l = new ArrayList<FileEntry>();
        l.add(dir("GJX"));
        l.add(dir(".hidden_dir"));
        l.add(file("run.sh", 100));
        l.add(file("NEW.SH", 100));
        l.add(file("notes.txt", 100));
        l.add(file(".nomedia", 0));
        return l;
    }

    private static FileEntry dir(String name) {
        return new FileEntry(name, "/x/" + name, true, "drwxr-xr-x", 0, null);
    }

    private static FileEntry file(String name, long size) {
        return new FileEntry(name, "/x/" + name, false, "-rw-rw-r--", size, null);
    }

    private static FileEntry find(List<FileEntry> list, String name) {
        for (FileEntry e : list) {
            if (name.equals(e.getName())) {
                return e;
            }
        }
        return null;
        }

    /** 逐字段比对两个条目（name/path/isDir/perm/size/linkTarget 全覆盖） */
    private static void assertSameEntry(FileEntry old, FileEntry now, String label) {
        assertNotNull(label + " 旧实现为空", old);
        assertNotNull(label + " 新实现为空", now);
        assertEquals(label + " name", old.getName(), now.getName());
        assertEquals(label + " path", old.getPath(), now.getPath());
        assertEquals(label + " isDir", old.isDir(), now.isDir());
        assertEquals(label + " perm", old.getPerm(), now.getPerm());
        assertEquals(label + " size", old.getSize(), now.getSize());
        assertEquals(label + " linkTarget", old.getLinkTarget(), now.getLinkTarget());
        assertEquals(label + " isSymlink", old.isSymlink(), now.isSymlink());
    }

    /** 逐位比对名称序列，顺序必须完全相同 */
    private static void assertSameNameOrder(List<FileEntry> old, List<FileEntry> now, String label) {
        assertEquals(label + " 长度", old.size(), now.size());
        for (int i = 0; i < old.size(); i++) {
            assertEquals(label + " 第 " + i + " 位顺序必须一致",
                    old.get(i).getName(), now.get(i).getName());
        }
    }
}