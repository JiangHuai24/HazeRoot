package com.hazeroot.scriptrunner;

import android.text.Editable;
import android.text.style.ForegroundColorSpan;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.StringsKt;

/* compiled from: SyntaxHighlighter.kt */

/* loaded from: classes3.dex */
public final class SyntaxHighlighter {
    public static final SyntaxHighlighter INSTANCE = new SyntaxHighlighter();
    private static final int MAX_BYTES = 262144;

    /* compiled from: SyntaxHighlighter.kt */
    
    public enum Lang {
        SHELL,
        PYTHON,
        JSON,
        INI,
        NONE;
    }

    /* compiled from: SyntaxHighlighter.kt */
    
    public /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[Lang.values().length];
            try {
                iArr[Lang.SHELL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Lang.PYTHON.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Lang.JSON.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[Lang.INI.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[Lang.NONE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    private SyntaxHighlighter() {
    }

    public final Lang detect(String fileName) {
        Intrinsics.checkNotNullParameter(fileName, "fileName");
        if (Kt.endsWith(fileName, ".sh", false, 2, (Object) null) || Kt.endsWith(fileName, ".bash", false, 2, (Object) null)) {
            return Lang.SHELL;
        }
        if (Kt.endsWith(fileName, ".py", false, 2, (Object) null)) {
            return Lang.PYTHON;
        }
        if (Kt.endsWith(fileName, ".json", false, 2, (Object) null)) {
            return Lang.JSON;
        }
        if (Kt.endsWith(fileName, ".ini", false, 2, (Object) null) || Kt.endsWith(fileName, ".conf", false, 2, (Object) null) || Kt.endsWith(fileName, ".cfg", false, 2, (Object) null)) {
            return Lang.INI;
        }
        return Lang.NONE;
    }

    public final void highlight(Editable ed, Lang lang, int cKeyword, int cString, int cComment, int cNumber, int cBuiltin) {
        Intrinsics.checkNotNullParameter(ed, "ed");
        Intrinsics.checkNotNullParameter(lang, "lang");
        if (lang == Lang.NONE || ed.length() > 262144) {
            return;
        }
        Object[] spans = ed.getSpans(0, ed.length(), ForegroundColorSpan.class);
        Intrinsics.checkNotNullExpressionValue(spans, "getSpans(...)");
        for (ForegroundColorSpan s : (ForegroundColorSpan[]) spans) {
            ed.removeSpan(s);
        }
        String text = ed.toString();
        int lineStart = 0;
        while (lineStart <= text.length()) {
            int lineEnd = Kt.indexOf(text, "\n", lineStart, false, 4, (Object) null);
            if (lineEnd < 0) {
                lineEnd = text.length();
            }
            int lineEnd2 = lineEnd;
            highlightLine(ed, text, lineStart, lineEnd2, lang, cKeyword, cString, cComment, cNumber, cBuiltin);
            if (lineEnd2 != text.length()) {
                lineStart = lineEnd2 + 1;
            } else {
                return;
            }
        }
    }

    private final void span(Editable ed, int start, int end, int color) {
        if (end > start) {
            ed.setSpan(new ForegroundColorSpan(color), start, end, 33);
        }
    }

    private final void highlightLine(Editable ed, String text, int ls, int le, Lang lang, int cKeyword, int cString, int cComment, int cNumber, int cBuiltin) {
        String line = text.substring(ls, le);
        Intrinsics.checkNotNullExpressionValue(line, "substring(...)");
        switch (WhenMappings.$EnumSwitchMapping$0[lang.ordinal()]) {
            case 1:
                highlightLine$mark(line, ed, ls, new Regex("\\b(if|then|elif|else|fi|for|while|until|do|done|case|esac|function|in|select|return|exit|local|export|readonly|declare|unset|shift|break|continue|source|alias|eval|exec|trap|set)\\b"), cKeyword);
                highlightLine$mark(line, ed, ls, new Regex("\\b(echo|printf|read|cd|ls|cat|rm|mv|cp|mkdir|rmdir|chmod|chown|chgrp|ln|touch|grep|egrep|sed|awk|cut|sort|uniq|head|tail|wc|find|xargs|test|true|false|sleep|kill|ps|mount|umount|sync|dd|tar|gzip|gunzip|curl|wget|modprobe|insmod|rmmod|sysctl|setprop|getprop|am|pm|su|sudo|busybox|od|tr|tee)\\b"), cBuiltin);
                highlightLine$mark(line, ed, ls, new Regex("\\$\\{[^}\\n]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*|\\$[0-9@#?*!$-]"), cNumber);
                highlightLine$mark(line, ed, ls, new Regex("\\b\\d+\\b"), cNumber);
                highlightLine$mark(line, ed, ls, new Regex("[frb]?\"[^\"\\n]*\"|'[^'\\n]*'"), cString);
                highlightLine$mark(line, ed, ls, new Regex("#.*$"), cComment);
                break;
            case 2:
                highlightLine$mark(line, ed, ls, new Regex("\\b(def|class|if|elif|else|for|while|try|except|finally|with|as|import|from|return|yield|lambda|pass|break|continue|in|not|and|or|is|None|True|False|global|nonlocal|raise|assert|del|async|await)\\b"), cKeyword);
                highlightLine$mark(line, ed, ls, new Regex("\\b(print|len|range|open|str|int|float|bool|list|dict|set|tuple|input|type|isinstance|enumerate|zip|map|filter|sorted|reversed|sum|min|max|abs|round|repr|format|self|cls|os|sys|re|json|time|subprocess|threading)\\b"), cBuiltin);
                highlightLine$mark(line, ed, ls, new Regex("\\b\\d+(\\.\\d+)?\\b"), cNumber);
                highlightLine$mark(line, ed, ls, new Regex("[frbu]?\"[^\"\\n]*\"|[frbu]?'[^'\\n]*'"), cString);
                highlightLine$mark(line, ed, ls, new Regex("#.*$"), cComment);
                break;
            case 3:
                highlightLine$mark(line, ed, ls, new Regex("\"[^\"]*\""), cString);
                highlightLine$mark(line, ed, ls, new Regex("\"[^\"]*\"(?=\\s*:)"), cKeyword);
                highlightLine$mark(line, ed, ls, new Regex("-?\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b"), cNumber);
                highlightLine$mark(line, ed, ls, new Regex("\\b(true|false|null)\\b"), cBuiltin);
                break;
            case 4:
                highlightLine$mark(line, ed, ls, new Regex("^\\[[^\\]\\n]+\\]"), cKeyword);
                highlightLine$mark(line, ed, ls, new Regex("[#;].*$"), cComment);
                highlightLine$mark(line, ed, ls, new Regex("^\\s*[^=\\n]+(?==)"), cNumber);
                highlightLine$mark(line, ed, ls, new Regex("(?<==).*$"), cString);
                break;
        }
    }

    private static final void highlightLine$mark(String line, Editable $ed, int $ls, Regex re, int color) {
        java.util.Iterator<MatchResult> it = re.findAll((CharSequence) line, 0).iterator();
        while (it.hasNext()) {
            MatchResult m = it.next();
            INSTANCE.span($ed, m.getRange().getFirst() + $ls, m.getRange().getLast() + $ls + 1, color);
        }
    }
}
