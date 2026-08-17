# Seal Desktop 项目自查报告（2026-06-15）

> 当前状态（2026-08-17）：本文已合并 `project-map`、`android-desktop-progress-tracker` 与后续自查结论，作为当前唯一行动清单。`project-map` 保留为项目导航，`android-desktop-progress-tracker` 保留为 2026-04 历史基线，不再代表当前剩余缺口数量。
>
> 规范边界（2026-08-12）：本文只记录缺陷、优先级、进度和验证证据。代码操作、模块边界、多语言、依赖来源和 Definition of Done 以根目录 `AGENTS.md` 与 `docs/development-guidelines.md` 为准。

## 结论先读

初始审计没有改业务代码，只做了跨模块自查、命令验证和风险归类。当时 `:desktop:compileKotlin` 通过，`desktopStorageSelfCheck` 的 `json`、`dual`、`sqlite` 三种后端也都通过，说明项目不是“整体不稳”，而是集中存在几类可拆修的问题：Windows installer 启动链、下载参数与设置落地、Desktop/Android UI 语义对齐、i18n 资源覆盖、CI/release 可复现性。

建议先修 P0/P1，因为它们会直接造成用户可见错误或 release 风险。

## 2026-08-17 release shrink 与 SQLite 平台裁剪

本轮确认 Compose Desktop 生成的 release 规则本身包含 `-dontobfuscate`，因此当前安全模式是“启用 shrink、禁用 obfuscation、禁用 optimizer”，不是旧文档所写的 shrink + obfuscation。直接 shrink 会移除 sqlite-jdbc 的服务加载/JNI 回调类；完整 sqlite-jdbc 又携带 24 组非当前平台原生库，造成无效体积。

- [x] `desktop:platformSqliteJdbc` 在依赖进入 Desktop runtime 前重打包，只保留当前构建机对应的 `Windows|Mac|Linux / x86_64|aarch64` 原生库；Linux x64 本机由约 13.5 MiB 降为 785 KiB。
- [x] ProGuard 保留 `org.sqlite.**`，继续使用 `-dontoptimize`；Windows、Linux、macOS 的普通 release 默认都启用 shrink，Windows debug launcher 模式仍显式关闭 shrink 以便排障。
- [x] packaged smoke 支持强制 `SEAL_DESKTOP_STORAGE_BACKEND=sqlite` 和隔离状态目录，并要求进程存活、`seal/seal.db` 非空且日志没有 JDBC/JNI 失败；安装器不会预建数据库。
- [x] Windows Lite/Full staged app-image 与静默安装后的 EXE、macOS Intel/arm64 Lite/Full app 与安装后 PKG、Linux Lite/Full app-image 和解包后的 Lite DEB 都已接入同一 SQLite 首启检查。
- [ ] 上述 Windows、macOS、Linux DEB 原生 CI 路径仍需在本次修改提交后的 Actions 运行中复验；本机只完成 Linux app-image，不能替代其他系统证据。

本轮验证：

- 通过：`./gradlew :desktop:platformSqliteJdbc :desktop:test --tests com.junkfood.seal.desktop.storage.DesktopStoragePathsTest :desktop:compileKotlin --stacktrace`。
- 通过：`./gradlew :desktop:test :desktop:compileKotlin --stacktrace`。
- 通过：`desktopStorageSelfCheck` 的 `json`、`dual`、`sqlite` 三个后端；SQLite 模式创建非空 `seal.db`。
- 通过：`./gradlew :desktop:createReleaseDistributable --stacktrace`，当前 Linux Lite app-image 约 133 MiB。
- 通过：Linux x64 shrunk app-image SQLite smoke，进程存活 12 秒并创建 24576-byte `seal.db`。
- 通过：`bash -n .github/scripts/smoke_unix_app_image.sh`、Windows PowerShell parser、`go run github.com/rhysd/actionlint/cmd/actionlint@latest`。
- 本机环境限制：`:desktop:packageReleaseDistributionForCurrentOS` 的 DEB 任务被本地 JDK `jpackage` 以 `Invalid or unsupported type: [deb]` 拒绝，最终 DEB 由 Ubuntu Actions 验证。

## 2026-08-14 Linux/macOS package smoke 修复

Actions 日志中的两个失败属于不同阶段：Linux 已完成 app-image 构建，但 ProGuard 7.6.0 optimizer 分别把 `kotlinx.coroutines.JobKt.invokeOnCompletion` 和 Compose Runtime `derivedStateOf` 改写成 JVM 无法验证的字节码；macOS arm64 已完成 `.app` 和 `.pkg` 构建，但 Unix smoke 脚本在 `cd app_root` 后继续执行相对 launcher 路径，导致路径被重复解析。

- [x] Unix app-image smoke 在切换工作目录前规范化 app root 与 launcher 的绝对路径；相对路径假 launcher 已通过 1 秒存活测试。
- [x] Linux/macOS 继续启用 ProGuard shrink；Compose Desktop 的生成规则禁用 obfuscation，项目通过 `-dontoptimize` 禁用会产生 `VerifyError` 的 optimizer；本地 Linux release app-image 已保持运行 12 秒。
- [x] 三平台 workflow 都显式设置 `DESKTOP_RELEASE_PROGUARD=true`，并在普通 release 构建前断言 Gradle 解析结果，避免后续默认值变化导致无意关闭压缩。
- [ ] macOS Intel、macOS arm64 的 app-image 与安装后 PKG smoke 仍需在修复提交后的原生 Actions runner 上复验；本地 Linux 不能替代这项证据。

本轮验证：

- 通过：`./gradlew :desktop:createReleaseDistributable -PdesktopReleaseProguard=true --stacktrace`
- 通过：`.github/scripts/smoke_unix_app_image.sh` 相对路径假 launcher，存活 1 秒。
- 通过：Linux x64 ProGuard release app-image 启动 smoke，存活 12 秒且不再出现 JVM `VerifyError`。

## 2026-07-08 项目整体整理与文档合并

这轮把“继续移植”调整为“带审计的移植”：不再默认把 Android 端所有入口照搬到 Desktop，而是先判断每个字段、页面和后端能力属于核心能力、平台特化、历史兼容还是应删除/隐藏的包袱。

### 文档治理结论

| 文档 | 新角色 | 处理方式 |
| --- | --- | --- |
| `AGENTS.md` | 强制操作入口 | 保留每次代码修改都必须遵守的短规则和最低验证要求。 |
| `docs/development-guidelines.md` | 详细工程规范 | 维护模块、parity、i18n、依赖、存储和验证边界，不记录任务进度。 |
| `docs/desktop-project-audit-2026-06-15.md` | 当前唯一行动清单 | 继续维护未完成项、已完成项、清理矩阵和验证命令。 |
| `docs/project-map.md` | 项目导航和模块地图 | 只保留项目结构、关键链路、当前入口，不再维护细粒度缺陷状态。 |
| `docs/android-desktop-progress-tracker.md` | 2026-04 历史基线 | 保留迁移历史和当时决策，不再作为当前 P1/P2 数量来源。 |
| `docs/android-desktop-backend-defects.md` | 旧入口兼容页 | 新增重定向说明，指向本文，避免旧 IDE 标签和引用失效。 |

### 当前项目画像

- 核心下载链路不是完全失控：Desktop 编译、自检、下载执行、依赖解析、队列/历史/设置存储都有可验证路径。
- 真正拖慢移植的是“边界不清”：`DownloadPreferences` 同时塞 Android-only、Desktop-only 和两端共享字段，导致 Desktop 很容易出现“有设置入口但没有平台语义”的伪功能。
- Android 原项目本身存在历史链路包袱：旧字段、兼容字段、平台 API、UI 组件和业务状态混在一起，Desktop 移植时不能盲目 1:1 复制。
- Desktop 端新增了必要的桌面特化：依赖检测、窗口关闭语义、portable/installer、文件选择器、系统路径、语言资源 provider，这些不应强行模拟 Android。
- 目前最危险的不是“缺功能”，而是“看起来有功能但实际不等价”：下载归档不可管理、播放列表只能整表、privateDirectory 无桌面语义、Cookies profile 与全局 cookies 文件语义不同。

### 代码整理分区

| 分区 | 代表项 | 当前判断 | 下一步 |
| --- | --- | --- | --- |
| 核心能力，必须保留 | yt-dlp plan、ffmpeg/ffprobe、队列、历史、设置、依赖 resolver | 保留并加测试锁行为 | 优先补 CLI 快照、依赖缺失、历史/归档行为测试。 |
| 平台特化，不能强行共用 | Android `libaria2c.so`、Desktop `aria2c(.exe)`、Android SD card、Desktop 文件选择器 | 保留平台 adapter | 共享层只保留语义，不写死平台路径或二进制名称。 |
| 兼容遗留，短期保留 | `customCommandLabel/customCommandTemplate` 单模板字段、JSON/dual storage | 保留迁移读取，不再扩大使用 | 加迁移注释，后续稳定后改成只读兼容或 schema 迁移。 |
| 伪功能，必须修或隐藏 | `privateDirectory`、下载归档管理、播放列表条目选择 | 不允许继续“有入口无兑现” | 短期改文案/禁用/隐藏，中期补完整行为。Desktop app 更新入口已改为手动打开 Releases。 |
| UI 复刻债 | 自定义格式页、字幕弹窗、下载前设置 sheet、历史页多选 | 保留为 parity 任务 | 以截图/录屏 checklist 验收，不只看代码是否存在。 |
| i18n 债 | 硬编码英文/中文通知、文件选择器标题、错误弹窗 | 必须抽资源 | 先扫 Desktop 用户可见字符串，再补 default/zh-rCN/zh-rTW。 |
| 包体/分发债 | aria2c 是否捆绑、latest/nightly 依赖、installer smoke test | 继续按“核心/可选”拆分 | aria2c 保持可选检测；yt-dlp/ffmpeg 逐步 pin provenance。 |

