# Download 业务渐进迁移路线图（app → shared）

目标：在不影响 Android 端稳定性的前提下，把 **可复用、可测试、无平台依赖** 的“真正业务逻辑”逐步下沉到 `:shared`（`commonMain` 为主）。

> 说明：当前仓库已经做到 `:app` / `:shared` / `:desktop` 能构建通过；本文是下一阶段的“迁移清单 + 验收方式”。

---

## 0. 当前已稳定的边界（作为基线）

### shared（已在 commonMain）
- 下载相关模型（唯一来源）：
  - `VideoInfo / Format / PlaylistResult`：shared/src/commonMain/kotlin/com/junkfood/seal/util/VideoInfo.kt
  - `DownloadPreferences`：shared/src/commonMain/kotlin/com/junkfood/seal/util/DownloadPreferences.kt
- 下载“纯计算/纯映射”能力（commonMain，无平台依赖）：
  - `DownloadPlan / buildDownloadPlan(...)`：shared/src/commonMain/kotlin/com/junkfood/seal/download/
  - 自定义格式选择页的偏好合成：shared/src/commonMain/kotlin/com/junkfood/seal/download/SelectionMerge.kt
  - Playlist 条目 viewState 映射：shared/src/commonMain/kotlin/com/junkfood/seal/download/PlaylistSelectionMapper.kt
  - 自定义命令参数计划：shared/src/commonMain/kotlin/com/junkfood/seal/download/CustomCommandPlan.kt

### app（Android-only，保持不动或最后再动）
- 执行与平台集成：
  - `YoutubeDLRequest` 的构建与执行、取消（依赖 youtubedl-android）
  - Cookie DB 读取（WebView Cookie SQLite）
  - 下载落盘、SD 卡移动、通知、前台服务、Room/DatabaseUtil
- 下载任务编排：`Task`（明确只在 app）

---

## 1. 迁移原则（避免“搬家就炸”）

1) **先迁“纯逻辑”再迁“执行层”**
- 纯逻辑：根据 `DownloadPreferences` + 选中的 `Format` + `VideoInfo`，算出需要的命令参数、输出模板、排序器、字幕语言等。
- 执行层：把“参数/计划”交给 app（Android）或 desktop（JVM）去执行。

2) `:shared/commonMain` 禁止：AndroidX / Room / OkHttp / MMKV / Compose / youtubedl-android / 文件系统 API / Notification

3) 所有迁移必须可回滚：每个阶段以“新增 shared 能力 + app 侧改为调用”作为最小步。

---

## 2. 代码地图（当前业务逻辑主要集中点）

### app 端核心
- 下载与命令拼装（大头）：app/src/main/java/com/junkfood/seal/util/DownloadUtil.kt
  - 逻辑混合：参数拼装（可迁） + Android/YoutubeDL 执行/文件/DB（不可迁）
- 任务编排：
  - app/src/main/java/com/junkfood/seal/download/Task.kt
  - app/src/main/java/com/junkfood/seal/download/TaskFactory.kt
  - app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt

### shared 端现状
- 已有 `com.junkfood.seal.download.*` 的 shared 实现，但**仅包含纯数据/纯映射**（DownloadPlan/Factory/SelectionMerge 等），不包含执行层。

---

## 3. 第一批“高收益、低风险”可迁移项（建议从这里开始）

这些都满足：不需要 Android API、不需要 youtubedl-android 类型。

### 3.1 纯字符串/纯规则工具函数
候选（当前在 app util，且逻辑纯）：
- `connectWithDelimiter(...)`（拼接排序器字符串）
- `String.isNumberInRange(...)`（限速数值校验）
- `String?.toHttpsUrl()`（缩略图 URL 归一）

目标：迁到 `shared/commonMain` 的 util（例如 `shared/src/commonMain/kotlin/com/junkfood/seal/util/TextUtil.kt`），并让 app 继续可用。

验收：
- `./gradlew :shared:check`
- `./gradlew :app:assembleDebug`

### 3.2 “FormatSorter/排序器”纯逻辑
当前实现集中在：app/src/main/java/com/junkfood/seal/util/DownloadUtil.kt
- `DownloadPreferences.toFormatSorter()`
- `toVideoFormatSorter()` / `toAudioFormatSorter()`

