# Android 与 Desktop 交付进度总览（2026-04-27）

> 归档说明（2026-07-08）：本文保留为 2026-04 阶段的历史进度基线和迁移决策记录。当前项目整体整理、Android/Desktop parity、冗余代码清理和新的未完成项已经合并到 `docs/desktop-project-audit-2026-06-15.md`。本文中“唯一进度基线”“P1 0 项 + P2 2 项”等结论仅代表当时快照，不再作为当前状态依据。

## 1. 目的与范围
- 本文曾是 Android 与 Desktop 对齐收尾阶段的进度基线，聚焦后端、迁移与模块边界；当前已降级为历史基线。
- 本文不覆盖纯 UI 视觉与动效差异。
- 本文已合并原模块边界与迁移路线文档的有效内容，并删除重复文件。

## 2. 结论摘要（历史快照）
- 历史快照：Desktop 相对 Android 当时剩余 P1 0 项 + P2 2 项。
- 已完成项：并发下载调度、窗口关闭语义、SQLite 数据持久化（三后端 DualWrite）、模板备份语义、Cookies 后端闭环、自定义命令通知闭环（started/completed/error）、自定义命令任务恢复语义升级（Interrupted 状态）。
- 可废除逻辑分三档：立即可删 1 项、条件下线 1 组、必须保留 1 组。

### 2.1 文档治理结论
1. 历史口径：本文曾作为当时唯一进度源，用于合并早期“边界清单/迁移路线图”副本。
2. 当前口径：模块边界、迁移里程碑、收尾缺口和清理策略统一在 `docs/desktop-project-audit-2026-06-15.md` 维护，避免同一事项多文档漂移。

## 3. Android 与 Desktop 后端差距复审（含已闭环项）

