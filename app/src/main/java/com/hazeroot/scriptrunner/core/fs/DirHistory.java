package com.hazeroot.scriptrunner.core.fs;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录访问历史（后退 / 前进）。
 *
 * 从 FilesFragment 里本轮临时加的 navHistory 抽出，改成独立可测的状态机。
 *
 * 语义：
 *  - {@link #record} 记录一次成功访问；重复记录同一目录不产生新条目
 *  - 记录新目录时截断"前进"分支（浏览器式行为）
 *  - {@link #back} / {@link #forward} 移动游标并返回目标目录
 * <p><b>⚠ 尚未接线</b>：本类当前<b>没有任何生产代码调用</b>，
 * 实际运行的是 {@code FilesFragment} 内的旧内联实现。
 * 因此单测覆盖的是「未被使用的代码」，请勿把本类当作运行时行为的真值来源。
 * 接线计划见 docs/core-fs接入审查.md。
 */
public final class DirHistory {

    /** 上限，防止无限增长 */
    private static final int MAX = 60;

    private final List<String> stack = new ArrayList<>();
    private int cursor = -1;

    /** 记录一次成功访问的目录 */
    public void record(String dir) {
        if (dir == null || dir.length() == 0) {
            return;
        }
        if (cursor >= 0 && cursor < stack.size() && dir.equals(stack.get(cursor))) {
            return;   // 已在当前位置，不重复记录
        }
        // 截断前进分支
        while (stack.size() > cursor + 1) {
            stack.remove(stack.size() - 1);
        }
        stack.add(dir);
        if (stack.size() > MAX) {
            stack.remove(0);
        }
        cursor = stack.size() - 1;
    }

    public boolean canBack() {
        return cursor > 0;
    }

    public boolean canForward() {
        return cursor >= 0 && cursor < stack.size() - 1;
    }

    /** 后退一步，返回要打开的目录；不能后退时返回 null */
    public String back() {
        if (!canBack()) {
            return null;
        }
        cursor--;
        return stack.get(cursor);
    }

    /** 前进一步，返回要打开的目录；不能前进时返回 null */
    public String forward() {
        if (!canForward()) {
            return null;
        }
        cursor++;
        return stack.get(cursor);
    }

    /** 当前目录；无记录返回 null */
    public String current() {
        return (cursor >= 0 && cursor < stack.size()) ? stack.get(cursor) : null;
    }

    public int size() {
        return stack.size();
    }

    public int cursor() {
        return cursor;
    }

    /** 清空 */
    public void clear() {
        stack.clear();
        cursor = -1;
    }
}
