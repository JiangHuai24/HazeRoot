package com.hazeroot.scriptrunner.core.fs

/**
 * 文件系统条目。
 *
 * 这是 HazeRoot 的 Kotlin 源码 —— 项目原作即 Kotlin，
 * 本次重构把 JADX 反编译出来的 Java 逐模块还原为 Kotlin。
 *
 * 职责：只做数据承载，不含任何 Android 依赖，可在 JVM 单测中直接构造。
 *
 * @property name       文件名（不含路径）
 * @property path       完整路径
 * @property isDir      是否目录
 * @property perm       权限串，如 drwxr-xr-x
 * @property size       字节数
 * @property linkTarget 软链目标；普通文件为 null
 */
data class FileEntryKt(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val perm: String,
    val size: Long,
    val linkTarget: String? = null
) {
    /** 点开头的隐藏文件 */
    val isHidden: Boolean
        get() = name.startsWith(".")

    /** 是否符号链接 */
    val isSymlink: Boolean
        get() = linkTarget != null

    /** 权限串是否表示目录 */
    val permIndicatesDir: Boolean
        get() = perm.isNotEmpty() && perm[0] == 'd'
}
