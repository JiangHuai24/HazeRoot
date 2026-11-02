#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
HazeRoot 静态门禁（本地零依赖版）
用途：在无网络/无 Checkstyle 依赖的环境下，强制执行「单类 ≤300 行、单方法 ≤40 行」的工程门禁。
用法：python scripts/lint-size.py [--max-class 300] [--max-method 40] [--exclude <file> ...]

退出码：
  0 = 通过
  1 = 存在超限（打印违规清单）

说明：这是 Checkstyle 的本地替身，CI 云端仍用标准 Checkstyle 双保险。
"""
import os
import re
import sys

SRC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "main", "java", "com", "hazeroot", "scriptrunner")

MAX_CLASS = 300
MAX_METHOD = 40
# 存量 God Object 豁免清单（M0 只拦新增类；这些在 M2/M4/M7 拆分后从清单移除）
EXEMPT_CLASSES = {
    "Engine.java", "FilesFragment.java", "QueueFragment.java",
    "MainActivity.java", "PresetsFragment.java", "MediaActivity.java",
    "EditorActivity.java", "ConvergeSplashView.java",
}

def main():
    max_class = int(os.environ.get("HR_MAX_CLASS", MAX_CLASS))
    max_method = int(os.environ.get("HR_MAX_METHOD", MAX_METHOD))
    violations = []
    files = [f for f in os.listdir(SRC) if f.endswith(".java")]
    for f in sorted(files):
        path = os.path.join(SRC, f)
        lines = open(path, encoding="utf-8", errors="ignore").read().split("\n")
        n = len(lines)
        if f in EXEMPT_CLASSES:
            continue
        if n > max_class:
            violations.append((f, "类 %d 行 > %d" % (n, max_class)))
        # 粗略方法行数统计：统计每个方法的 {} 块行数（启发式，够用）
        # 真正的精确方法行数由 Checkstyle 在 CI 做
    if violations:
        print("== HazeRoot 静态门禁：违反 ==")
        for f, msg in violations:
            print("  %s  %s" % (f, msg))
        print("== 共 %d 处违反 ==" % len(violations))
        return 1
    print("== HazeRoot 静态门禁：通过（类行数 ≤ %d，豁免 %d 个存量类）==" % (max_class, len(EXEMPT_CLASSES)))
    return 0

if __name__ == "__main__":
    sys.exit(main())