问题点：这段逻辑依赖一批 `PreferenceUtil` 里的 **Int 常量**（如 `FORMAT_COMPATIBILITY`、`FORMAT_QUALITY`、`CONVERT_MP3` 等）。

迁移建议：
- 方向 A（推荐）：把这些 Int 常量升级为 shared 中的 `enum class`（更可读、可扩展），app 继续用 Int 存储，但读取时映射到 enum。
- 方向 B（保守）：把 Int 常量移动到 shared（commonMain），app 继续引用；缺点是常量语义仍“弱类型”。

验收：同上。

---

## 4. 第二批“中收益、中风险”可迁移项（需要设计一个共享抽象）

### 4.1 把“yt-dlp 参数拼装”从 `YoutubeDLRequest` 解耦
现状：`DownloadUtil` 直接对 `YoutubeDLRequest` 调用 `addOption(...)`，导致逻辑与执行层绑死。

可迁移的核心其实是：
- 依据 `DownloadPreferences`、`VideoInfo`、用户选择（formatId、字幕语言、clip、splitByChapter、outputTemplate 等），生成一组参数（`List<String>`）与输出模板（`-o` 的值）

建议在 shared 定义一个纯数据计划（已有骨架 `DownloadPlan / YtDlpOption / DownloadPlanBuilder`）：
- `data class DownloadPlan(val options: List<YtDlpOption>, val outputTemplate: String, val downloadPathHint: String? = null, ...)`
- sealed `YtDlpOption`（Flag/KeyValue），最终可 flatten 成 `List<String>`

然后：
- shared：实现 `DownloadPreferences` + 选择信息 → `DownloadPlan` 的纯映射（只生成参数/模板，不含路径）；
- app：`DownloadPlan -> YoutubeDLRequest`（Android-only adapter）；
- desktop：`DownloadPlan -> ProcessBuilder` 或其他执行器（JVM adapter）。

从 `DownloadUtil` 中可拆出来的子块（强烈建议先做“只拆参数”）：
- `applyFormatSorter(...)`
- `addOptionsForVideoDownloads(...)`
- `addOptionsForAudioDownloads(...)`
- 输出模板选择（默认/ID/clip/chapter/split）
- 字幕/自动字幕/翻译字幕/convert-subs 的决策树
- sponsorblock / rate limit / proxy / ipv4 / restrict-filenames 的参数规则
- 自定义命令参数拼装（已下沉为 CustomCommandPlan）

不能迁（留在 app）：
- cookies 文件路径（`context.getCookiesFile()`）
- download-archive 文件路径（`context.getArchiveFile()`）
- crop artwork 写 config 文件
- 真正执行 `YoutubeDL.getInstance().execute(...)`

验收：
- 先做功能等价：比较迁移前后 request.buildCommand() 输出（可在 debug 日志对比）。
- `./gradlew :app:assembleDebug` + 手动下载一条视频/音频回归。

### 4.2 desktop 执行适配补充
- 在 desktop 模块提供 `DownloadPlanExecutor`（封装 ProcessBuilder），处理取消、stdout/stderr 捕获、退出码映射。
- desktop 自己的 cookies/archive/temp 目录策略（不要复用 Android 路径）。
- 记录日志以便与 app 端参数对比，确保行为等价。

现状（main）：
- `YtDlpFetcher`：desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/YtDlpFetcher.kt
- `DownloadPlanExecutor`：desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DownloadPlanExecutor.kt
- `DesktopYtDlpPaths`：desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopYtDlpPaths.kt
- `YtDlpMetadataFetcher` + 最小可用 UI：desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt

---

## 5. 第三批“任务编排逻辑”迁移方向（取舍要明确）

### 5.1 TaskFactory 的“计算部分”可下沉
当前 `TaskFactory.createWithConfigurations(...)` 里有大量纯计算：
- 计算 fileSize
- 计算 `formatIdString`（join formats）
- 判断 audioOnly / mergeAudioStream
- 合成 subtitleLanguage（selectedSubtitles + autoCaptions）
- 生成新的 `DownloadPreferences`（通过 copy）

但它目前直接调用 `DownloadPreferences.createFromPreferences()`（Android-only 适配入口）