### 3.1 并发下载调度（P0，已完成）
- 现状：Android 具备并发上限和调度门禁；Desktop 已落地并发下载调度。
- 结果：Desktop 现已通过 `Semaphore` 实现并发限流，默认并发上限为 3。
- 证据：
  - [Android 并发上限常量](../app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt#L43)
  - [Android 并发门禁](../app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt#L220)
  - [Desktop 并发上限常量](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L53)
  - [Desktop 并发信号量](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L76)
  - [Desktop 启动门禁 withPermit](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L224)
  - [Desktop 启动入口 1](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L430)
  - [Desktop 启动入口 2](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L605)
- 结论：3.1 从缺陷项转为已对齐项，后续仅保留“并发上限可配置化”增强。

### 3.2 后台保活语义（P0，桌面关闭语义已完成）
- 现状：Android 通过前台服务保活；Desktop 不提供独立后台服务，但已实现窗口关闭拦截与退出确认。
- 结果：Desktop 已具备“防误退出 + 优雅停机 + 状态回写”的最小闭环。
- 证据：
  - [Android 前台服务保活](../app/src/main/java/com/junkfood/seal/DownloadService.kt#L24)
  - [Android 服务启停联动](../app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt#L107)
  - [Desktop 窗口关闭拦截](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L190)
  - [Desktop 退出确认弹窗触发](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L192)
  - [Desktop 窗口 onCloseRequest 接线](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L199)
  - [Desktop 退出前取消并持久化](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L248)
  - [Desktop 运行任务统计与批量取消](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L116)
- 结论：3.2 从“缺失”转为“已达成桌面端语义闭环”；独立后台进程/托盘常驻不在本轮目标内。

### 3.3 数据持久化层级（P0，已完成）
- 当前状态：Desktop 已通过 `DesktopStorageBackend`（`json` / `dual` / `sqlite`）实现三后端存储切换，默认 `dual`（DualWrite）。
- 已完成 SQLite JDBC 接入，queue/history/app-settings 均走三后端分支读写；JSON 保底链路统一走原子写 + 损坏隔离 + 结构化事件日志。
- M1-M4 四个里程碑全部完成，自检命令通过。
- 以下 3.3.1-3.3.2 为迁移前基线分析，保留作为决策依据，不代表当前代码状态。

#### 3.3.1 差距矩阵（迁移前基线，仅供参考）

| 维度 | Android | Desktop | 差距与风险 |
| --- | --- | --- | --- |
| 模式治理与迁移 | Room 明确声明 schema 版本与自动迁移链路（v1 到 v5）。 | 仅队列文件带 version 字段；历史与设置文件没有显式 schema 版本与迁移步骤。 | Desktop 在字段变更时缺少统一升级契约，容易出现“可读但语义错位”或“读失败后回退默认值”的隐性风险。 |
| 一致性与事务 | DAO 层存在事务语义，支持读写门禁与批量操作原子化。 | 多处直接 Files.writeString 覆盖写文件，跨文件更新没有事务边界。 | 进程异常退出时，队列、历史、设置可能出现跨文件状态不一致。 |
| 查询与读取模式 | 支持按 id/path 定向查询与 Flow 增量观察。 | 以整文件读取反序列化为主，再在内存中处理。 | 数据规模增长后，读取放大明显，历史查询与筛选成本更高。 |
| 容错与可观测性 | 结构化层可在迁移/查询阶段暴露失败点。 | 多处 runCatching 后直接回退 empty/default。 | 解析失败可能被静默吞掉，用户表现为“数据丢失/清空”，但缺少明确告警与恢复路径。 |
| 跨域数据统一性 | 下载历史、模板、Cookies、快捷方式在同一数据库治理。 | 队列、历史、设置分散在多个 JSON 文件。 | 跨域联动变更（例如任务状态与设置快照）需要手工保证一致性，维护成本高。 |

#### 3.3.2 迁移前代码证据（已通过方案 A 解决，仅供参考）
- Android 侧（对照基线）：
  - [数据库版本与自动迁移链路](../app/src/main/java/com/junkfood/seal/database/AppDatabase.kt#L19)
  - [Room 实体统一注册](../app/src/main/java/com/junkfood/seal/database/AppDatabase.kt#L11)
  - [事务门禁 insertInfoDistinctByPath](../app/src/main/java/com/junkfood/seal/database/VideoInfoDao.kt#L39)
  - [按路径查询与定向删除](../app/src/main/java/com/junkfood/seal/database/VideoInfoDao.kt#L36)
  - [Flow 增量读取接口](../app/src/main/java/com/junkfood/seal/database/VideoInfoDao.kt#L22)
- Desktop 侧：
  - [队列 JSON 仅有 version=1](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L37)
  - [队列解析失败回退默认对象](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L58)
  - [历史解析失败回退空列表](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L166)
  - [设置解析失败回退 null](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L70)
  - [队列/历史/设置均为直接覆盖写文件](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L66)、[历史写入](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L174)、[设置写入](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L77)

> **注意：以上 Desktop 侧证据为迁移前状态。当前代码已全面替换为三后端分支（`when(backedn)`）+ SQLite + 原子写，详见以下 M1-M4 完成证据。**

#### 3.3.3 收尾阶段建议（SQLite 灰度迁移前提下）
1. 采用三后端开关：`json` / `dual` / `sqlite`，默认 `dual`，确保可灰度、可回滚。
2. DualWrite 阶段保持“读 JSON、写 JSON+SQLite”，并在 JSON 不可用时自动回退读 SQLite。
3. JSON 写入统一走原子写（临时文件 + 原子替换）并按路径串行化，避免并发交叉写。
4. 解析失败必须做损坏隔离（`.corrupt-*.bak`）并输出结构化存储事件日志。
5. 达成稳定后再按发布节奏切换默认后端为 `sqlite`，最后保留 `json` 仅作应急回滚。

#### 3.3.4 方案对比与最终决策（定版）

| 方案 | 改动规模 | 收益 | 风险 | 决策 |
| --- | --- | --- | --- | --- |
| A. 分阶段数据库化（引入 SQLite，先 dual 再 sqlite） | 中高 | 在可回滚前提下补齐事务与查询基础，逐步降低 JSON 一致性风险 | 需要灰度窗口与回归验证 | 采用（已落地） |
| B. 仅做 JSON 治理升级（版本化 + 原子写 + 可观测） | 中 | 能缓解部分一致性风险，改动相对可控 | 无法从根本上解决查询/跨域一致性问题 | 不采用 |
| C. 维持现状，仅补文档 | 低 | 无开发成本 | 风险延续，线上问题难追踪 | 不采用 |

- 最终决策：收尾版本采用方案 A（分阶段数据库化），已完成 SQLite 接入与 dual 灰度。
- 决策原因：在 [Desktop 队列存储](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L53)、[历史存储](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L166)、[设置存储](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L68) 保持现有调用面的前提下，可最低风险接入数据库能力。
- 生效范围：仅限 Desktop 的 queue/history/app-settings 三类持久化链路。
- 非目标：本轮不调整 UI 页面结构；不改 Android Room 侧现有模型。

#### 3.3.5 最终补充决策条款（执行口径）
1. 后端切换条款（必须）
  - 三后端开关固定为 `json` / `dual` / `sqlite`，默认 `dual`。
  - 开关来源统一支持 JVM 参数 `seal.desktop.storage.backend` 与环境变量 `SEAL_DESKTOP_STORAGE_BACKEND`。
2. SQLite 条款（必须）
  - 引入本地 SQLite 文件 [seal.db](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStoragePaths.kt#L27)。
  - 首次初始化执行 JSON 引导导入，并记录 `sqlite_json_bootstrap_completed` 事件。
3. JSON 安全条款（必须）
  - JSON 保底链路统一走原子写工具 [writeTextAtomically](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopFileIo.kt#L18)。
  - 同路径写入串行化，避免并发协程交叉覆盖。
4. 容错条款（必须）
  - 解析失败不得静默丢弃：必须保留损坏文件副本（`.corrupt-*.bak`）并记录结构化日志。
  - Dual 模式下 JSON 不可用时必须回退读 SQLite，并记录 `dual_mode_fallback_to_sqlite`。
5. 可观测条款（必须）
  - 存储链路统一输出结构化事件日志 [DesktopStorageEventLogger](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageEventLogger.kt#L17)。
  - 关键事件至少覆盖：SQLite 初始化、JSON 引导导入、解析失败、回退读取、损坏隔离。

#### 3.3.6 里程碑与完成定义（Definition of Done）
1. M1：后端骨架与依赖接入（已完成）
  - 交付：SQLite JDBC 依赖、三后端开关、统一存储路径。
  - 完成标准：可通过开关切换 `json` / `dual` / `sqlite`。
2. M2：双写灰度与引导导入（已完成）
  - 交付：queue/history/app-settings DualWrite、SQLite 首次 JSON 导入。
  - 完成标准：Dual 模式下可读写两端，并在 JSON 缺失时从 SQLite 读取。
3. M3：安全写入与容错可观测（已完成）
  - 交付：原子写、路径级串行化、损坏隔离、结构化事件日志。
  - 完成标准：解析失败时保留损坏样本，且可通过事件日志定位问题。
4. M4：一致性回归与可执行自检（已完成）
  - 交付：自检入口 [desktopStorageSelfCheck](../desktop/build.gradle.kts#L55)。
  - 完成标准：`json/dual/sqlite` 三后端自检通过，Dual 模式损坏回退通过。

#### 3.3.7 从 Dual 切换到 Sqlite 默认的触发条件（前瞻）
满足全部条件后可将默认后端从 `dual` 切换为 `sqlite`：
1. 连续两个版本未出现存储相关 P0/P1 回滚事件。
2. 自检脚本与回归用例在主分支持续通过。
3. 关键链路已具备“JSON 仅作兜底”运行观测，且 SQLite 失败率处于可接受范围。
4. 发布说明明确保留 `json` 开关作为应急回退路径。

#### 3.3.8 自检命令（当前执行基线）
1. 单后端定向验证：
  - `./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=json -PstorageStateDir=/tmp/seal-json`
  - `./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=dual -PstorageStateDir=/tmp/seal-dual`
  - `./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=sqlite -PstorageStateDir=/tmp/seal-sqlite`
2. 当前仓库不保留独立 shell wrapper，直接运行 Gradle 任务即为已验证路径。

#### 3.3.9 当前实现代码证据（M1-M4 完成，2026-04-27 复核）
- 三后端开关：[`DesktopStorageBackend` 枚举 + `DesktopStorageConfig` 环境变量/JVM 参数读取](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageBackend.kt#L3)
- SQLite 存储（queue/history/app-settings 三表 + JSON 引导导入 + WAL 模式）：[`DesktopSqliteStorage`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopSqliteStorage.kt#L16)
- 三后端读写分支：[队列 `when(backend)`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L85)、[历史 `when(backend)`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L195)、[设置 `when(backend)`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L93)
- DualWrite 读优先 JSON、JSON 不可用时回退 SQLite：[队列 DualWrite 分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L87)、[历史 DualWrite 分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L197)、[设置 DualWrite 分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L96)
- 原子写工具：[`writeTextAtomically` + 路径级串行化锁](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopFileIo.kt#L20)
- 损坏隔离：[`quarantineCorruptedFile` 生成 `.corrupt-*.bak`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopFileIo.kt#L42)
- 结构化事件日志：[`DesktopStorageEventLogger`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageEventLogger.kt#L17)
- 自检脚本：[`DesktopStorageSelfCheckMain`](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageSelfCheckMain.kt#L62)、[Gradle 任务注册](../desktop/build.gradle.kts#L55)

### 3.4 Cookies 后端管理（已完成）
- 当前状态：Desktop 已补全文件导入/导出/清除/计数/开关检查最小闭环，通过 `FileDialog` + `DesktopYtDlpPaths.cookiesFile()` 实现 Netscape 格式 cookies 管理。
- 执行层 `--cookies` 注入已就绪（`DesktopDownloadController` + `DesktopCustomCommandTaskManager`），与 UI 开关联动。
- 详细分析：[cookies-gap-analysis.md](cookies-gap-analysis.md)
- 实现计划：[cookies-implementation-plan.md](cookies-implementation-plan.md)（Task 1-5 全部完成）

### 3.5 通知交互动作（已完成 / Desktop 平台限制说明）
- 现状：自定义命令任务已具备完整通知生命周期：**任务开始**发送 "Command Started" 系统通知，运行中通过应用内悬浮卡片（`DesktopCustomCommandNotificationOverlay`）提供取消/查看日志动作，**完成/失败**发送 "Command Completed/Error" 系统通知。普通下载任务已具备 Completed/Error 系统通知。
- **平台限制**：跨平台系统通知交互动作（点击通知 cancel/retry）需要平台原生集成（Linux DBus action callback、macOS UNUserNotification、Windows Toast via COM），超出当前纯 JVM `notify-send`/`gdbus`/`osascript` 方案能力范围。应用内覆盖层是对等的桌面替代方案。
- 结论：本轮 P1 已在应用内闭环，系统通知交互动作列为平台专项（可选，非当前收尾范围）。
- 证据：
  - [自定义命令 started 通知注入](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L119)
  - [自定义命令 completed/error 通知](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L173)
  - [应用内运行中覆盖层](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandNotificationOverlay.kt#L36)
  - [Android 通知动作按钮（对照）](../app/src/main/java/com/junkfood/seal/util/NotificationUtil.kt#L102)
### 3.6 模板与快捷方式备份语义（已完成）
- 现状：Android 备份模型覆盖模板、快捷方式、历史；Desktop 已实现 templates + shortcuts 统一导入导出（见 3.7.2 第 6 项），同时兼容旧单模板 JSON 格式。
- 遗留：跨端备份 JSON 格式规范尚未与 Android `Backup.kt` 完全统一，列为 P2 后续改进。
- 证据：
  - [Android 模板导出入口](../app/src/main/java/com/junkfood/seal/database/backup/BackupUtil.kt#L20)
  - [Android Backup 字段定义](../app/src/main/java/com/junkfood/seal/database/backup/Backup.kt#L10)
  - [Desktop 模板与快捷方式持久化字段](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L42)
  - [Desktop backup 导出（templates + shortcuts）](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/CommandSettingsPage.kt#L129)
  - [Desktop backup 导入（先尝试 Backup 再回退单模板）](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/CommandSettingsPage.kt#L142)

### 3.7 自定义命令后端与页面一致性（复审，P0/P1/P2）
- 本轮复审范围：
  - Desktop 设置页：`CommandSettingsPage`、`TemplateEditPage`。
  - Desktop 侧边栏自定义命令页：`DesktopCustomCommandScreen`、`DesktopCustomCommandTaskManager`。
  - Desktop 下载入口接线：`Main`、`DesktopDownloadScreen`。
  - Android 对照页：`TemplateListPage`、`CommandTemplateDialog`、`TaskListPage`、`TaskLogPage`。
  - 后端状态链路：Android 前台服务/通知/任务备份 vs Desktop 进程内任务列表。

#### 3.7.1 复审结论摘要（2026-04-25 与代码对齐版）
1. 本轮对比确认：之前标记的 8 项差距中，6 项已被代码修复（窗口关闭保护、模板备份语义、模板编辑漂移、侧边栏偏好源、任务持久化、下载入口命令模式语义），1 项部分修复（通知），1 项仍为 P2 UI 完备度。
2. 当前关键闭环项已完成，剩余 P1 为 2 个增强项（通知增强、任务恢复语义升级）。
3. 其余保留项为 P2：设置页/日志页交互完备度。

#### 3.7.2 已对齐项（代码复审确认，共 11 项）
1. 已对齐：设置页“使用自定义命令”与当前模板已接入 Desktop 下载页执行分支。
  - 证据：[Main 注入下载页](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L478)、[下载页按开关转入命令任务执行](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadScreen.kt#L420)。
2. 已对齐：侧边栏任务卡已具备重启、查看日志、复制日志、复制错误等核心动作。
  - 证据：[任务卡动作集合](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandScreen.kt#L363)。
3. 已对齐：侧边栏任务输入支持多链接，多模板切换/编辑会同步当前选中模板字段。
  - 证据：[多行输入与模板选择同步](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandScreen.kt#L463)。
4. 已对齐：窗口关闭保护已同时检查普通下载与自定义命令运行任务。
  - 证据：[Main 关闭拦截加入 DesktopCustomCommandTaskManager 判断](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L190)。
5. 已对齐：Desktop 自定义命令任务已接入快照落盘，重启时恢复历史任务（Running -> Interrupted，与 Canceled 区分，提供专属 Restart 入口）。
  - 证据：[任务存储 load/save](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskStorage.kt#L23)、[启动恢复与 snapshotFlow 持续保存](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L64)。
6. 已对齐：Desktop 设置页模板导入导出已升级为 templates + shortcuts 语义，同时兼容旧单模板 JSON。
  - 证据：[导出 DesktopCommandBackup](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/CommandSettingsPage.kt#L129)、[导入先尝试 Backup 再回退单模板](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/CommandSettingsPage.kt#L142)。
7. 已对齐：设置页模板编辑/删除不再无条件改写当前选中模板，仅在编辑当前选中模板或选中项失效时才更新。
  - 证据：[保存时的 shouldUpdateSelection 判断](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/TemplateEditPage.kt#L102)、[删除时的 currentSelectionRemoved 回退](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/command/TemplateEditPage.kt#L68)。
8. 已对齐：侧边栏自定义命令页已复用主界面的共享 settingsState，不再单独创建偏好快照。
  - 证据：[Main 将 settingsState 传入 DesktopCustomCommandScreen](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L570)、[接收参数](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandScreen.kt#L123)。
9. 已对齐：自定义命令任务具备最小通知反馈，完成/失败时根据设置触发系统通知。
  - 证据：[通知分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L173)。
10. 已对齐：主导航自定义命令入口图标已改为终端语义。
  - 证据：[Destination.CustomCommand 图标](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L587)。
11. 已对齐：下载入口在启用自定义命令后，显式展示命令模板卡片并隐藏普通下载选项（Audio/Video/Playlist 类型、格式选择、附加设置）。
  - 证据：[DownloadOptionsSheet 中 isCustomCommandMode 分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/DesktopDownloadSettingsSheet.kt#L205)、[CustomCommandModeSection 组件](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/DesktopDownloadSettingsSheet.kt#L287)。
  - Android 对照：Android 命令模式下禁用格式选择并切换为模板区域 [Android 命令模式 UI](../app/src/main/java/com/junkfood/seal/ui/page/download/DownloadSettingsDialog.kt#L257)，Desktop 当前行为已等价。
12. 已对齐（应用内）：自定义命令运行中任务已具备覆盖层可见性与动作入口（查看日志、取消任务）。
  - 证据：[通知覆盖层组件](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandNotificationOverlay.kt#L36)、[在自定义命令页接入覆盖层](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandScreen.kt#L230)。

#### 3.7.3 仍待处理缺陷（按严重级）
1. ~~P1（增强项）：通知生命周期~~（已完成）：自定义命令任务现已发送 started/completed/error 三阶段系统通知；系统通知交互动作（cancel/retry 按钮）因跨平台 JVM 限制不可实现，由应用内覆盖层替代。
  - 证据：[started 通知](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L119)、[覆盖层](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandNotificationOverlay.kt#L36)。
2. ~~P1（增强项）：任务恢复语义~~（已完成）：Running -> Interrupted 状态独立区分，不再与用户主动 Canceled 混淆；Interrupted 任务显示专属状态标签并提供 Restart chip。
  - 证据：[Interrupted 状态定义](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt#L28)、[存储层 Interrupted 回写](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskStorage.kt#L41)、[UI canRestart 含 Interrupted](../desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandScreen.kt#L372)。
3. P2：Desktop 设置页与侧边栏日志视图在交互完备度上仍落后 Android。
  - 设置页差距：无帮助入口/使用说明弹窗、无多选批量导出/删除、无跨设置页降级提示（custom_command_enabled_hint）。
  - 侧边栏日志页差距：日志弹窗内无法直接执行取消/重启/复制错误。
  - 侧边栏新任务弹窗差距：不会自动从剪贴板预填 URL，模板 chip 只显示通用文案而非当前模板名。
  - 侧边栏模板编辑弹窗差距：无粘贴辅助、无 shortcuts 插入能力。

#### 3.7.4 本轮自我迭代（文档定版，2026-04-25）
1. 全面对齐代码现状：逐项核对了窗口关闭、任务持久化、通知、模板备份、模板编辑漂移、偏好源统一、下载入口语义 7 个维度。
2. 确认 6 项已从"待处理"转为"已对齐"：窗口关闭保护、任务持久化落盘、模板 templates+shortcuts 导入导出、模板编辑漂移修正、侧边栏共享 settingsState、下载入口命令模式语义。
3. 确认 1 项部分对齐：通知已补应用内运行中覆盖动作 + 完成/失败系统通知，仍缺系统侧进度与动作。
4. 保留 2 项 P1 增强项（通知增强、任务恢复语义升级）和 1 项 P2 UI 完备度差距。
5. 将 3.7.2 从 3 项扩展为 12 项，3.7.3 从 8 项压缩为 3 项。

#### 3.7.5 后续建议（下一轮）
1. 补齐自定义命令通知：至少增加运行中进度通知与可点击动作入口。
2. 将任务恢复语义从 Running -> Canceled 升级为可选的继续/恢复能力，或明确定义为只保留历史快照。
3. 为设置页、侧边栏页和下载入口补回归用例。

## 4. 可废除后端逻辑清单

### 4.1 立即可删（低风险）（已完成）
- 项：~~Android 占位空壳 VideoInfo 文件。~~
- 结论：已从代码库中彻底删除。

### 4.2 条件下线（中风险）（已完成）
- 项：旧下载后端链路（Downloader 传统下载分支）以及相关的旧 UI 组件。
- 结论：已彻底从代码库中删除，包括废弃的 `DownloadPage`、`SharedDownloadPane`、`HomePageViewModel`，以及 `Downloader.kt` 中的遗留核心下载函数（`getInfoAndDownload`, `downloadVideoWithInfo` 等），且保留了为“自定义命令”复用的状态逻辑。

### 4.3 必须保留（高风险）
- 项：Downloader 中自定义命令任务状态与通知回退路径。
- 证据：
  - [TaskList 使用命令任务状态](../app/src/main/java/com/junkfood/seal/ui/page/command/TaskListPage.kt#L136)
  - [TaskList 触发命令执行](../app/src/main/java/com/junkfood/seal/ui/page/command/TaskListPage.kt#L217)
  - [TaskLog 读取命令任务日志](../app/src/main/java/com/junkfood/seal/ui/page/command/TaskLogPage.kt#L59)
  - [命令执行写入任务状态](../app/src/main/java/com/junkfood/seal/util/DownloadUtil.kt#L486)
  - [通知取消失败时回退到 Downloader](../app/src/main/java/com/junkfood/seal/NotificationActionReceiver.kt#L60)
- 结论：当前仍在运行链路中，收尾阶段不可删除。

## 5. 为避免误判：Desktop 已具备的后端能力
- 队列与历史持久化（三后端）：[三后端开关定义](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageBackend.kt#L3)、[队列三后端读写分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadQueueStorage.kt#L85)、[历史三后端读写分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L195)、[设置三后端读写分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopAppSettingsState.kt#L93)
- SQLite 持久化：`DesktopSqliteStorage` 覆盖 queue/history/app-settings 三表，含 JSON 引导导入与 schema 版本管理 — [SQLite 存储实现](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopSqliteStorage.kt#L16)
- 原子写与损坏隔离：`writeTextAtomically` + `quarantineCorruptedFile` — [原子写](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopFileIo.kt#L20)、[损坏隔离](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopFileIo.kt#L42)
- 结构化事件日志：`DesktopStorageEventLogger` 覆盖 SQLite 初始化、JSON 引导导入、解析失败、回退读取、损坏隔离 — [事件日志](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageEventLogger.kt#L17)
- 自检验证：Gradle 任务 `desktopStorageSelfCheck` 支持三后端定向验证与 Dual 模式损坏回退 — [自检入口](../desktop/src/main/kotlin/com/junkfood/seal/desktop/storage/DesktopStorageSelfCheckMain.kt#L62)、[Gradle 任务注册](../desktop/build.gradle.kts#L55)
- 历史导入导出和 Android 历史兼容解析：[编码](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L120)、[解码](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/history/DesktopDownloadHistory.kt#L126)
- 进度行解析与暂停继续动作：[进度解析入口](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L774)、[解析函数](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L810)、[暂停](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L234)、[继续](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L572)
- 并发调度与关闭保护：[并发限流门禁](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L224)、[窗口关闭拦截](../desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt#L190)

## 6. 收尾优先级建议
1. ~~P1：通知动作闭环、自定义命令通知增强（进度/动作）、自定义命令任务恢复语义升级。~~（已完成，见 3.5 和 3.7.3）
2. P2：自定义命令设置页/日志页交互补齐、跨端备份 JSON 格式规范统一、旧下载后端链路按条件下线并做结构清理。

## 7. 最小回归清单
1. Android 下载主流程与并发行为。
2. Android 自定义命令任务列表、日志、取消路径。
3. Desktop 下载并发调度、暂停继续、历史导入导出。
4. 通知相关动作链路（取消、完成、错误）。
5. 模板与快捷方式导入导出跨端可读性。
6. Desktop 自定义命令：设置页开关生效、模板选择同步、侧边栏任务重启/日志复制。
7. Desktop 自定义命令：编辑非当前模板/删除非当前模板时，当前模板不漂移。
8. Desktop 自定义命令：通知链路、重启恢复、最新偏好生效。
9. Desktop 自定义命令：下载入口在启用自定义命令后，显式展示命令模板卡片并隐藏普通下载选项（Audio/Video/Playlist 类型、格式选择、附加设置）。

## 8. 模块边界基线（并入原 module-boundaries）

### 8.1 shared/commonMain 放置原则
- 可以放：跨端模型、纯业务逻辑、Compose Multiplatform 共享 UI。
- 禁止放：Android 平台 API（Room/MMKV/Service/Intent/ContentResolver/Lifecycle 等）。
- 证据：
  - [shared Compose 依赖](../shared/build.gradle.kts#L42)
  - [共享下载队列 UI](../shared/src/commonMain/kotlin/com/junkfood/seal/ui/download/queue/DownloadQueueScreenShared.kt#L1)

### 8.2 app/desktop 分工
- app：Android 任务编排、通知/服务、数据库与平台集成。
- desktop：桌面执行器、文件系统、桌面交互与平台适配。
- 依赖方向保持 `app/desktop -> shared`，禁止 shared 反向依赖平台模块。

## 9. 迁移路线现状（并入原 download-migration-roadmap）

### 9.1 已完成
1. Desktop 下载并发调度（`Semaphore(3)` + `withPermit`）。
  - [并发上限](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L53)
  - [并发门禁](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L224)
2. 队列 Resume/Cancel 链路。
  - [Resume 入口](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L572)
  - [Cancel 入口](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L234)
3. yt-dlp 进度解析与展示链路。
  - [进度更新入口](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L774)
  - [ETA/速度解析](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt#L810)
  - [共享队列进度显示](../shared/src/commonMain/kotlin/com/junkfood/seal/ui/download/queue/DownloadQueueScreenShared.kt#L544)
4. 下载入口命令模式接线。
  - [命令模式分支](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/DesktopDownloadSettingsSheet.kt#L197)
  - [执行命令任务](../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadScreen.kt#L425)

### 9.2 待推进
1. 参数一致性校验（Desktop CLI 与 Android `buildCommand()` 对照）。
2. ~~通知动作闭环与自定义命令通知增强。~~（已完成：started/completed/error 系统通知 + 应用内覆盖层动 作）
3. ~~自定义命令恢复语义升级（Running -> Canceled 之外的恢复策略）。~~（已完成：Running -> Interrupted 状态独立区分）
4. 跨端备份 JSON 格式规范与 Android `Backup.kt` 对齐（P2）。
5. Desktop 打包体积优化：已完成 3 级（Full/Lite/Min）打包策略，提供带工具完整版、无工具内置 JRE 精简版、纯 JAR 极限版，按需分发解决体积问题。