### 清理规则

1. 没有 Desktop 语义的 Android-only 字段，不直接移植 UI；先标注为 Android-only 或 Desktop unsupported。
2. 有 UI 入口就必须有后端兑现；短期做不到时，宁可隐藏、禁用或写清“暂不支持”。
3. shared 层只放平台无关语义；`libaria2c.so`、`aria2c.exe`、SD card、AppData/Library 路径这类内容必须留在 adapter。
4. 兼容旧数据的字段可以保留，但要标注“migration only”，避免新代码继续依赖旧字段。
5. 可选加速器不进默认包；`aria2c` 继续作为 optional dependency，避免 Desktop 包体继续膨胀。
6. 每完成一项修复，必须同步本文的打勾状态和验证命令，避免文档再次漂移。

### 新的整理优先级

1. P0：消除伪入口，包括 Desktop app 自动更新占位、下载归档不可管理、privateDirectory 无语义。
2. P1：收敛共享模型，把 `DownloadPreferences` 分成 core / Android-only / Desktop-only / migration 字段说明，后续再考虑结构拆分。
3. P1：继续自定义格式页 parity，尤其是 `downloadType` 过滤、字幕正则预选、剪切范围编辑。
4. P1：硬编码文案/i18n 扫描，把 Desktop 端通知、错误、文件选择器标题纳入资源。
5. P2：UI 动画用录屏 checklist 验收，避免“代码有动画但手感不对”的回归。
6. P2：清理历史文档和旧 issue 标题，`project-map` 只做导航，本文做行动清单。

## 2026-06-17 迭代状态

本节是后续修复后的状态同步，用来避免已经闭环的问题继续混在待办里。

- [x] SponsorBlock 分类保存实错已修：`GeneralSettingsPage` 现在保存弹窗返回的 `categories`，不再把 `DownloadPreferences.toString()` 写进 `sponsorBlockCategory`。
- [x] 视频/音频/自定义命令下载目录已接入执行层：`DesktopYtDlpPaths.downloadDirectoryFor()` 会根据下载类型使用 `videoDirectory`、`audioDirectory`，自定义命令使用 `commandDirectory`；并已补 `DownloadDirectorySelectionTest`。
- [x] Windows installer 启动链已恢复到可启动状态：installer 版已能启动；debug launcher 改成手动开关，默认 release 不再自带调试 CMD。
- [x] Windows release shrink 已在禁用 optimizer、保留 SQLite runtime 类并增加安装后 SQLite smoke 后恢复默认启用；Compose 生成规则禁用 obfuscation，仍可用 `-PdesktopReleaseProguard=true/false` 或 `DESKTOP_RELEASE_PROGUARD` 手动覆盖。
- [x] 全局展示版本已统一到 `0.0.5`：`buildSrc`、Android versionCode、About 页面展示和 Inno `AppVersion` 已对齐；macOS native package 因 Apple/Compose 要求 `MAJOR > 0`，使用兼容包版本 `1.0.5`。
- [x] Windows 包体积方向已确认：Inno 已开启 `Compression=lzma2/ultra64` 与 `SolidCompression=yes`；workflow 也打印包体大小并对 portable artifact 使用 zip 压缩。
- [x] Desktop aria2c 参数已脱离 Android `.so`：共享 plan 保留 Android 默认 `libaria2c.so`，Desktop 下载队列和自定义命令入口显式传 `aria2c`，并补了共享层覆盖测试。
- [x] yt-dlp 更新卡片已接入真实更新：点击更新会按 Stable/Nightly 下载到 Desktop auxiliary/bin；缺依赖弹窗的 portable 安装也复用同一 channel。更新期间前置图标切成进度圆环，并有进度条/状态行展开收起动画。
- [x] Windows workflow 已加入 Lite/Full app-image smoke test：Inno 打包前会优先运行 staged app-image 的 `.bat` 诊断启动器，若 JVM/主类/运行时有问题会在 CI 日志里失败并打印输出。
- [ ] Inno `[Languages]` 仍依赖 `EmitLanguagesSection`。虽然简中/繁中已本地提供，但“官方语言自动展开”的预处理来源仍不透明，后续建议改成 workflow 生成显式语言列表。
- [ ] UI/动画 parity 还有一批细节没有完全移植，详见“2026-06-17 UI / 动画 parity 复查补充”。

## 2026-07-07 Markdown 校对与 Android parity 复查

本轮重新核对了本文状态与当前代码，确认 Windows installer、版本号、Inno 压缩、aria2c 平台参数、yt-dlp 手动更新这些已完成项可以保留打勾。Windows shrink 策略后来于 2026-08-17 再次调整，以顶部最新结论为准。但“Android 端已有、Desktop 端看起来有入口却没有等价行为”的缺口还不少，主要集中在下面几类：

- [ ] Desktop app 自动更新页目前只有设置项和“Check for updates”占位，`UpdateSettingsPage` 的按钮仍是 `TODO desktop check for update`；Android 则有 `AppUpdater` 启动检查、`UpdateUtil.checkForUpdate()`、下载 APK 和安装流程。Desktop 如果短期不做自动更新，应把 UI 改成“暂不支持/手动下载”，避免假入口。
- [ ] 下载归档在 Android 上能打开 archive 文件、编辑保存、清空；Desktop 现在只有 `useDownloadArchive` 开关，执行层会写 `download-archive.txt`，但用户无法查看/清理。这个不是移动端特有能力，Desktop 也应补一个管理入口。
- [x] aria2c 已作为 Desktop 可选依赖纳入检测：不影响 yt-dlp/ffmpeg 的必需依赖判定，网络页会显示 `selfhost/system/missing` 并在缺失时禁用/自动关闭开关，执行层会在下载前给出可读错误。
- [x] Desktop 无痕模式已接入执行层：下载成功后不再写入历史，退出/恢复用的队列备份也会跳过 private mode 请求，避免跨启动留下 URL。
- [x] `cropArtwork` 已在 Desktop 执行层兑现：音频类下载且开启内嵌元数据、裁剪封面时会注入 Android 等价的 `--ppa ffmpeg ... crop ...`，队列命令预览也会显示该参数。
- [x] 内嵌字幕与 MKV 容器的联动已对齐：shared plan 会在 `downloadSubtitle && embedSubtitle` 时自动追加 MKV remux 参数，Desktop 格式页也会把 MKV 开关显示为已启用且不可关闭。
- [ ] 播放列表下载语义仍弱于 Android：Android 有播放列表条目选择页并把选中 index 传入任务；Desktop 只有 Playlist 下载类型，执行层始终不加 `--playlist-items`，等价于整表下载。
- [ ] 新建任务输入页仍明显弱于 Android：Android 支持剪贴板多链接识别、批量选择、保存链接、保存链接弹窗、滑动删除和列表动画；Desktop 当前 URL 输入框是单行，只有粘贴按钮，没有 saved links UI。
- [ ] Cookies 语义需要重新定：Android 有 Cookie Profile 数据库和 WebView 生成/管理路径；Desktop 目前更像单个 Netscape cookies 文件 + 浏览器提取器。平台差异可以接受，但需要明确“全局 cookies 文件”与“按站点 profile”的取舍，否则从 Android 迁移过来的用户会找不到 profile。
- [ ] 下载历史页 Desktop 已有搜索和导入导出，但 Android 的长按多选、BottomAppBar、批量删除/导出、详情 drawer、搜索 `AnimatedVisibility` 与条目动画仍未对齐。
- [ ] 自定义格式页仍没有把 `downloadType` 传给 `FormatPageImpl` 做 UI 过滤：最终下载时知道类型，但页面仍会同时展示音频/视频/混合格式；音频入口可见视频格式的问题仍存在。
- [ ] 自定义格式页的字幕预选、视频剪切范围、搜索框样式和选中动画仍是 parity 高风险点：Android 支持 `en.*,.*-orig` 正则式字幕偏好和 `VideoSelectionSlider`，Desktop 仍是 exact code 匹配与菜单 toggle。
- [ ] `privateDirectory` 仍是半迁移状态：共享偏好和 Android 路径逻辑存在，但 Desktop 没有 UI，也没有路径语义。需要决定桌面端是否实现隐藏/私有下载目录，或彻底隐藏这个偏好避免误会。
- [ ] 硬编码文案扫描仍能抓到多个用户可见字符串，尤其是格式页加载/错误中文、网络/Cookies 中英混排、通知标题、历史页错误弹窗、Desktop unavailable 占位。

## 历史验证记录

- 通过：`./gradlew :desktop:compileKotlin`
- 通过：`./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=json -PstorageStateDir=/tmp/seal-storage-json`
- 通过：`./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=dual -PstorageStateDir=/tmp/seal-storage-dual`
- 通过：`./gradlew :desktop:desktopStorageSelfCheck -PstorageBackend=sqlite -PstorageStateDir=/tmp/seal-storage-sqlite`
- 通过：`./gradlew :desktop:compileKotlin :shared:allTests`
- 通过：`./gradlew :desktop:test --tests com.junkfood.seal.desktop.ytdlp.DownloadDirectorySelectionTest --tests com.junkfood.seal.desktop.ytdlp.YtDlpMetadataFetcherTest --tests com.junkfood.seal.desktop.download.DesktopDownloadHistoryPrivacyTest :desktop:compileKotlin`
- 编译警告：Kotlin Multiplatform 提示当前 AGP `8.7.2` 高于 Kotlin Gradle Plugin 最大已测试版本 `8.5`。
- 编译警告：Gradle 已有 deprecated feature，未来升级 Gradle 9 前需要 `--warning-mode all` 清理。

## P0/P1：建议优先修

### 1. [x] SponsorBlock 分类保存逻辑是实错

