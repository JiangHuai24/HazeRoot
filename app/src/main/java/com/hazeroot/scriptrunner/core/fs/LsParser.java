package com.hazeroot.scriptrunner.core.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 {@code ls -Al} 的输出为 {@link FileEntry} 列表。
 *
 * 严格复刻原 FilesFragment 里的实现，行为完全一致：
 *  - 正则：{@code ^([-dlbcps][rwxsStT.-]{9})\s+\d+\s+\S+\s+\S+\s+(\d+)\s+\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}\s(.+)$}
 *    分组 1 = 权限串，2 = 大小，3 = 文件名（可能带 " -> 目标"）
 *  - 名字为 "." 或 ".." 时丢弃
 *  - 名字为空时丢弃
 *  - " -> " 之后视为软链目标；名字取 " -> " 之前并 trim
 *  - 是否目录取自权限串首位是否为 'd'
 *  - 大小解析失败按 0 处理
 *
 * 为什么抽出来：这段逻辑原本内联在 4858 行的 FilesFragment 里，
 * 且是 JADX 还原的 while 循环 + goto 风格，完全无法测试。抽出后可单测。
 */
public final class LsParser {

    /**
     * 一行 {@code ls -Al} 的形态：
     * <pre>
     * drwxr-xr-x  2 root root 4096 2024-01-01 00:00 somedir
     * lrw-r--r--  1 root root   30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys
     * </pre>
     * 注意 toybox 的日期恒为 ISO 格式、owner/group 不含空格。
     */
    private static final Pattern LS_RE = Pattern.compile(
            "^([-dlbcps][rwxsStT.-]{9})\\s+\\d+\\s+\\S+\\s+\\S+\\s+(\\d+)\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\s(.+)$");

    private LsParser() {
    }

    /**
     * 解析整个 ls 输出。
     *
     * @param lsOutput    {@code ls -Al} 的原始输出，可为 null
     * @param dir         这些条目所在的父目录（用于拼出完整 path）
     * @return 永不为 null 的列表
     */
    public static List<FileEntry> parse(String lsOutput, String dir) {
        List<FileEntry> out = new ArrayList<>();
        if (lsOutput == null || lsOutput.length() == 0) {
            return out;
        }
        String base = dir == null ? "/" : dir;
        if (base.length() > 0 && !base.endsWith("/")) {
            base = base + "/";
        } else if (base.length() == 0) {
            base = "/";
        }

        for (String line : lsOutput.split("\n")) {
            FileEntry e = parseLine(line, base);
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    /**
     * 解析单行。
     *
     * @param line 一行 ls 输出
     * @param base 已带结尾斜杠的父目录前缀
     * @return 解析失败 / 该跳过时返回 null
     */
    static FileEntry parseLine(String line, String base) {
        if (line == null) {
            return null;
        }
        Matcher m = LS_RE.matcher(line);
        if (!m.find()) {
            return null;
        }
        String perm = m.group(1);
        String sizeS = m.group(2);
        String rawName = m.group(3);
        if (perm == null || rawName == null) {
            return null;
        }

        String target = null;
        String name;
        int arrow = rawName.indexOf(" -> ");
        if (arrow >= 0) {
            name = rawName.substring(0, arrow).trim();
            target = rawName.substring(arrow + 4).trim();
        } else {
            name = rawName.trim();
        }

        // "." 与 ".." 不列出来（虽然 -A 不会输出它们，但防御）
        if (".".equals(name) || "..".equals(name) || name.length() == 0) {
            return null;
        }

        boolean isDir = perm.charAt(0) == 'd';
        long size;
        try {
            size = Long.parseLong(sizeS);
        } catch (NumberFormatException ex) {
            size = 0L;
        }
        return new FileEntry(name, base + name, isDir, perm, size, target);
    }
}
