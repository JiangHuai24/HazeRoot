# HazeRoot

**Android 通用 root 脚本执行器** —— 把「每次手动敲一堆回答」变成「点一下跑完」。

HazeRoot 以 root 身份按队列顺序执行 shell 脚本 / ELF 可执行文件，自动应答脚本的交互提问；整条队列连同回答记录可存为「方案」，之后一键重放。适用场景：需要在 root 环境反复执行同一套脚本的设备调试与自动化。

## 功能特性

| 能力 | 说明 |
| --- | --- |
| 脚本执行 | 以 root 身份运行 `.sh` 与 ELF 可执行文件，支持队列顺序执行 |
| 交互应答 | 脚本提问时给出提示行，在输入框回答；回答自动录制 |
| 一键方案 | 队列 + 回答记录存为「方案」，一键重放、自动回填答案 |
| 终端 | ANSI 颜色解析 + UTF-8 / GBK 双编码自动识别，中文不乱码 |
| 文件管理 | 双窗文件管理：复制 / 移动 / 压缩 / 解压 / 权限 / 重命名 / 删除 / 分享 / APK 安装 |
| 编辑与预览 | 内置脚本编辑器（语法高亮 + 字符集识别）、媒体播放、图文使用手册 |

## 运行要求

- Android 8.0 及以上（minSdk 26）
- 已获取 root 权限（Magisk / KernelSU / APatch 任一）
- 不依赖任何品牌专属接口

## 构建

环境：JDK 17+、Android SDK（compileSdk 34）、Gradle 8.6

```bash
./gradlew assembleDebug     # 或 gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 文档

- [功能与实现逻辑](docs/HazeRoot-功能与实现逻辑.md) —— 全部模块的底层实现与设计细节
- [版本沿革与迭代记录](docs/HazeRoot-版本沿革与迭代记录.md) —— 版本时间线与迭代规范
- [交接文档](docs/HazeRoot-交接文档.md) —— 工程交接与维护说明

## 版本

当前版本 **v2.8.4**（versionCode 70）

## 许可

[MIT License](LICENSE) © HazeRoot

---

本项目仅供学习与技术研究使用，请确保在合法合规的范围内使用。