证据：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/general/GeneralSettingsPage.kt:196`

状态：已完成。当前代码在 `onConfirm = { categories -> ... }` 中写入弹窗返回的分类字符串。

历史问题：旧代码在 `onConfirm` 里把内层 `DownloadPreferences` 的 `it.toString()` 写进 `sponsorBlockCategory`，没有写入弹窗返回的分类字符串。结果是打开 SponsorBlock 后，最终 yt-dlp 会收到一串 `DownloadPreferences(...)`，而不是 `sponsor,intro,...`。

后续建议：

- 给 `DownloadPlanFactory` 增一个测试：开启 sponsorBlock 且分类为 `sponsor,selfpromo` 时，CLI 参数必须包含 `--sponsorblock-remove sponsor,selfpromo`。

### 2. [x] 视频/音频下载目录设置没有真正影响执行目录

证据：

- 设置页写入 `DownloadPreferences.videoDirectory/audioDirectory`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/directory/DirectorySettingsPage.kt`
- 执行器只使用 `plan.downloadPathHint`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DownloadPlanExecutor.kt:99`
- `DesktopYtDlpPaths.downloadDirectory(hint)` 目前忽略 `hint`，永远返回默认 Downloads：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopYtDlpPaths.kt:47`

状态：已完成。`DesktopYtDlpPaths` 已拆出 `defaultDownloadDirectory()`、`configuredDownloadDirectory()`、`downloadDirectoryFor(preferences, hint)`；`DownloadPlanExecutor`、自定义命令和目录设置页已经改用新的路径解析，并补了目录选择测试。

历史问题：用户设置“视频目录/音频目录”后，实际下载仍会走默认 Downloads。这个会被用户理解为设置不生效。

后续建议：把目录选择测试继续扩展到实际生成的 yt-dlp 工作目录和输出模板，避免后续改 executor 时回归。

### 3. [x] Desktop 的 aria2c 参数仍是 Android 形态

证据：

- `shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:64` 写死 `--downloader libaria2c.so`
- `shared/src/commonMain/kotlin/com/junkfood/seal/download/CustomCommandPlan.kt:36` 也写死 `libaria2c.so`
- 测试还把 `libaria2c.so` 固化为预期：`shared/src/commonTest/kotlin/CustomCommandPlanTest.kt:30`

状态：已完成主修复。共享 plan 新增 `aria2cDownloader` 参数，默认仍是 Android 需要的 `libaria2c.so`；Desktop 下载队列和自定义命令执行入口统一传入 `aria2c`。已补 `DownloadPlanFactoryTest` 和 `CustomCommandPlanTest` 覆盖“默认 Android 值不变、平台 adapter 可覆盖”。

历史问题：这对 Android 合理，但 Desktop 上通常应该是 `aria2c` 可执行文件或禁用/提示未安装。继续共用会导致 Desktop 勾选 aria2c 后 yt-dlp 找不到 downloader。

后续建议：

- Desktop 依赖检测页仍未报告 aria2c 是否可用。后续可以把 `aria2c` 纳入 `DesktopDependencyResolver`，或在系统找不到 aria2c 时禁用/提示此开关。
- 如果后续想更彻底，可以把共享层参数从 `String` 升级为语义型 `ExternalDownloader.Aria2c`，由 Android/Desktop adapter 统一映射。

### 4. [x] yt-dlp 更新设置目前像“可更新”，实际没有更新能力

证据：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/YtDlpFetcher.kt:28`

`invalidateCachedBinary()` 永远返回 `false`，`version`、`cacheRoot` 参数也只是保留兼容。设置页会给用户一种“点按钮可以刷新/更新 yt-dlp”的感觉，但实际只会重新读取现有依赖状态。

状态：已完成主修复。`YtDlpFetcher.invalidateCachedBinary()` 现在会调用 `DesktopAuxiliaryDownloader.downloadYtDlpBinary()`，按设置页的 Stable/Nightly channel 下载到 Desktop auxiliary/bin；更新后重新解析依赖并展示实际版本。缺依赖弹窗的 portable 安装也接入同一 channel，避免设置页与自动配置策略不一致。

UI/动画状态：已补齐更新期间的进度语义。`ActionWithDividerCard` 支持加载中圆形进度图标，描述文本通过 `AnimatedContent` 切换；`YtdlpUpdateCard` 在更新时展示进度条和最新日志行，并用淡入/高度展开收起动画承接状态变化。

后续建议：

- `ytDlpAutoUpdate` 和 `ytDlpUpdateInterval` 仍只是设置项，还没有后台调度器；后续要么实现启动时/定时检查，要么在 UI 中标注“后续支持”。
- 如果用户把依赖来源强制设为 `system`，应用内更新只会更新 auxiliary/bin，不会更新系统包管理器里的 yt-dlp；后续可以在 System 模式下改成“重新检测/提示使用包管理器更新”。

### 5. 自定义格式页没有把下载类型传入渲染层

证据：

- 外层 `CustomFormatSelectionSheet` 有 `downloadType`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt:142`
- 传入 `FormatPageImpl` 时没有传 `downloadType`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt:181`
- `FormatPageImpl` 总是同时列出 video-only、audio-only、video+audio：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt:298`

这会让“音频下载 -> 自定义格式”仍可选视频格式。即使后端 `preferencesForType` 最后设置了 `extractAudio = true`，UI 选择和队列展示已经出现语义偏差。

建议：

- `FormatPageImpl(downloadType: DesktopDownloadType)`。
- Audio 模式隐藏或弱化视频格式，只展示音频格式和推荐音频。
- Video 模式保持推荐、视频、音频组合逻辑。
- Playlist 模式确认是否允许自定义单视频格式，若不支持应在入口禁用。

2026-07-07 复核：仍未完成。当前 `CustomFormatSelectionSheet` 只在 `startDownloadWithSelection(type = downloadType)` 阶段传入类型，`FormatPageImpl` 本身没有 `downloadType` 参数，页面仍按 `formats.filter { ... }` 直接列出 audio-only、video-only、video+audio 三组。

### 6. 自定义格式页字幕预选不支持 Android 的正则语义

证据：

- 默认字幕设置显示 `en.*,.*-orig`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/subtitle/SubtitleSettingsPage.kt:97`
- 格式页只按逗号拆分并 exact match：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt:306`、`:1430`

Android 侧常用 `en.*,.*-orig` 这类 yt-dlp subtitle pattern。Desktop 格式页如果只 exact match，会导致默认字幕在自定义格式页不自动选中。

建议：

- 抽一个 `matchesSubtitleLanguagePattern(code, pattern)`，支持普通 code 和 regex-like pattern。
- 推荐字幕 chips 和“查看更多”弹窗初始选中都复用同一匹配函数。
- 加测试覆盖 `en`, `en-US`, `en-orig`, `zh-Hans`, `.*-orig`。

### 7. 视频剪切在自定义格式页只有开关，没有范围编辑

证据：

- `clipStartText/clipEndText` 只来自旧 preferences 或默认 `0..duration`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt:349`
- 点击 header 菜单只 toggle `clipVideo`，没有弹出范围选择 UI：同文件 `:449`
- 下载时直接用当前文本构造 clips：同文件 `:398`

如果用户在自定义格式页点“切分/剪切视频”，可能只是开启了整段剪切或复用旧值，行为不透明。

建议：

- 如果本轮只做 Android parity，优先复刻 Android 的 clip range dialog/slider。
- 如果暂不支持范围编辑，则菜单项改成 disabled，并提示去设置页配置。

### 8. [x] Windows portable 可打开而 installer 报错，方向应从安装启动链查起

状态：已完成第三轮修复。2026-07-07 出现“所有 Windows 版本 `Failed to launch JVM`”回归后曾临时关闭 Windows ProGuard；2026-08-17 在禁用 optimizer、保留 SQLite runtime 类并补齐 staged/安装后 SQLite smoke 后恢复默认 shrink。`debug_launcher` 仍是手动开关，默认 release 不自带调试 CMD。

历史现象：portable 可打开，installer 版 `Failed to launch JVM` 且没有弹出调试 CMD。

当前状态：

- 三平台普通 release 默认启用 shrink，Compose 生成规则禁用 obfuscation，项目规则禁用 optimizer；排障时可通过 workflow env 或 Gradle property 覆盖。
- Windows jpackage console 默认关闭，排障时才打开。
- Inno `AppVersion` 已由 Gradle 输出的 Desktop version 注入，不再固定 latest。
- Inno 压缩已确认开启，包体积偏大时应继续看 runtime/内置依赖/ProGuard 产物，而不是先怀疑 Inno 没压缩。

判断：

- portable 能打开，说明 app image/runtime 主体大概率没坏。
- installer 报错更像安装后的 source 布局、shortcut 覆盖、工作目录、安装后运行 flags、旧快捷方式残留或 Inno 复制路径问题。
- “没有弹出 CMD”说明实际启动路径可能不是 Inno 生成的 cmd shortcut，或 `MyAppLaunchThroughCmd` 没在最终 iss 编译中生效。

后续建议：

- 在 Windows workflow 上传 staged installer source 的目录列表和 `app/Seal.cfg` 为 artifact，别只打印日志。
- 如果后续还要保留 Debug shortcut，建议显式安装两个快捷方式：`Seal` 和 `Seal Debug`，避免调试路径污染发行路径。
- 明确卸载旧 shortcut 或改 AppId/shortcut name 测试，排除用户机器上的旧快捷方式残留。
- 安装后 smoke 已静默安装到临时目录，并要求真实 launcher 启动后创建 SQLite 数据库；修复提交后仍需 Windows Actions 复验。

### 9. Inno 多语言脚本依赖不透明

证据：`.github/scripts/windows_setup.iss:34`

脚本里直接使用 `#expr EmitLanguagesSection`，但仓库内没有定义或 include。现在又手动追加简中/繁中 `.isl`。如果 CI 报“找不到语言文件”或预处理失败，根可能不是某个翻译文件缺失，而是 Inno 环境里的 `EmitLanguagesSection` 不稳定。

建议：

