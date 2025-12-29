# 模块边界对照（shared ↔ app）

用于下载业务渐进移植/梳理时的快速核对：哪些代码放 shared，哪些留在 app，常见桥接方式，以及验证步骤。

## shared（commonMain）应包含
- 纯模型与序列化：`VideoInfo` / `Format` / `PlaylistResult` / `DownloadPreferences` 等通用数据结构。
- 纯业务逻辑与算法：不依赖平台 API，只依赖标准库、kotlinx-serialization、kotlinx-coroutines（参见 shared/build.gradle.kts 的依赖）。
- 纯工具函数：跨平台可用的解析/转换/验证逻辑。

## shared（androidMain/desktopMain）可选放置
- 极少量平台分支实现（若必须用平台 API，隔离在 androidMain/desktopMain，不进入 commonMain）。
- 推荐先考虑“在 app/desktop 做适配”，除非跨平台复用价值很明确。

## app（Android）应包含
- 平台/框架依赖：Room 实体与 DAO、MMKV/Preferences 访问、网络/文件系统、通知/前台服务、生命周期感知逻辑。
- 下载编排：`Task` 及其状态机、命令模板（`CommandTemplate`）、与数据库/存储交互的实现。
- UI/DI/启动入口：Compose 界面、ViewModel、Koin/Hilt wiring、Android 资源与配置。
- 桥接层：把 Android 数据源映射成 shared 模型（或反向），在 app 内实现。

## desktop 模块
- 依赖 shared，编写桌面专用 UI/文件系统/网络适配；同样通过适配层与 shared 模型交互。

## 禁止/避免
- 不要在 shared/commonMain 引入 AndroidX/Room/OkHttp/MMKV/Compose 及任何 Android 特定 API。
- 不要在 shared 声明与 app 相同名的数据类（避免重复声明/歧义）。
- 不要让 app 反向被 shared 引用；依赖方向保持 app → shared。

## 典型桥接方式
- 映射函数：在 app 中写 `fun Task.toSharedModel(): DownloadPreferences` 或反向 mapper。
- 工厂/适配：在 app 暴露 `fun Preferences.toDownloadPreferences()`，不要把 Preferences 直接塞进 shared。
- 平台分支：若必须跨平台共用接口，定义在 shared/commonMain，具体实现放 androidMain/desktopMain。

## 下载相关迁移检查清单
- 数据模型单一来源：`VideoInfo`/`Format`/`PlaylistResult`/`DownloadPreferences` 仅在 shared；app 不再保留重复定义（可删除占位的旧文件）。
- 任务与编排留在 app：`Task` 以及依赖数据库/文件/网络的逻辑不进入 shared。
- 偏好读取入口：创建/读取 `DownloadPreferences` 的实现放在 app 的适配层，shared 仅持有数据类本身。
- UI 只在 app/desktop：Compose 组件、Material3 等不进入 shared。
- 依赖方向：只允许 app/desktop -> shared，禁止 shared 反向依赖 app/desktop。

## 快速验证步骤
- 构建 shared：`./gradlew :shared:check`
- 构建 Android：`./gradlew :app:assembleDebug`
- 构建 Desktop（如需）：`./gradlew :desktop:packageReleaseDistributionForCurrentOS`
- 依赖方向自检：全局搜索是否有 shared/commonMain 调用 Android/Room/MMKV/Compose。

## 记录与沟通
- PR 描述中注明是否修改了 shared；若修改，需确认不含 Android 依赖、无重复模型。
- 审查时对照本清单，避免回归或重复声明。
