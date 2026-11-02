package com.hazeroot.scriptrunner.core.fs;

import java.util.Comparator;
import java.util.List;

/**
 * 文件列表排序策略。
 *
 * 复刻原实现的两条规则：
 *  1. **目录优先**（无论什么排序方式，目录永远在文件前面）
 *  2. 同一类别内按用户选择：名称升序（忽略大小写）/ 大小降序
 *
 * 原实现是 CollectionsKt.sortWith + 嵌套匿名 Comparator（compareByDescending + thenBy），
 * 反编译后非常难读。这里用显式 Comparator 重写，行为等价且可单测。
 */
public final class FileSort {

    /** 排序模式 */
    public enum Mode {
        /** 按名称，忽略大小写升序 */
        NAME,
        /** 按大小，大的在前 */
        SIZE
    }

    private FileSort() {
    }

    /**
     * 构造比较器。
     *
     * @param mode      排序模式，null 视为 NAME
     * @param dirsFirst 是否目录优先（原实现恒为 true）
     */
    public static Comparator<FileEntry> comparator(final Mode mode, final boolean dirsFirst) {
        final Mode m = (mode == null) ? Mode.NAME : mode;
        return new Comparator<FileEntry>() {
            @Override
            public int compare(FileEntry a, FileEntry b) {
                if (a == null && b == null) {
                    return 0;
                }
                if (a == null) {
                    return 1;
                }
                if (b == null) {
                    return -1;
                }
                if (dirsFirst && a.isDir() != b.isDir()) {
                    return a.isDir() ? -1 : 1;
                }
                if (m == Mode.SIZE) {
                    long sa = a.getSize();
                    long sb = b.getSize();
                    if (sa != sb) {
                        return sa > sb ? -1 : 1;   // 大的在前
                    }
                    // 大小相同则退回名称，保证结果稳定
                }
                String na = a.getName().toLowerCase(java.util.Locale.ROOT);
                String nb = b.getName().toLowerCase(java.util.Locale.ROOT);
                return na.compareTo(nb);
            }
        };
    }

    /** 就地排序 */
    public static void sort(List<FileEntry> list, Mode mode) {
        if (list == null || list.size() < 2) {
            return;
        }
        java.util.Collections.sort(list, comparator(mode, true));
    }
}