- 不依赖 `EmitLanguagesSection`，在仓库脚本中显式生成 `[Languages]`。
- 官方 Inno 语言使用 `{autopf}\Inno Setup 6\Languages\xxx.isl` 或预先探测存在再写入。
- 简中/繁中继续使用仓库内 `.github/scripts/ChineseSimplified.isl`、`ChineseTraditional.isl`。
- Windows workflow 增加一个 “Print Inno languages dir” 步骤，输出实际可用官方语言文件。

## P1：下载后端与依赖检测

### 10. [x] 获取视频信息阶段强制要求 ffmpeg，可能过严

证据：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/YtDlpMetadataFetcher.kt:17`

`fetch()` 只是执行 `yt-dlp -J --no-playlist`，但调用 `ensureDependencies()`，即要求 yt-dlp 和 ffmpeg 都完整存在。用户只是打开格式页/获取 metadata 时，如果缺 ffmpeg，可能提前失败。

状态：已完成。`YtDlpMetadataFetcher.fetch()` 改为调用 `resolveDependencies()`，只在 `ytDlp` 缺失时抛出可读错误；`ffmpeg` 存在时继续附带 `--ffmpeg-location`，不存在时仍可执行 `yt-dlp -J --no-playlist` 获取 metadata。

完成细节：

- metadata fetch 只 require yt-dlp。
- 真正下载、合并、转码前仍由 `DownloadPlanExecutor.requireComplete()` 要求 ffmpeg。
- 已补 `YtDlpMetadataFetcherTest` 覆盖无 ffmpeg 时不注入 `--ffmpeg-location`、有 ffmpeg 时继续注入。

### 11. `--sponsorblock-remove` 对空分类没有兜底

证据：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:81`

如果 `preferences.sponsorBlock = true` 且 `sponsorBlockCategory` 为空，仍会写 `--sponsorblock-remove ""`。结合 P0 的保存 bug，会放大失败概率。

建议：

- 空分类时使用 yt-dlp 推荐默认值或不输出该参数。
- SponsorBlockDialog 至少选中一个分类，否则禁用确认。

### 12. `embedMetadata` 的语义在视频/音频不一致

证据：

- 视频总是 `--add-metadata`：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:119`
- 音频才根据 `preferences.embedMetadata` 决定 `--embed-metadata`、`--embed-thumbnail`：同文件 `:197`

如果 UI 的“内嵌元数据” chip 显示在通用附加设置里，用户会以为视频也受这个开关控制。

建议：

- UI 文案明确“音频元数据/封面”。
- 或把视频 `--add-metadata` 也交给 `embedMetadata` 控制，并另设 `addMetadata` 默认行为。

### 13. [x] `DesktopAuxiliaryDownloader` 与设置页 update channel 没打通

证据：

- 自动下载使用 stable latest：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopAuxiliaryDownloader.kt:85`
- workflow 使用 nightly latest：`.github/workflows/windows_x64_portable.yml:17`
- 设置页存在 `yt-dlp-nightly-builds` 文案：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/YtdlpUpdateCard.kt:192`

状态：已完成应用内主修复。`DesktopAuxiliaryDownloader` 现在有 Stable/Nightly 常量和 URL resolver；`YtdlpUpdateCard` 与 `DesktopEnvironmentSetupDialog` 都会把当前 `ytDlpUpdateChannel` 传给 downloader。Stable 指向 `yt-dlp/yt-dlp`，Nightly 指向 `yt-dlp/yt-dlp-nightly-builds`。

后续建议：

- Workflow 和 Desktop downloader 仍是两处 URL 配置，后续尽量复用同一份 URL 规则或至少写入 build metadata。
- 设置页后续可以继续显示实际来源：Bundled/System/Selfhost + Stable/Nightly + version。

### 13a. Desktop app 自动更新设置目前是占位 UI

证据：

- Android 启动层有 `AppUpdater()`，会根据 `PreferenceUtil.isAutoUpdateEnabled()` 调用 `UpdateUtil.checkForUpdate()` 并显示更新弹窗：`app/src/main/java/com/junkfood/seal/ui/page/AppUpdater.kt`
- Android 设置页手动检查也调用 `UpdateUtil.checkForUpdate()`：`app/src/main/java/com/junkfood/seal/ui/page/settings/about/UpdatePage.kt`
- Desktop 设置页存在 `autoUpdateEnabled/updateChannel`，但 “Check for updates” 的点击仍是 `/* TODO desktop check for update */`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/about/UpdateSettingsPage.kt:105`

影响：Desktop 用户打开 About -> Auto update 会以为已经支持应用更新，但这个开关目前不会触发任何检查、下载或跳转。

建议：

- 短期：Desktop update page 文案改成“暂不支持应用内更新，请到 Releases 下载”，并禁用或隐藏自动更新开关。
- 中期：接入 GitHub Releases 检查，只提示并打开下载页，不自动替换正在运行的 app。
- 长期：按平台实现 installer/package 自更新策略，但 Windows/macOS/Linux 应分别设计，不建议直接照搬 Android APK 安装流程。

### 13b. Desktop 下载归档缺少查看、编辑、清空入口

证据：

- Android general 设置页读取 `context.getArchiveFile().readText()`，并提供 archive 编辑保存弹窗：`app/src/main/java/com/junkfood/seal/ui/page/settings/general/GeneralDownloadPreferences.kt:358`
- Android adapter 在启用 archive 时会预读 archive，并在重复命中时抛 `download_archive_error`：`app/src/main/java/com/junkfood/seal/download/YoutubeDlRequestAdapter.kt:99`
- Desktop general 设置页只有 `download_archive` toggle：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/general/GeneralSettingsPage.kt:169`
- Desktop 执行层实际会在需要时传 `--download-archive download-archive.txt`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DownloadPlanExecutor.kt:156`
- Desktop 下载队列也会拼出 `--download-archive`，但没有等价的重复预检查：`desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt:218`

影响：用户可以开启下载归档，但无法在 UI 中确认已经记录了哪些条目，也无法像 Android 一样清理误记录的 URL。迁移测试时容易被误判为“下载按钮没反应”或“某个链接跳过了但不知道原因”。

建议：

- 在 General 设置的 `download_archive` 卡片上增加 secondary action 或详情页：显示 archive 路径、条目数、打开文件、编辑保存、清空。
- 在 Desktop adapter/controller 中补重复预检查。重复时不要把任务标成普通 Completed，而应给出“已在下载归档中”的可读状态或 snackbar/dialog。
- Desktop 可以额外提供“在文件管理器中显示”。
- 清空前使用确认弹窗，并在清空后刷新条目数。

### 13c. [x] aria2c 可选依赖检测与执行前校验

原问题：

- `DesktopDependencyResolution` 只有 `ytDlp` 和 `ffmpeg`，`complete` 也只检查这两项：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopDependencyResolver.kt:27`
- 网络页可以直接开启 `preferences.aria2c`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/network/NetworkSettingsPage.kt:99`
- 下载 plan 已会在 Desktop 传入 `--downloader aria2c`，但系统不存在 `aria2c` 时只能到执行阶段失败。

约束：根据之前的体积取舍，`aria2c` 仍应作为 Desktop 可选加速器，不建议默认打进完整版包。

状态：已完成。`DesktopDependencyResolution` 新增 optional `aria2c`，但 `isComplete` 仍只要求 `ytDlp + ffmpeg`，避免把可选加速器变成启动硬依赖。`NetworkSettingsPage` 会异步检测并显示 `selfhost/system/missing`，缺失时禁用开关并自动清掉已保存的 aria2c 偏好；`GeneralSettingsPage` 的依赖检测摘要也会显示 aria2c 状态。`DownloadPlanExecutor` 对普通下载和自定义命令都做执行前校验，检测到 `--downloader aria2c` 但缺依赖时会抛出可读错误。

完成细节：

- `DesktopDependencyResolver` 增加 optional `aria2c` 检测，不影响 `complete`。
- 网络页显示 `aria2c: selfhost/system/missing`，missing 时禁用开关并自动关闭已保存配置。
- 执行层如果用户配置了 aria2c 但检测不到，会在任务开始前给出可读错误，而不是让 yt-dlp 失败后透传长日志。

### 13d. Cookies 与 Android Cookie Profiles 不是同一套语义

证据：

- Android 有 `CookieProfilesPage` / `CookiesViewModel` / `WebViewPage`，以数据库里的 `CookieProfile(url, content)` 管理多份 cookies。
- Desktop 有 `CookiesSettingsPage`、浏览器提取器和一个全局 `DesktopYtDlpPaths.cookiesFile()`，并支持导入/导出 Netscape cookies 文件。

影响：Desktop 当前实现适合桌面浏览器导出/提取，但不等价于 Android 的“按站点 profile 管理”。如果用户从 Android 迁移，会找不到 Cookie profile 列表，也无法为不同站点保存多套内容。

建议：

- 先在文档和 UI 中明确 Desktop 是“全局 cookies 文件”，不是 profile database。
- 如果要 parity，新增 Desktop cookie profiles，并在下载前设置里选择 profile 或按 URL 自动匹配。
- 如果不做 profile，至少在 Cookies 页支持显示当前 cookies 文件覆盖的网站列表，减少黑盒感。

### 13e. [x] Desktop 无痕模式没有禁用下载历史

证据：

