package com.hazeroot.scriptrunner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import kotlin.TuplesKt;
import kotlin.jvm.functions.Function1;
import kotlin.text.StringsKt;

/** 反编译桥：Kotlin stdlib $default 合成方法的 Java 等价实现（仅供重建工程内部使用） */
public final class Kt {
    private Kt() {}

    private static int indexOfIgnoreCase(CharSequence s, CharSequence needle, int from) {
        String a = s.toString();
        String b = needle.toString();
        if (b.isEmpty()) return Math.max(0, Math.min(from, a.length()));
        return a.toLowerCase(Locale.ROOT).indexOf(b.toLowerCase(Locale.ROOT), from);
    }

    public static boolean contains(CharSequence s, CharSequence needle, boolean ignoreCase, int mask, Object o) {
        if (!ignoreCase) return s.toString().contains(needle);
        return indexOfIgnoreCase(s, needle, 0) >= 0;
    }

    public static boolean contains(CharSequence s, char needle, boolean ignoreCase, int mask, Object o) {
        return contains(s, String.valueOf(needle), ignoreCase, mask, o);
    }

    public static boolean endsWith(CharSequence s, CharSequence suffix, boolean ignoreCase, int mask, Object o) {
        String a = s.toString();
        String b = suffix.toString();
        if (!ignoreCase) return a.endsWith(b);
        if (b.length() > a.length()) return false;
        return a.regionMatches(true, a.length() - b.length(), b, 0, b.length());
    }

    public static boolean startsWith(CharSequence s, CharSequence prefix, boolean ignoreCase, int mask, Object o) {
        String a = s.toString();
        String b = prefix.toString();
        if (!ignoreCase) return a.startsWith(b);
        if (b.length() > a.length()) return false;
        return a.regionMatches(true, 0, b, 0, b.length());
    }

    public static boolean startsWith(CharSequence s, CharSequence prefix, int startIndex, boolean ignoreCase, int mask, Object o) {
        String a = s.toString();
        String b = prefix.toString();
        if (!ignoreCase) return a.startsWith(b, startIndex);
        if (startIndex + b.length() > a.length()) return false;
        return a.regionMatches(true, startIndex, b, 0, b.length());
    }

    public static int indexOf(CharSequence s, CharSequence needle, int startIndex, boolean ignoreCase, int mask, Object o) {
        return ignoreCase ? indexOfIgnoreCase(s, needle, startIndex) : s.toString().indexOf(needle.toString(), startIndex);
    }

    public static String replace(CharSequence s, CharSequence oldStr, CharSequence newStr, boolean ignoreCase, int mask, Object o) {
        String a = s.toString();
        String b = oldStr.toString();
        String c = newStr.toString();
        if (!ignoreCase) return a.replace(b, c);
        return Pattern.compile(Pattern.quote(b), Pattern.CASE_INSENSITIVE).matcher(a).replaceAll(
                java.util.regex.Matcher.quoteReplacement(c));
    }

    public static List<String> split(CharSequence s, List<String> delims, boolean ignoreCase, int limit, int mask, Object o) {
        StringBuilder re = new StringBuilder();
        for (String d : delims) {
            if (re.length() > 0) re.append('|');
            re.append(Pattern.quote(d));
        }
        int lim = limit <= 0 ? -1 : limit;
        String[] parts = Pattern.compile(re.toString(), ignoreCase ? Pattern.CASE_INSENSITIVE : 0).split(s.toString(), lim);
        List<String> out = new ArrayList<>();
        for (String part : parts) out.add(part);
        // Kotlin split(limit=0) 语义：移除尾部空串
        if (limit <= 0) {
            while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        }
        return out;
    }

    public static List<String> split(CharSequence s, char[] delims, boolean ignoreCase, int limit, int mask, Object o) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < delims.length; i++) {
            if (re.length() > 0) re.append('|');
            re.append(Pattern.quote(String.valueOf(delims[i])));
        }
        int lim = limit <= 0 ? -1 : limit;
        String[] parts = Pattern.compile(re.toString(), ignoreCase ? Pattern.CASE_INSENSITIVE : 0).split(s.toString(), lim);
        List<String> out = new ArrayList<>();
        for (String part : parts) out.add(part);
        if (limit <= 0) {
            while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        }
        return out;
    }

    public static String substringAfter(CharSequence s, CharSequence delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        String d = delim.toString();
        int i = a.indexOf(d);
        return i < 0 ? missing : a.substring(i + d.length());
    }

    public static String substringAfterLast(CharSequence s, CharSequence delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        String d = delim.toString();
        int i = a.lastIndexOf(d);
        return i < 0 ? missing : a.substring(i + d.length());
    }

    public static String substringBefore(CharSequence s, CharSequence delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        String d = delim.toString();
        int i = a.indexOf(d);
        return i < 0 ? missing : a.substring(0, i);
    }

    public static String substringBeforeLast(CharSequence s, CharSequence delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        String d = delim.toString();
        int i = a.lastIndexOf(d);
        return i < 0 ? missing : a.substring(0, i);
    }

    public static String substringBefore(CharSequence s, char delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        int i = a.indexOf(delim);
        return i < 0 ? missing : a.substring(0, i);
    }

    public static String substringAfterLast(CharSequence s, char delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        int i = a.lastIndexOf(delim);
        return i < 0 ? missing : a.substring(i + 1);
    }

    public static String substringBeforeLast(CharSequence s, char delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        int i = a.lastIndexOf(delim);
        return i < 0 ? missing : a.substring(0, i);
    }

    public static String substringAfter(CharSequence s, char delim, String missing, int mask, Object o) {
        String a = s.toString();
        if ((mask & 2) != 0) missing = a;
        int i = a.indexOf(delim);
        return i < 0 ? missing : a.substring(i + 1);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String joinToString(Iterable it, CharSequence sep, CharSequence prefix, CharSequence postfix,
                                      int limit, CharSequence truncated, Function1 transform, int mask, Object o) {
        // Kotlin $default 语义：mask 位为 1 时该参数取默认值，忽略调用方传入的值。
        if ((mask & 1) != 0) sep = ", ";
        if ((mask & 2) != 0) prefix = "";
        if ((mask & 4) != 0) postfix = "";
        if ((mask & 8) != 0) limit = -1;
        if ((mask & 16) != 0) truncated = "...";
        if ((mask & 32) != 0) transform = null;
        if (sep == null) sep = ", ";
        if (prefix == null) prefix = "";
        if (postfix == null) postfix = "";
        if (truncated == null) truncated = "...";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        int count = 0;
        Iterator<?> iter = it.iterator();
        while (iter.hasNext()) {
            Object e = iter.next();
            count++;
            if (count > 1) sb.append(sep);
            if (limit >= 0 && count > limit) {
                sb.append(truncated);
                break;
            }
            sb.append(transform != null ? transform.invoke(e) : e);
        }
        sb.append(postfix);
        return sb.toString();
    }
}
