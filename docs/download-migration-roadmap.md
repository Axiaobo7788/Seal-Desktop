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
    - [x] 定义跨平台队列模型：`DownloadQueueItemState`（标题/作者/缩略图/时长/大小/进度/状态/错误/文件路径）、`DownloadQueueState`（集合、过滤器、视图模式、选中项），`DownloadQueueAction`（Cancel/Resume/Delete/OpenFile/CopyURL/OpenURL/OpenThumbURL/CopyError/ShareFile/ShowDetails 等）。
    - [x] 抽取共享队列 UI：从 DownloadPageV2 拆出过滤条、SubHeader、网格/列表切换、空态、卡片/列表项、动作面板内容，全部改为纯回调，参数化窗口宽度，不依赖 `LocalWindowWidthState`/Android 资源。
    - [x] Android 适配：`Task` + `Task.State` → 共享模型，回调转发到 `DownloaderV2`/文件/分享/剪贴板/URL 打开；DownloadDialog 维持原逻辑。
    - [x] desktop 适配：基于 `DownloadPlanExecutor` 维护最小队列状态（Idle/Running/Completed/Error），接入共享 UI；取消/删除/打开文件按桌面能力实现或暂留空。
  - [ ] 验收：Android 队列功能不回归；desktop 可展示/更新任务，空态与切换正常；shared/commonMain 无平台依赖且编译通过。

  #### 9.1a Desktop-first 可先做的工作
    - [x] 细化 desktop 队列模型到执行器：为 `DownloadPlanExecutor` 暴露任务 ID、状态、进度、可取消句柄，形成 `DesktopQueueItem`（含 stdout/stderr 日志截断、错误码）。
    - [x] 桌面 UI MVP：在 `:desktop` 引入轻量队列页（列表视图 + 空态），仅依赖共享模型 + 回调，动作最小化为 Cancel/RevealInFolder/CopyError。
    - [x] 路径与日志策略：确认 cookies/archive/temp 路径与日志文件位于 desktop 独立目录，添加文档与配置开关。
    - [ ] 参数一致性校验：在 desktop 增加 DownloadPlan → yt-dlp CLI 的组装日志（与 Android `buildCommand()` 对比），并可选添加 snapshot 测试。
    - [x] 进阶动作占位：为未实现的桌面动作（Resume/Delete/OpenURL/ShareFile）预留回调接口，UI 按钮禁用或隐藏，避免阻塞先行交付。

  ### 9.2 当前进度
    - 9.1 全部完成，Android/desktop 已接入共享队列 UI 与模型。
    - 9.1a 已完成除“参数一致性校验”外的条目。

  ---

  ## 10. 设置（Settings）迁移（下一阶段）

  目标：把与平台无关的设置模型与页面 UI 下沉到 shared，Android/desktop 通过适配层接入，平台差异通过回调/数据源隔离。

  ### 10.1 待办拆解
  - [ ] 定义跨平台设置模型：`SettingsSection`、`SettingsItem`（开关/下拉/输入/导航/说明）与 `SettingsState`。
  - [ ] 抽取共享设置 UI：列表/分组/搜索（若有）、开关/下拉/输入控件封装为纯回调组件。
  - [ ] Android 适配：从 Preferences/MMKV/Store 读写映射到 shared 模型；与系统权限/Intent/文件选择等平台能力通过回调桥接。
  - [ ] desktop 适配：从本地配置/文件读写映射到 shared 模型；桌面专用项（路径、执行器、代理等）用平台层注入。
  - [ ] 验收：Android 设置功能不回归；desktop 可用；shared/commonMain 无平台依赖且编译通过。

  ---

  ## 11. Desktop 端功能差距清单（对齐 Android）

  目标：在不影响现有 desktop MVP 的前提下，补齐 Android 端主要能力与交互路径。

  ### 11.1 导航与页面（高优先级）
  - [x] 自定义命令页（CustomCommand）：
    - 展示模板列表、创建/编辑/删除。
    - 支持“启用/禁用”与快速套用到下载任务。
    - 与现有设置页 `customCommand*` 字段打通。
  - [x] 赞助页（Sponsor）：
    - 已从 Desktop 主导航移除，入口仅保留在设置页。

  ### 11.2 下载队列（高优先级）
  - [ ] 详情弹窗：
    - 展示参数、日志尾部、输出路径、文件大小、退出码。
  - [ ] Resume 支持：
    - 若不支持，需在 UI 禁用并提示（避免当前“无反馈”的点击）。
  - [ ] 进度解析与显示：
    - 解析 yt-dlp 输出的进度/速度/ETA，更新 `DownloadQueueItemState`。
    - 与 shared 队列组件联动。
  - [x] 软暂停（Desktop）：
    - Cancel 语义调整为“暂停”，Resume 语义调整为“继续”。
    - 覆盖按钮支持暂停/继续切换，不再需要打开详情。

  ### 11.3 格式与播放列表（中优先级）
  - [ ] 自定义格式选择：
    - 拉取并展示可用格式列表（含音/视频/分辨率/码率）。
    - 支持多选或最佳规则选择，写回 `DownloadPreferences`。
  - [ ] 播放列表条目选择：
    - 拉取 playlist 条目、支持批量勾选与范围下载。
    - 将选择映射为 `DownloadPlan`（多任务队列）。

  ### 11.4 执行器与一致性校验（中优先级）
  - [ ] 参数一致性校验：
    - desktop 端输出完整 CLI 日志（与 Android `buildCommand()` 对比）。
    - 可选 snapshot 测试或 golden log 比对。
  - [ ] 多任务并发：
    - 从单一 `runningItemId` 扩展到并发队列与限流。

  ### 11.5 体验与回归（低优先级）
  - [ ] 打开目录/文件体验：
    - Windows/macOS/Linux 差异适配。
  - [ ] 错误可读性：
    - 错误码/错误日志归因与提示。

  ### 11.6 验收
  - [ ] Desktop 下载队列：支持至少 3 个并发任务、可取消、可打开文件/目录。
  - [ ] 自定义命令页：可创建模板并成功下发到下载。
  - [ ] 详情弹窗：展示参数与日志尾部。
  - [ ] 自定义格式选择：支持多格式与分辨率筛选。

  ---

  ## 12. UI 对齐清单（Android → Desktop）

  目标：以 Android 端视觉为准，逐项对齐桌面端 UI。

  ### 12.1 下载设置弹窗
  - [x] 基础结构：标题/下载类型/格式选择/附加设置/底部按钮。
  - [x] 预设“三点”点击弹出编辑对话框（非展开列表）。
  - [x] 预设描述文案与样式对齐（如“首选 质量优先，最高画质”）。
  - [x] 预设编辑弹窗的选项解释文案与布局对齐。
  - [x] 预设区分“自定义/内置”的视觉层级（标签或副标题）。
  - [x] 附加设置 Chip 的高度、圆角、间距与 Android 对齐。
  - [x] 底部按钮的主次样式与宽度对齐（主按钮填充/次按钮描边）。
  - [x] “更多设置”展开/收起样式与动画节奏对齐。

  ### 12.2 下载队列与详情面板
  - [x] 覆盖按钮支持暂停/继续并优先响应。
  - [x] 媒体信息补全：视频/音频格式、码率、分辨率、大小、提取器。
  - [x] 详情面板样式对齐：标题区/主按钮区/分组间距。
  - [x] 空态插画与文案对齐。
  - [x] 队列卡片标题与副标题字号/行高对齐。
  - [x] 进度条高度、圆角与进度文字对齐。
  - [x] 列表/网格切换按钮的选中态样式对齐。
  - [x] “已暂停/失败/完成”状态颜色与提示文案对齐。

  ### 12.3 历史与设置页
  - [x] 历史页卡片样式、筛选条、导入导出对话框对齐。
  - [x] 设置子页卡片间距与分组样式对齐。
  - [x] 设置页顶部标题与返回按钮样式对齐。
  - [x] 开关/下拉/输入类控件的行高与间距对齐。

  ### 12.4 下载动作面板（ActionSheet）
  - [x] 主操作按钮行（下载/复制链接/分享）顺序与间距对齐。
  - [x] 图标尺寸、文字字号与按钮高度对齐。
  - [x] 分组分割线/留白与 Android 对齐。
  - [x] 详情信息区的键值布局与对齐方式一致。
  - [x] 错误信息区块样式与“复制错误”按钮对齐。

  ### 12.5 自定义命令页
  - [x] 列表项结构（标题/描述/开关）与 Android 对齐。
  - [x] 新建/编辑对话框字段顺序与说明文案对齐。
  - [x] 删除确认弹窗与危险操作提示对齐。

  ### 12.6 验收
  - [ ] 下载设置弹窗：与 Android 截图对比，整体布局与间距误差 < 5%。
  - [ ] 下载队列与动作面板：按钮顺序与样式一致，状态色一致。
  - [ ] 历史与设置页：顶部与列表区域样式一致。

  #### 12.x 验收概览（当前）
  - 已完成：12.1 / 12.2 / 12.3 / 12.4
  - 待完成：12.6（截图验收）