- Desktop general 设置页提供 `private_mode` 开关并写入 `preferences.privateMode`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/general/GeneralSettingsPage.kt:145`
- Desktop 下载成功后无条件 `historyEntries.add(0, entry)` 并保存，存在普通下载和 metadata fallback 两条路径：`desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt:527`、`:751`
- Android 下载完成后会在 `privateMode` 时直接返回空结果，不插入历史：`app/src/main/java/com/junkfood/seal/util/DownloadUtil.kt:410`、`:424`

影响：用户开启“无痕/隐私模式”后，Desktop 仍会留下下载历史。这是比普通 parity 更严重的信任问题，因为 UI 明确承诺了隐私语义。

状态：已完成。`DesktopDownloadController` 现在通过 `appendDesktopHistoryEntryIfAllowed()` 统一落历史，`privateMode=true` 时直接跳过 append/save；队列备份也跳过 private mode 请求，避免未完成任务在下次启动恢复时暴露 URL。已补 `DesktopDownloadHistoryPrivacyTest` 覆盖 private 不保存、普通模式前插并裁剪 500 条。

完成细节：

- 所有 Desktop 成功落历史的路径都改为调用 `appendHistoryEntryIfAllowed()`。
- Private mode 下队列项仍可显示本次任务状态，但不会写入持久化历史。
- Queue snapshot 构建时会跳过 private mode 请求，避免下次启动恢复出敏感 URL。
- `DesktopDownloadHistoryPrivacyTest` 覆盖 `privateMode=true` 不 append/save，以及普通模式前插、裁剪并保存快照。

### 13f. [x] `cropArtwork` 已在 Desktop 执行层生效

原问题：

- Desktop 格式设置页展示 `crop_artwork`，且在 `extractAudio && embedMetadata` 时可开启：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/format/FormatSettingsPage.kt:136`
- Android adapter 会在音频类下载、内嵌元数据、裁剪封面同时满足时写入 `--ppa "ffmpeg: ... crop ..."` 配置，并通过 `--config` 注入：`app/src/main/java/com/junkfood/seal/download/YoutubeDlRequestAdapter.kt:23`、`:133`
- Desktop 执行链扫描不到 `cropArtwork` 使用点；当前只有设置页能读写该值。

影响：用户打开“裁剪封面”后，Desktop 生成的 yt-dlp 命令不会变化，最终封面不会被裁剪。这个和 `embedMetadata` 类似，属于“设置存在但后端没有兑现”。

状态：已完成。`DownloadPlanExecutor.defaultConfigFor()` 会在音频类下载、`embedMetadata=true`、`cropArtwork=true` 同时满足时，为 `ExecutionConfig.extraArgs` 注入 Android 等价的 `--ppa ffmpeg: ... crop ...` 参数。`DesktopDownloadController` 的队列 CLI 预览也会追加 `extraArgs`，避免 UI 展示和实际执行不一致。

完成细节：

- 实现选择直接传入 ProcessBuilder 参数列表中的 `--ppa`，而不是生成临时 config 文件，减少 Windows 引号与路径空格的额外不确定性。
- 开启 `extractAudio + embedMetadata + cropArtwork` 时，Desktop config 会包含 crop 配置。
- 关闭 `embedMetadata` 等前置条件时，不会注入 crop 参数。

### 13g. `privateDirectory` 在 Desktop 端仍是半迁移状态

证据：

- `DownloadPreferences` 仍包含 `privateDirectory`：`shared/src/commonMain/kotlin/com/junkfood/seal/util/DownloadPreferences.kt:38`
- Android adapter 会根据 `privateDirectory` 把音频/视频写入 `App.privateDownloadDir`：`app/src/main/java/com/junkfood/seal/download/YoutubeDlRequestAdapter.kt:54`
- Desktop 路径解析只看 `audioDirectory/videoDirectory`，没有处理 `privateDirectory`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopYtDlpPaths.kt:55`
- Desktop 目录设置页还 import 了 `private_directory/private_directory_desc`，但页面没有渲染对应设置：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/directory/DirectorySettingsPage.kt:71`

判断：这不一定要照搬 Android。Android 的 private directory 是平台沙盒/媒体库语义，Desktop 上更合理的实现可能是用户数据目录下的隐藏 downloads，或只提供“不要写历史/不要预览”的隐私模式。

建议：

- 先做产品决策：Desktop 是否需要“私有下载目录”。如果不需要，删除 stale import，并确保 UI 不暴露这个偏好。
- 如果需要，实现平台路径：Windows `%LOCALAPPDATA%/Seal/PrivateDownloads`，macOS `~/Library/Application Support/Seal/PrivateDownloads`，Linux `${XDG_STATE_HOME:-~/.local/state}/seal/private-downloads`，并在文件管理器入口处提醒这是应用私有目录。
- 不要把 `privateDirectory` 和 `privateMode` 混在一起：前者是保存位置，后者是历史/痕迹策略。

### 13h. 分章节输出模板需要测试锁住多 `-o` 语义

证据：

- shared plan 在 `splitByChapter` 时先添加 `-o chapter:%(section_number)s - %(section_title)s.%(ext)s`，再设置普通 output template：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:94`
- `DownloadPlan.asCliArgs()` 会无条件在末尾再追加 `-o outputTemplate`：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlan.kt:16`
- 当前测试只覆盖 `SelectionMerge` 的 `splitByChapter` 偏好合并，没有覆盖最终 yt-dlp CLI。

判断：yt-dlp 支持 typed output template，多 `-o` 不一定是错；但这里同时输出 `chapter:` 和普通模板，且 Android/屏幕提示都围绕“切分成 N 个片段”，非常需要测试固定预期，避免未来改动时把章节文件名或主输出模板弄反。

建议：

- 给 `DownloadPlanFactoryTest` 增加 `splitByChapter=true` 的 CLI 快照测试，明确应包含哪些 `-o`、顺序如何。
- 用一条实际公开视频在本地 smoke test，确认 Desktop 与 Android 生成的文件名一致。
- 如果发现第二个 `-o` 覆盖或干扰章节模板，应改成 yt-dlp 推荐的 typed output 写法，并同步 Android adapter。

### 13i. [x] Desktop 内嵌字幕没有自动联动 MKV 容器

证据：

