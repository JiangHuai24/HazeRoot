package com.hazeroot.scriptrunner.hazefs

import java.util.Locale

/**
 * 路径处理纯函数。
 *
 * 这些操作原本散落在 FilesFragment / MainActivity 各处（取父级、拼路径、取文件名），
 * 每处各写一份，且都是反编译的 Kotlin 内联调用（`StringsKt.substringBeforeLast` 之类）。
 */
object PathUtil {

    /** 取父目录；已在根返回 "/" */
    @JvmStatic
    fun parent(dir: String?): String {
        if (dir.isNullOrEmpty() || dir == "/") return "/"
        val d = if (dir.endsWith("/")) dir.substring(0, dir.length - 1) else dir
        if (d.isEmpty()) return "/"
        val i = d.lastIndexOf('/')
        return if (i <= 0) "/" else d.substring(0, i)
    }

    /** 取最后一段文件名；路径为空返回 "" */
    @JvmStatic
    fun fileName(path: String?): String {
        if (path.isNullOrEmpty()) return ""
        val p = if (path.endsWith("/")) path.substring(0, path.length - 1) else path
        val i = p.lastIndexOf('/')
        return if (i < 0) p else p.substring(i + 1)
    }

    /**
     * 拼接路径，自动补斜杠。
     *
     * ★ 修复：原实现只去掉**一个**尾斜杠，实测 `/data//x`、`/data/adb///x` 会残留。
     *   现在剥掉全部尾斜杠再拼，保证输出永远单斜杠。
     */
    @JvmStatic
    fun join(dir: String?, name: String?): String {
        if (dir.isNullOrEmpty()) return "/" + (name ?: "")
        if (name.isNullOrEmpty()) return dir.trimEnd('/').ifEmpty { "/" }

        val trimmedDir = dir.trimEnd('/')
        val trimmedName = name.trimStart('/')
        if (trimmedDir.isEmpty()) return "/$trimmedName"
        return "$trimmedDir/$trimmedName"
    }

    /** 是否根目录 */
    @JvmStatic
    fun isRoot(dir: String?): Boolean = dir == "/"

    /** 扩展名（不含点，小写）；无扩展名返回 "" */
    @JvmStatic
    fun extension(name: String?): String {
        if (name == null) return ""
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot >= name.length - 1) return ""
        return name.substring(dot + 1).lowercase(Locale.ROOT)
    }

    /** 是否压缩包（不应当作脚本执行） */
    @JvmStatic
    fun isArchive(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".tar.gz") ||
            n.endsWith(".tgz") || n.endsWith(".gz") || n.endsWith(".7z") ||
            n.endsWith(".rar") || n.endsWith(".xz") || n.endsWith(".bz2")
    }

    /**
     * 是否 APK。
     *
     * ★ 修复：原实现用 `endsWith(".apk")`，覆盖不到 split APK 的命名形态。
     *   旧实现（FilesFragment 的 isApkFile）用的是正则 `.*\.?apk(\.\d+)?`，
     *   能匹配 `base.apk`、`base.apk1`、`base.apk.1` 这些实际存在于
     *   Android 应用目录的名字。这里对齐旧实现，改用正则。
     */
    @JvmStatic
    fun isApk(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return APK_RE.matcher(n).matches()
    }

    /**
     * 是否文本文件（可用内置编辑器打开）。
     *
     * ★ 修复：`.sh` 必须排除。
     *   旧实现（FilesFragment 的 isTextFile）第一句就是 `.sh → false` ——
     *   脚本走「编辑 / 执行」专用面板，不该被当成纯文本直接编辑。
     *   TEXT_EXT 里保留了 "sh" 是为了让其他调用方（如搜索）也能识别脚本，
     *   但本方法明确排除它。
     */
    @JvmStatic
    fun isTextFile(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        if (n.endsWith(".sh") || n.endsWith(".bash")) return false
        return TEXT_EXT.any { n.endsWith(".$it") }
    }

    /** 是否视频 */
    @JvmStatic
    fun isVideo(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return VIDEO_EXT.any { n.endsWith(".$it") }
    }

    /** 是否音频 */
    @JvmStatic
    fun isAudio(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return AUDIO_EXT.any { n.endsWith(".$it") }
    }

    /** 是否图片 */
    @JvmStatic
    fun isImage(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return IMG_EXT.any { n.endsWith(".$it") }
    }

    /** 是否媒体文件（视频/音频/图片） */
    @JvmStatic
    fun isMedia(name: String?): Boolean = isVideo(name) || isAudio(name) || isImage(name)

    // ---- 扩展名集合（与原 FilesFragment 的常量保持一致）----
    private val TEXT_EXT = listOf(
        "txt", "md", "log", "json", "xml", "ini", "conf", "cfg", "properties",
        "yaml", "yml", "csv", "py", "js", "ts", "html", "htm", "css", "java",
        "c", "cpp", "h", "kt", "sql", "bat", "list", "prop", "sh"
    )
    private val VIDEO_EXT = listOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "m4v")
    private val AUDIO_EXT = listOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "amr", "wma")
    private val IMG_EXT = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    /**
     * APK 文件名正则。
     *
     * 需要覆盖这些真实存在的命名形态：
     * ```
     * base.apk        标准
     * base.apk1       某些管理器给重复文件加序号（无点）
     * base.apk.1      split APK 的分包编号（带点）
     * ```
     * 注意：旧实现的正则是 `.*\.?apk(\.\d+)?`，其中 `\.?` 可选导致
     * `apk` 后面直接跟数字（`base.apk1`）匹配不上 —— 这里修正为
     * `(\d+)?` 与 `(\.\d+)?` 两种后缀都接受。
     */
    private val APK_RE = java.util.regex.Pattern.compile(".*\\.?apk(\\d+|(\\.\\d+))?")
}
