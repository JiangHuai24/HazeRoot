package com.hazeroot.scriptrunner.core.fs;

/**
 * 文件系统条目（纯数据类，不依赖 Android）。
 *
 * 从 FilesFragment.FsEntry 抽出。抽出的意义：
 *  - 可以在 JVM 单元测试里直接构造，不需要 Android 环境
 *  - 解析器（LsParser）、排序（FileSort）、过滤（FileFilter）都只依赖它
 */
public final class FileEntry {

    private final String name;
    private final String path;
    private final boolean isDir;
    private final String perm;
    private final long size;
    private final String linkTarget;

    public FileEntry(String name, String path, boolean isDir, String perm, long size, String linkTarget) {
        this.name = name == null ? "" : name;
        this.path = path == null ? "" : path;
        this.isDir = isDir;
        this.perm = perm == null ? "" : perm;
        this.size = size;
        this.linkTarget = linkTarget;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public boolean isDir() {
        return isDir;
    }

    public String getPerm() {
        return perm;
    }

    public long getSize() {
        return size;
    }

    /** 符号链接指向的目标；普通文件为 null */
    public String getLinkTarget() {
        return linkTarget;
    }

    public boolean isSymlink() {
        return linkTarget != null;
    }

    /** 是否点开头的隐藏文件 */
    public boolean isHidden() {
        return name.startsWith(".");
    }

    /** 权限串是否表示目录（第一位是 'd'） */
    public boolean permIndicatesDir() {
        return perm.length() > 0 && perm.charAt(0) == 'd';
    }

    @Override
    public String toString() {
        return "FileEntry{" + name + ", dir=" + isDir + ", size=" + size + "}";
    }
}