- Android 构造 `DownloadPreferences` 时会把 `mergeToMkv` 设置为 `(downloadSubtitle && embedSubtitle) || MERGE_OUTPUT_MKV`：`app/src/main/java/com/junkfood/seal/util/DownloadUtil.kt:597`
- Desktop 字幕页确认内嵌字幕时只写 `embedSubtitle = true`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/subtitle/SubtitleSettingsPage.kt:188`
- shared plan 只有在 `preferences.mergeToMkv` 为 true 时才追加 `--remux-video mkv` 与 `--merge-output-format mkv`：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:150`
- Desktop 格式页虽然单独提供 “Remux to MKV” 开关，但它不是内嵌字幕确认流的一部分：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/format/FormatSettingsPage.kt:162`

影响：Desktop 用户在字幕页确认“启用实验功能/内嵌字幕”后，看到提示但不会自动获得 Android 的 MKV 容器兜底。最终能否嵌入取决于所选格式和 yt-dlp/ffmpeg 行为，失败时用户会以为是字幕下载坏了。

状态：已完成。`DownloadPlanFactory` 现在使用 `effectiveMergeToMkv()`，在 `mergeToMkv || (downloadSubtitle && embedSubtitle)` 时统一追加 `--remux-video mkv` 和 `--merge-output-format mkv`；Desktop `FormatSettingsPage` 会在内嵌字幕启用时把 MKV 开关显示为开启并禁用手动关闭，同时显示 `embed_subtitles_mkv_msg` 提示。已补 `DownloadPlanFactoryTest` 覆盖“内嵌字幕强制 MKV”和“只下载字幕不强制 MKV”两个边界。

建议：

完成细节：

- 规则下沉到 shared plan，Desktop/Android/未来入口都共用同一 effective MKV 语义。
- Desktop 格式页 UI 已避免“后端会 remux 但开关显示关闭”的矛盾状态。
- 验证通过：`./gradlew :shared:allTests :desktop:compileKotlin`。

### 13j. Desktop 播放列表下载缺少 Android 的条目选择语义

证据：

- Android 有 `PlaylistSelectionPageImpl`，用户可以选中多个条目后创建任务：`app/src/main/java/com/junkfood/seal/ui/page/downloadv2/configure/PlaylistSelectionPage.kt:217`
- Android `TaskFactory.createWithPlaylistResult()` 会把每个选中 index 写进 `Task.TypeInfo.Playlist(index)`：`app/src/main/java/com/junkfood/seal/download/TaskFactory.kt:65`
- Android 下载器 fetch info 时把 playlist index 传入 `DownloadUtil.fetchVideoInfoFromUrl()`，最终成为 `--playlist-items`：`app/src/main/java/com/junkfood/seal/download/DownloaderV2.kt:251`
- Desktop 下载设置 sheet 只有 `DesktopDownloadType.Playlist`，并且隐藏自定义格式编辑：`desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/DesktopDownloadSettingsSheet.kt:235`
- Desktop metadata fetcher 固定使用 `-J --no-playlist`，没有 Android 的 `--flat-playlist` 获取列表分支：`desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/YtDlpMetadataFetcher.kt:26`
- Desktop controller 构造 plan 时 `playlistItem = if (type == DesktopDownloadType.Playlist) 0 else 0`，所以永远不会输出 `--playlist-items`：`desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt:462`、`:686`
- shared plan 只有 `playlistItem != 0` 时才加 `--playlist-items`：`shared/src/commonMain/kotlin/com/junkfood/seal/download/DownloadPlanFactory.kt:56`

影响：Desktop 的 Playlist 类型更像“下载整个播放列表”，而不是 Android 的“先获取列表 -> 选择条目 -> 队列多个任务”。如果用户只想下载其中几项，Desktop 当前没有等价 UI。

建议：

- 短期：把 Desktop Playlist 文案明确为“下载整个播放列表”，并在下载前设置中说明不支持条目选择。
- 中期：复用 shared `PlaylistSelectionMapper`，给 Desktop 增加播放列表条目选择页；选中项可以生成多个 queue item，每个 item 带 `playlistItem=index`。
- 测试：给 Desktop controller/plan 增加 playlist item 覆盖，确保选中第 N 项时 CLI 包含 `--playlist-items N`。

## P1：CI / Release / Packaging

### 14. Release workflow 取“最新成功构建”，不是取当前 commit/tag 对应构建

证据：`.github/workflows/release.yml:96`

`release.yml` 会找三个 workflow 的 latest successful run，再下载 artifact。这样如果 main 上后续又跑过构建，或某个平台构建滞后，release 可能拼出不同 commit 的产物。

建议：

- release 输入 commit SHA，按 `head_sha == github.sha` 查找 workflow run。
- 或把 build 和 release 串成同一个 workflow，使用 `workflow_run`/`workflow_call` 传 artifact。
- artifact 名里写入 commit short SHA，release 前校验三端一致。

### 15. Desktop workflows 下载 latest/nightly 依赖，不可复现

证据：

- Windows yt-dlp nightly latest：`.github/workflows/windows_x64_portable.yml:17`
- Linux yt-dlp nightly latest：`.github/workflows/linux_x64_portable.yml:17`
- FFmpeg master latest：多个 workflow env

这会造成同一个源码 commit 在不同时间构建出不同二进制。

建议：

- release 构建 pin 到明确 version 和 checksum。
- nightly/latest 只用于 dev artifact 或手动 channel。
- 在 artifact 中写入 `THIRD_PARTY_VERSIONS.txt`，记录 yt-dlp/ffmpeg URL、version、sha256。

### 16. Actions 版本和 nightly action 需要统一审计

证据：

- desktop workflows 使用 `actions/upload-artifact@v6`
- Android workflow 使用 `actions/upload-artifact@v7`
- Android signing 使用 `ilharp/sign-android-release@nightly`

本报告没有联网核验这些 major 是否当前可用，但从可维护性看应统一策略。尤其 `nightly` action 会影响 release 可复现性。

建议：

- 统一 action 版本，并固定到已验证 major 或 commit SHA。
- release/signing 类 action 尽量不要用 nightly。
- 写一个 `docs/ci-dependency-policy.md` 说明“哪些允许 latest，哪些必须 pin”。

### 17. [x] ProGuard 和 console 调试状态不能长期留在 release

证据：`desktop/build.gradle.kts:105`、`:154`

状态：已完成并三次调整。三平台普通 release 默认启用安全 shrink；Compose 生成规则禁用 obfuscation，项目通过 `-dontoptimize` 避免 Kotlin/Compose verifier 错误，并保留 SQLite 的服务加载/JNI 类。Windows console/debug cmd 由 `desktopWindowsDebugLauncher` / workflow input 控制，不污染默认发行体验。

历史问题：为了排查 Windows JVM 启动问题，release ProGuard 曾关闭且 Windows console 打开。这对定位问题有帮助，但如果长期保留，会导致包体、启动体验和性能不可控。

建议：

- 保持 `main` 默认 release 发行参数不被排障参数污染。
- Windows/macOS release 结果仍必须由对应原生 runner 的 app-image 与安装后 SQLite smoke 验证；Linux 本机通过不能替代。

### 18. macOS 默认 target 仍包含 DMG，workflow 靠 env 改成 PKG

证据：`desktop/build.gradle.kts:114`

workflow 已设置 `DESKTOP_TARGET_FORMATS=pkg`，所以 CI 当前只打 pkg。但如果本地或未来 workflow 忘了 env，macOS 默认仍会打 DMG + PKG。

建议：

- 如果项目已经决定 macOS 只发 pkg，把 Gradle 默认改成 `Pkg`。
- 或保留默认，但在 docs/workflow 注释里写明 “CI intentionally overrides to pkg only”。

## P1：国际化与语言切换

### 19. Desktop 暴露语言列表少于资源目录

事实：

- `shared/src/commonMain/composeResources` 有 62 个 `values*` 目录。
- `DesktopLocaleOptions.kt` 暴露 48 个语言选项。
- 资源存在但 Desktop 未暴露的包括：`ckb`、`gl`、`ji`、`kab`、`kmr`、`lt`、`lv`、`mr`、`or`、`ro`、`sk`、`sl`、`ur`、`uz`、`ar-SA` 等。

建议：

- 后续把 Desktop 语言列表从资源目录生成，或至少写一个检查脚本防止 drift。
- `en-US` 这种没有专属资源目录但走默认资源的情况要显式标记为 fallback-ok。

### 20. 仍有不少 Desktop 硬编码中文/英文文案

扫描到的代表路径：

- `desktop/src/main/kotlin/com/junkfood/seal/desktop/ytdlp/DesktopAuxiliaryDownloader.kt`
- `desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/network/NetworkSettingsPage.kt`
- `desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/network/CookiesSettingsPage.kt`
- `desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/DesktopEnvironmentSetupDialog.kt`
- `desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt`
- `desktop/src/main/kotlin/com/junkfood/seal/desktop/download/DesktopDownloadController.kt`

建议：

- 新增 `desktop_i18n_hardcoded_scan` 任务或脚本，允许注释中文，但阻止 UI/错误文案硬编码。
- 优先抽下载流程、依赖检测、格式页、网络页，因为这些是用户高频路径。

### 21. 很多新增字符串在非中英文资源中缺失

现状：资源目录很多，但大量 locale 缺新增 key，会回退英文。短期不影响构建，但语言切换后会出现半本地化。

建议：

- 增加资源 key 覆盖率报告。
- 对未翻译语言，在语言页标注翻译不完整，或只暴露覆盖率超过阈值的语言。
- Weblate 同步前，把 Desktop 新增字符串集中补齐到 default/zh-rCN/zh-rTW。

### 22. ResourceEnvironment 反射方案需要 smoke test 锁住

当前语言切换的递归问题看起来已通过缓存 `originalResourceEnvironment` 修好，但实现依赖 Compose Resources 内部类/方法反射。Compose 升级时很容易破。

建议：

- 增加一个 Desktop 启动级 smoke test 或小型 JVM test，验证 `desktopResourceLocaleForTag("zh-Hans")` 能命中 `zh/CN`，`he/id` 能映射到 `iw/in`。
- Compose 版本升级 PR 必须手动验证语言切换。

### 22A. `AndroidStrings` 与 Compose Resources 的 locale fallback 尚未统一

当前 `DesktopLocaleOptions` 已为 Compose Resources 处理 `zh-Hans -> zh/CN`、`zh-Hant -> zh/TW`、`he -> iw` 和 `id -> in`，但 `AndroidStrings.buildCandidatePaths()` 仍只按 Java Locale 的 `language + country -> language -> default` 查找。

影响：非 Compose 路径在简繁中文脚本 locale、Hebrew、Indonesian 下可能跳过已有目录并回退英文，形成“页面已切换，但日志、错误或平台文本仍是英文”的混合语言。

建议：

- 把 persisted tag 到 resource qualifier 的映射抽成一份可由 Compose Resources 与 `AndroidStrings` 共用的纯函数。
- 为 `zh-Hans`、`zh-Hant`、`he/iw`、`id/in`、地区优先和默认回退增加参数化测试。
- 在修复并验证前，不把“全部 Desktop 文案语言链已完整”标为完成。

## P2：存储与数据模型

### 23. DownloadPreferences 未纳入 DesktopStorageConfig

证据：`desktop/src/main/kotlin/com/junkfood/seal/desktop/settings/DesktopSettingsState.kt:39`

下载偏好仍固定写 `~/.local/state/seal/settings.json`，没有走 `json/dual/sqlite` 存储后端。队列、历史、app settings 已经在新 storage 架构里，偏好设置是漏网项。

建议：

- 新增 `DesktopPreferencesStorage` 的 SQLite/dual 实现。
- 自检覆盖偏好设置 load/save/quarantine。
- 迁移时保留旧 `settings.json` bootstrap。

### 24. Desktop state path 在 Windows/macOS 上不够原生

证据：

- settings 默认：`~/.local/state/seal/settings.json`
- yt-dlp state 默认：`~/.local/state/seal/yt-dlp`

这在 Linux 合理，但 Windows/macOS 用户更期待 AppData 或 Library/Application Support。辅助二进制路径已有平台化倾向，settings/storage 也应统一。

建议：

- Windows：`%APPDATA%\Seal` 或 `%LOCALAPPDATA%\Seal`
- macOS：`~/Library/Application Support/Seal`
- Linux：继续 XDG
- 做迁移：新路径不存在时从旧 `.local/state` 导入。

### 25. SQLite 当前是“JSON payload 表”，不是细粒度关系模型

这不是 bug，作为迁移第一阶段很稳。但如果未来要做历史搜索、筛选、统计，单 row JSON payload 不能发挥 SQLite 价值。

建议：

- 短期保留，先稳定迁移。
- 中期为 history/queue 增加 normalized columns，例如 url/title/status/createdAt/extractorKey。

### 26. dual 自检会输出预期内堆栈，进 CI 前要优化日志

`dual` 自检通过，但会故意写坏 JSON 并打印 `JsonDecodingException` stack trace。作为本地诊断可以，CI 里容易被误判成失败。

建议：

- 自检模式下把预期异常压缩成一行。
- 或增加 `-PstorageSelfCheckVerbose=true` 才打印堆栈。

## P2：UI / Android parity

### 27. 自定义格式页仍有硬编码中文状态文案

证据：

- “正在获取视频信息...”
- “加载失败”
- “重试”
- “返回”
- “未获取到可用链接”
- “音频与无音轨视频可组合下载”

这些都在 `desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt`。

建议：抽到 compose resources，并复用 Android 文案 key。

### 28. 自定义格式页 UI 细节还要继续和 Android 对齐

已接近 Android 的点：缩略图右侧三点菜单、字幕“查看更多”、搜索、清空按钮、重命名弹窗、切分提示都已有结构。

仍需复核：

- Header 菜单项出现/隐藏条件要与 Android 完全一致。
- “切分视频”启用后的提示、撤销、divider 间距要截图对齐。
- 字幕搜索框 shape、背景、清空按钮、选中动画需要继续肉眼对照。
- `Show all %d items` 与中文 “查看全部” 的文案和位置要统一。

### 29. 附加设置展开动画已改过，但仍建议做回归截图

用户反馈过“收缩完突然空一格再跌落”和点击范围太窄。当前代码可能已修，但这类是 Compose layout animation 容易回归的点。

建议：

- 对 `DesktopDownloadSettingsSheet` 加一个开发用 screenshot checklist。
- 收起时避免 `AnimatedVisibility` 外层保留底部 padding。
- Header 点击区域至少 48dp 高，并包含文字、空白区域和箭头。

### 30. Custom Commands 仍有硬编码英文通知和错误

代表路径：`desktop/src/main/kotlin/com/junkfood/seal/desktop/customcommand/DesktopCustomCommandTaskManager.kt`

包括 `Command Started`、`Command Completed`、`Command Error`、`URL is empty` 等。自定义命令是 Desktop 重要功能，应纳入 i18n。

### 31. Desktop Main 仍有 “not available” 占位文案

证据：`desktop/src/main/kotlin/com/junkfood/seal/desktop/Main.kt:674`

如果用户能导航到该页面，会显得未完成。若只是兜底分支，可以改成资源文案并记录 unreachable。

## 2026-06-17 UI / 动画 parity 复查补充

这轮重点只看“Android 已有、Desktop 看起来可用但细节还没完全移植”的 UI/动画。下面按用户路径拆分。

### UI-A. 下载前设置 sheet

- [x] Desktop 已有 sheet 内部页面切换动画：`DesktopDownloadScreen` 使用 `AnimatedContent` 在输入页和下载设置页之间滑动切换。
- [x] 附加设置已改成可展开/收起，并带箭头旋转、`expandVertically` / `shrinkVertically` 动画；点击区域已比早期更宽，后续仍建议按 48dp 触控/点击目标做回归。
- [ ] Android `DownloadDialogV2` 的主体使用 `Column.animateContentSize()` 包住“格式选择/模板选择”区域，Desktop 目前只有局部 `AnimatedContent` 和 `AnimatedVisibility`，切换下载类型时的高度变化手感仍不完全一致。
- [ ] Android 的 `SingleChoiceItem` 使用 spring 圆角/颜色动画和 `Crossfade` 图标，Desktop 的下载类型 segmented row 和 preset/custom card 只做了颜色/summary 动画，选中态的圆角、图标出现方式、卡片高度还没有完全复刻。
- [ ] Android 的 `ExpandableTitle` 当前更像“一次展开”的 bottom sheet 交互，Desktop 做成了可收起。这个是 Desktop 体验增强还是需要完全对齐 Android，建议后续用截图/录屏定一次规范。
- [ ] Desktop 附加设置里保留了 `embedMetadata`、`SponsorBlock` 两个 Desktop-specific chip，功能上合理，但文案和信息层级要继续避免用户误解为“所有下载类型都等价生效”。

### UI-B. 新建任务 / URL 输入页

- [ ] Android `InputUrlPage` 支持剪贴板多链接提示、选择多个链接、保存链接入口、保存链接弹窗、滑动删除、`animateItem()` 列表动画；Desktop `DownloadInputSheet` 目前只有单行 URL、粘贴按钮、取消/继续。
- [ ] Android 输入框支持多行 URL 自动整理，Desktop 当前是 `singleLine = true`。多链接输入虽然后端可以处理换行，但 UI 会截断显示，不利于用户确认。
- [ ] Android 保存链接 chip 会根据“当前 URL 是否已保存”用 `animateContentSize()` 切换文案；Desktop 还没有 saved links UI。
- [ ] Android 保存链接弹窗用 `SwipeToDismissBox` 删除并有颜色过渡，Desktop 若要做桌面化，可以改成 hover actions/context menu，但需要保留等价的删除反馈动画。
- [ ] 2026-07-07 复核：Desktop 已经 import `save` 资源，但用途是内部设置保存按钮，不是 Android 的 saved URLs 功能；不要误判为“保存链接已迁移”。

### UI-C. 自定义格式页

- [x] Desktop 已补缩略图右侧三点菜单，菜单中有重命名、缩略图、剪切视频、切分视频。
- [x] Desktop 已补字幕“查看更多”、字幕搜索、清空按钮、`animateItem()` 列表排序动画、重命名弹窗、切分提示行。
- [ ] Desktop 仍未把 `downloadType` 传入 `FormatPageImpl` 做 UI 过滤；音频入口仍会展示视频格式，详见 P1 第 5 项。
- [ ] Desktop 仍有硬编码中文提示：例如 `FormatHintInfo(text = "音频与无音轨视频可组合下载")`，以及加载/错误/重试等状态文案，应继续抽到 compose resources。
- [ ] Android 剪切视频是 `VideoSelectionSlider` + `VideoClipDialog` 的范围选择；Desktop 目前是菜单 toggle 后依赖 `clipStartText/clipEndText`，视觉反馈和可编辑性都不等价。
- [ ] Android 字幕预选支持 `en.*,.*-orig` 这种正则式偏好；Desktop 仍按 exact code 匹配，导致“默认字幕设置”和“格式页初始选中”可能不一致。
- [ ] Android 字幕弹窗使用项目统一的 `SealSearchBar`；Desktop 是本地手写 `DesktopSealSearchBar`。搜索框高度、圆角、背景、placeholder、清空按钮 hover/focus 状态需要截图压齐。
- [ ] Android `ClickableTextAction` 使用 spring fade exit；Desktop 改成 90ms tween。若追求一致，建议统一到 Android 的 spring，或抽 common action text component。
- [ ] Android 格式卡片来自 `FormatItem` / `SuggestedFormatItem` 组件；Desktop 是本地 `FormatItemCard`。已接近，但选中边框、容器色、图标位置、长按/分享替代交互和网格最小宽度仍需录屏比对。
- [ ] “显示全部 %d 项 / 查看全部”在 Desktop 的字幕区和格式区文案不完全统一，建议统一复用 `show_all_items`，同时中文下位置保持 Android 的右侧文本动作。

### UI-D. 下载队列卡片与 action sheet

- [x] Desktop 队列已迁到 shared `DownloadQueueScreenShared`，action sheet、状态按钮、错误详情、复制/打开/删除等主交互比早期更接近 Android。
- [ ] Android `VideoCardV2` 的状态区域使用 `AnimatedContent + materialSharedAxisY`，Desktop/shared 当前的状态文本和按钮动画需要继续核对，尤其是 Running -> Completed/Error、Canceled -> Resume 的图标切换。
- [ ] Android `ActionSheet` 里的按钮使用 `animateItem()` 排列变化；shared action sheet 已有相似结构，但 Desktop 的 `ModalBottomSheet` 默认动画、sheet handle、padding、宽屏最大宽度仍不是 Android `SealModalBottomSheet` 的视觉。
- [ ] Desktop 队列有 grid/list 视图切换，这是 Desktop 特化项；需要确认切换时条目是否有 placement animation，否则会显得“瞬移”。

### UI-E. 下载历史页

- [ ] Android 历史页有搜索 `AnimatedVisibility`、长按多选、底部 `BottomAppBar`、批量删除/导出、条目进入/退出动画；Desktop 历史页目前是搜索 `if (showSearch)` 直接出现、单条 dropdown 操作，没有多选底栏。
- [ ] Android 历史条目打开详情用 `VideoDetailDrawer` / bottom sheet；Desktop 历史条目以行内菜单为主。桌面可保留菜单，但如果要 Android parity，应补一个详情侧栏/弹窗并带进入/退出动画。
- [ ] Desktop 历史导入/导出弹窗已实现，但错误弹窗仍有 `Text("OK")` 硬编码，且文件选择器标题 `Import/Export` 是英文。
- [ ] 2026-07-07 复核：Desktop 已有导入/导出能力，缺的是 Android 的“选中若干历史项后只导出选中项”与批量删除确认流，不应把“有导入导出”误判成历史页 parity 完成。

### UI-F. 设置页与自定义命令设置

- [x] Desktop 设置页已有按页面深度滑动的 `AnimatedContent`，并使用 `rememberSaveableStateHolder` 保留子页状态；语言切换后回到主页的问题已被这一方向覆盖。
- [ ] Android 设置页基于 `LargeTopAppBar` 的 collapse scroll behavior；Desktop `SettingsPageScaffold` 的顶部栏和内容滚动效果仍是桌面化实现，未完全复刻 Android 的大标题折叠。
- [ ] Android 外观页的语言卡片 description 会显示 `Locale.getDefault().toDisplayName()`，Desktop 目前固定显示 `language_settings`。语言切换已经能即时刷新，但列表页外层缺少“当前语言是什么”的摘要反馈。
- [ ] Android 自定义命令模板页有长按多选、`AnimatedVisibility` 底栏、模板列表选择动画；Desktop 模板设置页目前以列表项、导入导出和编辑页为主，多选批处理手感未迁。
- [ ] Desktop 自定义命令运行通知 overlay 有 `AnimatedVisibility`，但通知标题仍是 `Command Started/Completed/Error` 等英文硬编码，应继续接入 compose resources。
- [ ] 设置页中多个 Desktop-only 弹窗使用 `AnimatedAlertDialog`，但按钮、padding、icon/title 间距和 Android `SealDialog` 不是同一套 token。建议后续抽 Desktop dialog tokens 或做一页对照截图。

### UI-G. 全局动画规范

- [ ] 现在 Android 侧使用 `SealModalBottomSheet`、`SealDialog`、`SealSearchBar`、`materialSharedAxisX/Y` 等统一组件；Desktop 侧有不少本地复刻组件。建议建一个 Desktop parity component map，明确哪些必须共用/复刻，哪些允许桌面化。
- [ ] 建议为下载前设置 sheet、自定义格式页、字幕弹窗、历史页、设置页各录一段 Android reference，再录 Desktop reference。只靠代码 diff 很难发现“收缩后空一格”“选中动画不丝滑”这类视觉 bug。
- [ ] 对容易回归的动画点写 screenshot checklist：附加设置展开/收起、格式页切分提示显示/撤销、字幕搜索输入/清空、队列状态 Running -> Completed、设置页进入/返回。

### UI-H. 硬编码 UI 文案/辅助标题补充

- [ ] `Main.kt` 仍有 Desktop 不可用页英文占位：`This page is not available on Desktop yet.` / `Tip: use the Android app for now.`。
- [ ] `DesktopDownloadController` 的系统通知标题仍是 `Download Completed` / `Download Error`，应接入资源或通知文案映射。
- [ ] `DesktopCustomCommandTaskManager` 的通知标题和错误仍是 `Command Started` / `Command Completed` / `Command Error` / `URL is empty`。
- [ ] 历史页错误弹窗和文件选择器标题仍有 `OK` / `Error` / `Import` / `Export`。
- [ ] 目录设置页的系统目录选择标题仍是 `Select Video Directory` / `Select Audio Directory` / `Select Custom Command Directory`。
- [ ] Network/Cookies/Environment setup 仍有若干中英硬编码，例如 `自动检测本机代理（Xray）`、`从文件导入 (Fallback)`、`This will delete the cookies file...`、`Copy`。
- [ ] `UpdateSettingsPage` 仍有应用更新 TODO；如果暂不实现，也要本地化并改成明确的 unsupported/manual update 状态。
- [ ] `SponsorBlockDialog` 的 `default` / `all` 可以保留为 yt-dlp 语义值，但最好把显示 label 与实际写入值分离，避免未来本地化时误改参数值。

### UI-I. Desktop 全局滚动位置反馈

- [x] 已增加 KMP `PlatformVerticalScrollbar` 的 `expect/actual` 封装：Android 保持原有触摸端外观，Desktop 统一使用 8dp 圆角滑块、悬停增强和溢出检测。
- [x] 已覆盖设置首页、共用设置子页、Cookies 页、赞助页、下载队列的列表/网格、下载历史、自定义命令任务和自定义格式网格。
- [x] 已覆盖字幕选择、命令日志/模板选择、下载详情、yt-dlp 更新、输出模板、命令快捷项、环境安装日志和队列 action sheet 等可溢出弹窗。
- [x] 滚动条与 `ScrollState` / `LazyListState` / `LazyGridState` 分别绑定，不在 Window 根层强行共享位置；切换队列列表/网格时两种位置独立保留。
- [ ] 人工回归 Windows / macOS / Linux 的鼠标悬停、拖动、点击轨道与高 DPI 显示；特别检查格式页 FAB 、宽屏右边距和弹窗按钮区是否被覆盖。

## P2：构建系统与依赖管理

### 32. `mavenLocal()` 会让本地和 CI 依赖解析不一致

证据：`settings.gradle.kts:18`

这对开发调试有用，但如果本地装过未发布 Compose/KMP artifact，可能出现“我本地能构建、CI 不能”的情况。

建议：

- 仅在 `-PuseMavenLocal=true` 时启用。
- 或至少在 CI 明确禁用/打印 repository list。

### 33. Desktop/shared 缺少格式化或静态检查入口

Android 有 ktfmt 配置，但 Desktop/shared 没看到等价 lint/format workflow。迁移阶段代码变动快，UI 文件变长，格式漂移会加速。

建议：

- 引入 ktlint/spotless 或统一 ktfmt 到 desktop/shared。
- CI 增加 `:desktop:compileKotlin`、`:shared:desktopTest`、storage selfcheck。

### 34. Gradle wrapper 仍是 8.10.2，已有 Gradle 9 兼容预警

这不是立刻要升，但要提前清 deprecated feature。否则未来 Compose/AGP/Kotlin 升级时会被迫一次性处理。

建议：

- 单独开 issue：`./gradlew help --warning-mode all`，列出插件/脚本 warnings。
- 与 AGP/Kotlin 兼容矩阵一起处理。

## P3：文档与维护体验

### 35. [x] 旧缺陷文档已恢复为兼容入口

原问题：用户之前一直提到 `docs/android-desktop-backend-defects.md`，但当时 `docs/` 下没有该文件。可能是未跟踪、被移动或在另一工作树。

状态：已完成。已恢复 `docs/android-desktop-backend-defects.md` 作为兼容入口，并明确旧缺陷清单已合并到本文；后续不再维护平行缺陷列表。

完成细节：

- `docs/android-desktop-backend-defects.md` 只保留重定向说明，避免旧 IDE 标签和引用断链。
- 当前行动清单统一维护在 `docs/desktop-project-audit-2026-06-15.md`。

### 36. 近期 git log 中 Windows installer 修复提交信息过于临时

最近多条提交是 `try: fix windows #...`。排障阶段没问题，但合入前最好 squash 或补一个总结 commit/message，否则以后查“之前怎么修的”会很痛。

