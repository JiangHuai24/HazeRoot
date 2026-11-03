package com.hazeroot.scriptrunner.core.fs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旧实现参考副本 —— 本次改造的"行为基线裁判"。
 *
 * 逐段抄自改造前的 FilesFragment.java（refactor/layering @ 2bc208b），
 * 目的不是复用代码，而是给 JVM 单测提供一个与 core/fs 完全独立的旧行为实现，
 * 用来做逐项对比：一旦 core/fs 的语义和旧实现分了叉，等价性测试立刻红。
 *
 * 来源对照（FilesFragment.java 行号）：
 *   1. LS_RE 正则           L98
 *   2. ls 解析循环           L1894-L2005  (Pane.load$lambda$10)
 *   3. 过滤 + 排序           L2045-L2106
 *   4. fmtSize              L4839-L4850
 *   5. up() 取父目录         L1869
 *
 * 注意：这里刻意保留旧实现的所有怪癖（包括已知缺陷），
 * 因为等价性测试要比的是"旧行为"，不是"正确行为"。缺陷在测试里单独立项标注。
 */
public final class LegacyFsReference {

    /** 与 FilesFragment.java L98 完全一致 */
    private static final Pattern LS_RE = Pattern.compile(
            "^([-dlbcps][rwxsStT.-]{9})\\s+\\d+\\s+\\S+\\s+\\S+\\s+(\\d+)\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\s(.+)$");

    private LegacyFsReference() {
    }

    /* ===================== 1. ls -Al 解析 ===================== */

    /**
     * 旧解析逻辑。语义要点：
     *  - 逐行 Regex.find，不匹配直接跳过（total 行因此被丢掉）
     *  - target = 含 " -> " 时取第一个 " -> " 之后的 trim 结果，否则 null
     *  - name   = 第一个 " -> " 之前的 trim 结果（无箭头时就是整段 trim）
     *  - "." / ".." / 空名丢弃
     *  - path   = trimEnd(dir,'/') + "/" + name   ← 注意这里不做双斜杠归一
     *  - isDir  = 权限首字符 == 'd'
     *  - size   = toLongOrNull 失败按 0
     */
    public static List<FileEntry> parse(String lsOutput, String dir) {
        List<FileEntry> out = new ArrayList<FileEntry>();
        if (lsOutput == null) {
            return out;
        }
        String base = dir == null ? "/" : dir;
        for (String line : lsOutput.split("\n")) {
            Matcher m = LS_RE.matcher(line);
            if (!m.find()) {
                continue;
            }
            String perm = m.group(1);
            String sizeS = m.group(2);
            String rawName = m.group(3);
            int arrow = rawName.indexOf(" -> ");
            String target = null;
            String name;
            if (arrow >= 0) {
                name = rawName.substring(0, arrow).trim();
                target = rawName.substring(arrow + 4).trim();
            } else {
                name = rawName.trim();
            }
            if (".".equals(name) || "..".equals(name) || name.length() == 0) {
                continue;
            }
            boolean isDir = perm.charAt(0) == 'd';
            long size;
            try {
                size = Long.parseLong(sizeS);
            } catch (NumberFormatException ex) {
                size = 0L;   // Kotlin toLongOrNull 语义：溢出/非数字 → 0
            }
            out.add(new FileEntry(name, trimEnd(base, '/') + "/" + name, isDir, perm, size, target));
        }
        return out;
    }

    /* ===================== 2. 过滤 + 排序 ===================== */

    /**
     * 旧"先过滤后排序"流水线（FilesFragment L2045-L2106，就地修改链表后再 sortWith）。
     * 旧实现过滤用倒序索引 remove，null 元素因不满足 {@code instanceof FsEntry} 被 continue 保留。
     */
    public static List<FileEntry> filterAndSort(List<FileEntry> src, boolean showHidden,
                                                boolean onlyScripts, boolean bySize) {
        List<FileEntry> work = new ArrayList<FileEntry>(src);
        for (int i = work.size() - 1; i >= 0; i--) {
            FileEntry fe = work.get(i);
            if (fe == null) {
                continue;   // 旧实现：!(fo instanceof FsEntry) → continue，null 留在表里
            }
            boolean drop = false;
            if (!showHidden && fe.getName().startsWith(".")) {
                drop = true;
            }
            if (onlyScripts
                    && (fe.isDir() || !fe.getName().toLowerCase(Locale.ROOT).endsWith(".sh"))) {
                drop = true;
            }
            if (drop) {
                work.remove(i);
            }
        }
        if (work.size() >= 2) {
            Collections.sort(work, comparator(bySize));
        }
        return work;
    }

    /**
     * 旧比较器：compareByDescending(isDir) thenBy { bySize ? -size : name.toLowerCase }。
     * isDir 用 Boolean 自然序取反 → 目录在前；size 相等时落到名称（忽略大小写）升序。
     */
    public static Comparator<FileEntry> comparator(final boolean bySize) {
        return new Comparator<FileEntry>() {
            @Override
            public int compare(FileEntry t2, FileEntry t3) {
                int dirsFirst = Boolean.compare(t3.isDir(), t2.isDir());
                if (dirsFirst != 0) {
                    return dirsFirst;
                }
                if (bySize) {
                    long s1 = t2.getSize();
                    long s2 = t3.getSize();
                    if (s1 != s2) {
                        return s1 > s2 ? -1 : 1;
                    }
                }
                return t2.getName().toLowerCase(Locale.ROOT)
                        .compareTo(t3.getName().toLowerCase(Locale.ROOT));
            }
        };
    }

    /* ===================== 3. fmtSize ===================== */

    /**
     * 旧 fmtSize（L4839-L4850）逐行等价副本。
     * ★ 关键差异保留：旧实现用 String.format 且【不指定 Locale】→ 走默认 Locale。
     *   德语等区域下同一数值会渲染成 "77,8M"（逗号），而 core SizeFormat 固定 Locale.ROOT。
     */
    public static String fmtSize(long b) {
        if (b >= 1048576L) {
            return String.format("%.1fM", Float.valueOf(b / 1048576.0f));
        }
        if (b < 1024L) {
            return b + "B";
        }
        return String.format("%.1fK", Float.valueOf(b / 1024.0f));
    }

    /* ===================== 4. up() 取父目录 ===================== */

    /**
     * 旧 up()（L1869）：{@code substringBeforeLast(dir, '/', "")}，空则回落 "/"。
     * ★ 旧实现没有先 trimEnd 尾斜杠，所以 "/data/adb/" 会得到 "/data/adb"（原地不动），
     *   与 PathUtil.parent 的 "/data" 不一致。这是待改造时要确认的行为差。
     * 另外 dir == "/" 时旧实现直接 toast"已在根目录"，压根不进这段逻辑。
     */
    public static String up(String dir) {
        if (dir == null) {
            return "/";   // 旧实现此处 NPE（Intrinsics 判空），测试不覆盖
        }
        String parent = substringBeforeLast(dir, '/');
        return parent.length() == 0 ? "/" : parent;
    }

    private static String substringBeforeLast(String s, char delimiter) {
        int i = s.lastIndexOf(delimiter);
        return i < 0 ? "" : s.substring(0, i);
    }

    private static String trimEnd(String s, char c) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == c) {
            end--;
        }
        return s.substring(0, end);
    }
}