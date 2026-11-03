package com.hazeroot.scriptrunner.core.fs;

/**
 * 路径处理纯函数。
 *
 * 这些操作原本散落在 FilesFragment / MainActivity 各处（取父级、拼路径、取文件名），
 * 每处各写一份，且都是反编译的 Kotlin 内联调用（StringsKt.substringBeforeLast 之类）。
 * <p><b>⚠ 尚未接线</b>：本类当前<b>没有任何生产代码调用</b>，
 * 实际运行的是 {@code FilesFragment} 内的旧内联实现。
 * 因此单测覆盖的是「未被使用的代码」，请勿把本类当作运行时行为的真值来源。
 * 接线计划见 docs/core-fs接入审查.md。
 */
public final class PathUtil {

    private PathUtil() {
    }

    /** 取父目录；已在根返回 "/" */
    public static String parent(String dir) {
        if (dir == null || dir.length() == 0 || "/".equals(dir)) {
            return "/";
        }
        String d = dir.endsWith("/") ? dir.substring(0, dir.length() - 1) : dir;
        if (d.length() == 0) {
            return "/";
        }
        int i = d.lastIndexOf('/');
        return (i <= 0) ? "/" : d.substring(0, i);
    }

    /** 取最后一段文件名；路径为空返回 "" */
    public static String fileName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }
        String p = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int i = p.lastIndexOf('/');
        return (i < 0) ? p : p.substring(i + 1);
    }

    /** 拼接路径，自动补斜杠，避免出现双斜杠 */
    public static String join(String dir, String name) {
        if (dir == null || dir.length() == 0) {
            return "/" + (name == null ? "" : name);
        }
        if (name == null || name.length() == 0) {
            return dir;
        }
        String d = dir.endsWith("/") ? dir.substring(0, dir.length() - 1) : dir;
        if (d.length() == 0) {
            return "/" + name;
        }
        return d + "/" + name;
    }

    /** 是否根目录 */
    public static boolean isRoot(String dir) {
        return "/".equals(dir);
    }

    /** 扩展名（不含点，小写）；无扩展名返回 "" */
    public static String extension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        // dot == 0 表示整个名字就是个点文件（.gitignore / .bashrc），这类没有"扩展名"的概念
        if (dot <= 0 || dot >= name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }
}