建议：

- Windows installer 稳定后，写 `docs/windows-installer-debugging.md`。
- 把最终原因、无效尝试、必要构建参数、回滚方式记下来。

### 37. project map 可以继续扩展 Desktop 迁移章节

已有 `docs/project-map.md` 和 `docs/android-desktop-progress-tracker.md`。建议把以下主题加入 tracker：

- Desktop dependency resolver
- Desktop storage backend
- Custom format parity
- i18n/resource switching
- Windows installer pipeline
- Release artifact provenance

## 建议实施顺序

### 第一批：小改但收益最高

- [x] 修 SponsorBlock 保存 bug。
- [x] 修 `DownloadPreferences.videoDirectory/audioDirectory/commandDirectory` 不生效。
- [x] Desktop 正确映射 aria2c 参数，不再传 Android `libaria2c.so`。
- [x] Desktop 将 aria2c 纳入可选依赖检测，缺失时禁用/提示网络页开关。
- [x] Desktop 无痕模式真正跳过历史持久化，并确认 queue snapshot 不长期保留敏感 URL。
- [x] `cropArtwork` 在 Desktop 执行层实现 ffmpeg crop config，或在 Desktop 暂时隐藏/禁用。
- [x] Desktop 内嵌字幕自动联动 MKV remux，或在共享 plan 里统一计算 effective mergeToMkv。
- [x] `YtDlpMetadataFetcher` 只要求 yt-dlp，不强制 ffmpeg。
- Desktop 下载归档增加查看、编辑、清空入口。
- Desktop 下载归档补重复预检查和可读反馈，不要把已归档跳过伪装成普通完成。
- Desktop app 自动更新 UI 改成真实检查或明确暂不支持。
- Inno 语言 section 改成显式 `[Languages]`，不要依赖未定义 `EmitLanguagesSection`。