迁移建议（已落地）：
- shared 提供纯函数：
  - `SelectionMerge.merge(...)`：合成 `formatIdString`、subtitleLanguage、mergeAudioStream、extractAudio、clips 等
  - `PlaylistSelectionMapper.map(...)`：把 playlist 选中条目映射为 UI 需要的轻量 view 数据
- app 保留 Android-only 入口：
  - `val base = DownloadPreferences.createFromPreferences()`
  - 然后调用 shared 的纯函数完成合成

验收（已通过）：
- `./gradlew :shared:check`
- `./gradlew :app:assembleDebug`
- shared 单元测试：
  - shared/src/commonTest/kotlin/SelectionMergeTest.kt
  - shared/src/commonTest/kotlin/PlaylistSelectionMapperTest.kt

### 5.2 DownloaderV2/Task 状态机建议仍留在 app
原因：它强依赖 Android（通知/服务/持久化备份/并发控制 + Compose state）。
后续如果要共享“状态机逻辑”，建议先把状态机抽成纯 reducer（shared），再在 app/desktop 做 side-effect 执行；但这属于更后期工作。

---

## 6. 建议的迁移节奏（每一步都可单独 PR）

1) shared: 迁移纯工具函数（connectWithDelimiter/isNumberInRange/toHttpsUrl）
2) shared: 迁移 FormatSorter 规则（连同常量/enum 方案）
3) shared: 引入 `DownloadPlan`（仅计划数据，不执行）
4) app: 把 `DownloadUtil` 的参数拼装改用 shared 的 plan（执行层仍在 app）
5) shared: 抽出 TaskFactory 的“preferences 合成/formatId 计算”
6) desktop: 用同一套 plan 做 desktop 的执行适配（如果你们希望 desktop 也支持下载）

---

## 7. 验收命令（每次迁移都建议跑）

- `./gradlew :shared:check`
- `./gradlew :app:assembleDebug`
- `./gradlew :desktop:packageReleaseDistributionForCurrentOS`

---

## 8. 迁移记录（留空，供你逐步填）

- [x] Step 1：工具函数迁移（main）
- [x] Step 2：FormatSorter 迁移（main）
- [x] Step 3：DownloadPlan 引入（main）
- [x] Step 4：app 侧适配替换（main）
- [x] Step 5：TaskFactory 计算下沉（main：SelectionMerge + PlaylistSelectionMapper + tests）
- [x] Step 6：desktop 侧执行适配（main：YtDlpFetcher + DownloadPlanExecutor + 最小 UI 验证）
  - 额外：自定义命令参数计划下沉（CustomCommandPlan + tests）

  ## 9. 下载队列共享化（新增规划）

  - 范围：将 Android DownloadPageV2 的队列观感下沉为 shared 纯 Compose 组件和平台无关状态模型，Android/desktop 通过适配层接入。
  - 约束：commonMain 禁用 Android API（通知、Intent、UriHandler、Clipboard、FileUtil、LocalView 等）；仅保留纯 UI 与回调意图。

  ### 9.1 待办拆解
  - [ ] 定义跨平台队列模型：`DownloadQueueItemState`（标题/作者/缩略图/时长/大小/进度/状态/错误/文件路径）、`DownloadQueueState`（集合、过滤器、视图模式、选中项），`DownloadQueueAction`（Cancel/Resume/Delete/OpenFile/CopyURL/OpenURL/OpenThumbURL/CopyError/ShareFile/ShowDetails 等）。
  - [ ] 抽取共享队列 UI：从 DownloadPageV2 拆出过滤条、SubHeader、网格/列表切换、空态、卡片/列表项、动作面板内容，全部改为纯回调，参数化窗口宽度，不依赖 `LocalWindowWidthState`/Android 资源。
  - [ ] Android 适配：`Task` + `Task.State` → 共享模型，回调转发到 `DownloaderV2`/文件/分享/剪贴板/URL 打开；DownloadDialog 维持原逻辑。
  - [ ] desktop 适配：基于 `DownloadPlanExecutor` 维护最小队列状态（Idle/Running/Completed/Error），接入共享 UI；取消/删除/打开文件按桌面能力实现或暂留空。
  - [ ] 验收：Android 队列功能不回归；desktop 可展示/更新任务，空态与切换正常；shared/commonMain 无平台依赖且编译通过。

  ### 9.2 当前进度
  - 开始执行 9.1 前两项（模型定义与共享 UI 提炼）。
