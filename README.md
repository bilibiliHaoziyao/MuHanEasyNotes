# 慕寒轻松记（MuHanEasyNotes）

一款专为 **Android 智能手表（Wear OS）** 设计的记事本应用，界面与交互参考小米便签，针对手表小屏优化，主打**语音快速记录**。

> For a long time, Android watches have had no convenient sticky notes available. MuHanEasyNotes is the solution to that problem.

## 功能特性

- 📝 **新建 / 编辑 / 删除笔记**，本地持久化（Room 数据库），离线可用
- 🎙️ **语音输入**：点按麦克风即可把要说的话变成文字，最适合手表端使用；自动申请录音权限；设备无语音引擎时自动隐藏
- ⚙️ **软件内设置**：可调整界面缩放大小、字体大小，并支持编辑时自动保存（防抖 1.2s）；内含「关于」页
- 🏷️ **智能标题**：标题可不填，保存后自动取正文第一句话作为标题
- 📌 **置顶**：重要笔记一键置顶，列表顶部优先展示
- 🎨 **多彩笔记**：内置 7 种小米便签风格颜色，可自定义每条笔记的强调色
- ⌚ **沉浸交互**：无顶栏全屏沉浸显示、弯曲滚动列表、滑动返回、位置指示器
- 📱 **兼容更广**：最低 Android 7.1.1（API 25），同时支持 32 位与 64 位 ABI，兼容手机屏幕布局
- 🔍 列表卡片显示标题 + 摘要 + 更新时间，点击进入编辑，长按快速删除

## 技术栈

| 组件 | 说明 |
| --- | --- |
| Kotlin + Jetpack Compose for Wear OS | UI 层 |
| androidx.wear.compose:compose-material 1.4.0 | Wear 风格组件（Material1 稳定版） |
| Room 2.6.1 | 本地数据库 |
| ViewModel + Kotlin 协程/Flow | 架构层 |
| 系统语音识别（RecognizerIntent） | 语音输入 |

## 环境要求

- JDK 17+
- Android SDK：compileSdk 35、minSdk 25（Android 7.1.1 / Wear OS 2.x 及以上）、build-tools 34.0.0
- Gradle 8.14.5（已内置 Wrapper）

## 快速构建

```bash
# 1. 确保 local.properties 中配置了 SDK 路径，或设置环境变量 ANDROID_HOME
# 2. 构建 Debug APK
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 构建 Release APK（开启 R8 压缩）
./gradlew :app:assembleRelease
```

## 安装到手表

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/muhan/notes/
├── MainActivity.kt            # 入口，注入 ViewModel
├── NoteViewModel.kt           # 状态与业务逻辑
├── data/                      # 数据层（Room）
│   ├── Note.kt                # 笔记实体
│   ├── NoteDao.kt             # 数据访问对象
│   ├── NoteDatabase.kt        # 数据库
│   └── NoteRepository.kt      # 仓库
└── ui/
    ├── NotesApp.kt            # 导航（SwipeDismissableNavHost）
    ├── theme/                 # 主题与颜色
    ├── list/                  # 笔记列表页
    ├── edit/                  # 新建/编辑页
    └── components/            # 语音按钮、笔记卡片等复用组件
```

## 开源协议

本项目基于 [MIT License](LICENSE) 开源，自由使用、修改与分发。

Copyright (c) 2026 MuHan