### 第二批：用户可感知 parity

- 自定义格式页传入 `downloadType` 并按类型过滤格式。
- 字幕预选支持 `en.*,.*-orig`。
- 视频剪切菜单接入范围编辑，或暂时禁用菜单项。
- 播放列表下载补条目选择页；若短期不做，明确标注为“下载整个播放列表”。
- 新建任务页补多链接识别、保存链接列表和删除反馈。
- 历史页补长按/多选/批量删除/选中项导出，或定义 Desktop 替代交互。
- Cookies 页明确全局文件语义；如果要 Android parity，补 Cookie Profiles。
- 决定 Desktop 是否实现 `privateDirectory`；不实现就清理 stale import/偏好入口，实现则定义平台私有路径。
- 分章节下载补 CLI 快照测试，锁住多 `-o`/typed output template 行为。
- 抽离 Desktop 格式页、网络页、依赖安装页硬编码文案。
- 外观页语言卡片显示当前语言摘要，和 Android 设置页一致。

### 第三批：release 稳定性

- [x] Windows installer 已增加安装后 smoke test；SQLite 首启版仍待修复提交后的 Actions 复验。Debug shortcut 继续只由手动 debug 开关生成。
- release workflow 绑定 commit SHA，不再取 latest successful run。
- 把 yt-dlp/ffmpeg 从 latest/nightly 改成 release 可 pin。
- [x] 恢复 release ProGuard/console 正常配置，用 Gradle property 控制 debug build。

### 第四批：长期维护

- DownloadPreferences 纳入 SQLite/dual storage。
- 增加 Desktop 语言列表与资源目录 drift 检查。
- 清 Gradle 9 deprecation、AGP/Kotlin 兼容警告。
- 统一 workflow action 版本与 pin 策略。

## 可以直接开 issue 的标题

- `fix(desktop): save SponsorBlock categories from dialog result`
- `fix(desktop): honor video/audio/custom command download directories`
- `fix(desktop): map aria2c downloader per platform instead of libaria2c.so`
- `fix(desktop): detect optional aria2c before enabling external downloader`
- `fix(desktop): honor private mode by skipping persisted history`
- `fix(desktop): implement or hide crop artwork on desktop`
- `fix(desktop): force mkv remux when embedding subtitles`
- `fix(desktop): allow metadata fetch with yt-dlp only`
- `feat(desktop): add download archive viewer and clear action`
- `fix(desktop): surface download archive duplicate skips`
- `fix(desktop): disable or implement app update check UI`
- `fix(desktop): decide and implement private directory semantics`
- `test(shared): lock split chapter output template arguments`
- `feat(desktop): add playlist item selection parity`
- `feat(desktop): add saved links and multi-url input parity`
- `feat(desktop): add history multi-select and selected export parity`
- `feat(desktop): define cookie profiles parity for desktop cookies`
- `fix(desktop): pass downloadType into custom format page and filter formats`
- `fix(desktop): support subtitle language patterns in custom format selection`
- `fix(windows): make Inno languages explicit and add debug shortcut`
- `ci(release): bind desktop artifacts to release commit sha`
- `ci(desktop): add compileKotlin and storage self-check matrix`
- `i18n(desktop): remove hardcoded Chinese strings from setup/network/format pages`
