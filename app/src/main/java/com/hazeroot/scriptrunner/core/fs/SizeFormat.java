package com.hazeroot.scriptrunner.core.fs;

import java.util.Locale;

/**
 * 文件大小格式化。
 *
 * 严格复刻原 fmtSize：
 *  <pre>
 *  >= 1MB  -> "%.1fM"  （例 77.8M）
 *  < 1KB   -> "&lt;n&gt;B"    （例 257B）
 *  其它    -> "%.1fK"  （例 3.4K）
 *  </pre>
 * 注意：原实现没有 GB 档，超过 1024MB 仍显示为 M（如 2048.0M）。
 * 这里保持原行为不变 —— 重构的原则是等价，不是改行为。
 */
public final class SizeFormat {

    private SizeFormat() {
    }

    public static String format(long bytes) {
        if (bytes >= 1048576L) {
            return String.format(Locale.ROOT, "%.1fM", bytes / 1048576.0f);
        }
        if (bytes < 1024L) {
            return bytes + "B";
        }
        return String.format(Locale.ROOT, "%.1fK", bytes / 1024.0f);
    }
}
