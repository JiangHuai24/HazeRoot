package com.hazeroot.scriptrunner.core.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文件列表的显示过滤。
 *
 * 两种过滤（原实现在 FilesFragment 的 load 成功回调里内联）：
 *  1. {@link #filterHidden} —— 是否隐藏点开头文件
 *     ★ 注意默认语义：现有命令是 {@code ls -Al}，点文件本来就是列出来的，
 *       所以"显示隐藏文件"开关的默认值必须是 true，否则会改变现有行为。
 *  2. {@link #filterScriptsOnly} —— 只显示 .sh 脚本
 *     HazeRoot 是脚本执行器，找 .sh 是文件页的主要目的。
 */
public final class FileFilter {

    private FileFilter() {
    }

    /**
     * 应用过滤。
     *
     * @param list        输入列表（不会被修改）
     * @param showHidden  true = 保留点文件；false = 去掉点文件
     * @param scriptsOnly true = 只保留 .sh 文件（目录一律去掉）
     * @return 新的过滤后列表
     */
    public static List<FileEntry> apply(List<FileEntry> list, boolean showHidden, boolean scriptsOnly) {
        List<FileEntry> out = new ArrayList<>();
        if (list == null) {
            return out;
        }
        for (FileEntry e : list) {
            if (e == null) {
                continue;
            }
            if (!showHidden && e.isHidden()) {
                continue;
            }
            if (scriptsOnly) {
                if (e.isDir()) {
                    continue;
                }
                if (!isShellScript(e.getName())) {
                    continue;
                }
            }
            out.add(e);
        }
        return out;
    }

    /** 是否 .sh / .bash 脚本 */
    public static boolean isShellScript(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".sh") || n.endsWith(".bash");
    }
}
