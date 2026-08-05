# Kaloscope Android TV

> [!WARNING]
> 本 Android TV 客户端是一个 Vibe Coding 项目，代码主要由 AI 辅助生成。
> 代码质量、稳定性与设备兼容性均没有保证，请在使用前自行评估并承担风险。

> [!NOTE]
> 本项目接受完全由 AI 生成的 Pull Request（纯 AI PR）。
> 提交者无需手写代码，但仍需说明改动目的、完成必要验证，并对提交内容负责。

[![GitHub Release](https://img.shields.io/github/v/release/kaloscope/android-tv?label=Release&color=green&style=flat-square)](https://github.com/kaloscope/android-tv/releases)
[![Android TV](https://img.shields.io/badge/Platform-Android%20TV-3DDC84?logo=android&style=flat-square)](https://developer.android.com/tv)
[![Min SDK](https://img.shields.io/badge/minSdk-23-3DDC84?logo=android&style=flat-square)](app/build.gradle.kts)
[![MIT License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)

这是 [Kaloscope](https://github.com/kaloscope/kaloscope) 项目的原生电视客户端，面向仅使用遥控器的 Android TV 设备。
客户端连接到用户自行部署的服务端，提供媒体库浏览、网络资源搜索和电视端播放能力。

本项目不包含服务端，也不会在本地提供媒体管理服务。使用前需要准备一台客户端可以访问的服务器。

## 功能特性

- 使用 D-pad 方向键、确认键和返回键完成主要操作
- 添加、切换和删除服务器，并保存各服务器的登录状态
- 浏览最近观看记录、媒体库、媒体详情、分季和分集内容
- 使用服务端索引器搜索网络资源，并支持站点筛选条件
- 播放本地媒体和网络资源，支持渐进式媒体、HLS 与 DASH
- 本地媒体支持直连、HLS 转码及自动回退策略
- 本地媒体支持播放进度记录和续播；播放器支持章节、字幕、倍速和自动连播
- 支持弹幕显示、类型屏蔽、字号、速度、透明度和显示区域设置
- 支持默认启动页、播放模式、转码清晰度和字幕偏好等客户端设置

## 安装与使用

### 使用要求

- Android TV 设备，Android 6.0（API 23）或更高版本
- 已部署且可从电视设备访问的服务端
- 服务端账号；如需浏览或搜索内容，账号需有相应媒体库或索引器权限

服务端安装方式参见[部署指南](https://kaloscope.org/docs/deployment)。

### 安装 APK

1. 前往 [GitHub Releases](https://github.com/kaloscope/android-tv/releases) 下载最新版本的 APK。
2. 将 APK 传输并安装到 Android TV 设备，按系统提示允许安装即可。
3. 打开应用，添加服务器地址并登录账号。

安装新版本时直接覆盖安装即可。不要先卸载旧版本，以免丢失本机保存的服务器和登录状态。

## 项目结构

本项目采用单 `app` 模块、单 Activity 和 Jetpack Compose UI。主要代码位于`app/src/main/java/org/kaloscope/tv`：

```text
android-tv/
├── app/
│   └── src/
│       ├── main/java/org/kaloscope/tv/
│       │   ├── app/       # 应用外壳、启动流程、依赖注入与导航
│       │   ├── core/      # 通用模型、网络、存储、设计系统与播放策略
│       │   ├── data/      # Repository、远程 DTO、映射与持久化适配器
│       │   └── feature/   # 页面、ViewModel 与功能协调器
│       ├── test/          # JVM 单元测试和网络契约测试
│       └── androidTest/   # Compose UI、焦点、截图与设备测试
├── gradle/
│   └── libs.versions.toml # 依赖和插件版本目录
└── .github/workflows/     # GitHub Actions 工作流
```

主要功能包包括服务器配置与登录、首页、网络搜索、媒体库、媒体详情、播放器和设置。
页面级 ViewModel 通过 `StateFlow` 暴露不可变 UI 状态，复杂状态转换由可测试的协调器或策略类承载。

## 技术栈与主要依赖

实际依赖版本以 [Gradle Version Catalog](gradle/libs.versions.toml) 为准。

| 类别         | 主要组件                                                                                                                                                             | 用途                                                   |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------ |
| 语言与构建   | Kotlin、Gradle、Java 17                                                                                                                                              | Android 应用开发与构建                                 |
| TV UI        | [Jetpack Compose](https://developer.android.com/compose)、[Compose for TV](https://developer.android.com/develop/ui/compose/tv)、TV Material                         | 单 Activity 的遥控器友好界面                           |
| 导航         | [Navigation 3](https://developer.android.com/guide/navigation/navigation-3)                                                                                          | 使用可序列化路由管理页面栈                             |
| 依赖注入     | [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)                                                                                     | 组装网络、存储、Repository 和 ViewModel                |
| 网络与序列化 | [Retrofit](https://github.com/square/retrofit)、[OkHttp](https://square.github.io/okhttp/)、[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | 调用服务端 API 并解析 JSON                             |
| 异步状态     | [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)、StateFlow                                                                                         | 生命周期内的异步任务与 UI 状态流                       |
| 本地存储     | [Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore)、Android Keystore                                                      | 保存客户端设置并保护登录令牌                           |
| 图片         | [Coil](https://coil-kt.github.io/coil/)                                                                                                                              | 加载海报、背景图和头像等服务端图片                     |
| 播放         | [AndroidX Media3](https://developer.android.com/media/media3)                                                                                                        | ExoPlayer、MediaSession、HLS、DASH 和 Compose 播放界面 |
| 弹幕         | [AkDanmaku](https://github.com/C5H12O5/AkDanmaku)                                                                                                                    | 播放器弹幕渲染与同步                                   |
| 测试         | JUnit、Coroutines Test、MockWebServer、Compose UI Test                                                                                                               | 策略、数据、API 契约和 TV 交互验证                     |

## 本地开发

### 环境要求

- JDK 17
- Android SDK，包含项目所需的 API 37 平台和 Build Tools
- Android Studio 或可执行 Gradle Wrapper 的命令行环境
- 可选：Android TV 模拟器或实体设备，用于焦点和播放验证

仓库已经包含 Gradle Wrapper，无需单独安装 Gradle：

```bash
git clone https://github.com/kaloscope/android-tv.git
cd android-tv
./gradlew :app:assembleDebug
```

Debug APK 生成在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

常用验证命令：

```bash
# JVM 单元测试与网络契约测试
./gradlew :app:testDebugUnitTest

# Release 变体静态检查
./gradlew :app:lintRelease

# 本地 Release 构建；未提供完整签名环境变量时生成未签名 APK
./gradlew :app:assembleRelease
```

涉及遥控器焦点、按键、播放器或设备性能的改动，还应在 Android TV 模拟器或实体设备上验证。
更新已有测试安装时应保留应用数据，避免通过卸载或清空数据破坏登录状态。

## 参与贡献

欢迎提交 Issue 和 Pull Request，包括完全由 AI 生成的 PR。提交前请阅读[AGENTS.md](AGENTS.md) 中的项目边界，并至少说明：

- 要解决的问题以及修改范围
- 是否会影响界面、网络、存储、播放、遥控器或焦点行为
- 实际执行的构建、测试或设备验证
- 仍未验证的路径和已知风险

请勿提交服务器地址、账号、令牌、媒体路径、签名文件、密码或其他私有配置。
无论代码由人类还是 AI 生成，提交者都应先审阅最终 diff，并对提交内容负责。

## 相关项目

- [服务端与 Web UI](https://github.com/kaloscope/kaloscope)
- [使用文档](https://kaloscope.org/docs/introduction)
- [社区工作流模板](https://github.com/kaloscope/workflows)
- [弹弹play API 代理服务](https://github.com/kaloscope/danmaku)

## 开源协议

本项目基于 [MIT](LICENSE) 开源协议发布。
