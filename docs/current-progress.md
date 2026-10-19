# Seal-Desktop Current Progress

> Role: short current-state page for resuming work.
>
> Refreshed: 2026-10-20
>
> GitHub main checked this round: `c34e88e7d1d02d5eaec8a5234b83231979bed58e`.
>
> Active release branch: `release/0.1.0-rc.1`, created from GitHub `origin/main` at the reviewed baseline above. The target user-visible version is `0.1.0-rc.1`. No tag, GitHub Release, merge or push is part of this preparation task.
>
> This page is the current resume point. Revalidate entries against code before changing behavior. Product capability status lives in `feature-roadmap.md`.

## Resume Summary

Core Desktop functionality is considered mature enough for the first release candidate. This freeze is limited to localization correctness, release-facing wording, version correctness and low-level regressions already visible in the RC. Playlist item selection, true multi-URL tasks, Saved URLs, History bulk actions, automatic application updates and private-directory semantics remain Planned or Deferred rather than release blockers.

The final pre-freeze product baseline is `c34e88e7d1d02d5eaec8a5234b83231979bed58e`. Native portable/package workflows passed on that exact SHA: Windows x64 run `39284835064`, Linux x64 run `39284835054`, and macOS x64 plus arm64 run `39284835186`. Those runs prove the reviewed baseline only; the final localization/version SHA still requires the same native matrix and dependency-download smoke before release.

The `0.1.0-rc.1` preparation audits Desktop and shared Desktop UI strings against Android XML as the source of truth, validates literal and known dynamic `AndroidStrings` keys, preserves default fallback for untranslated locales, and verifies the user-visible version separately from the transformed macOS package version. Remaining acceptance is a short English/Simplified Chinese/Traditional Chinese/Follow System UI pass, real browser Cookies checks where relevant, and native package verification on the final SHA.

Local release-freeze validation on 2026-10-20 passed Android-to-Compose resource sync, Desktop compilation, all Desktop tests, all Shared target tests, and Android `genericDebug` Kotlin compilation. The reports contain 199 Desktop tests plus 22 tests each for Shared Desktop, Android Debug and Android Release, all with zero failures, errors or skips. Resource-directory parity is 62/62, XML parsing and regular-string placeholder contracts pass, and version tasks report display `0.1.0-rc.1`, native package `0.1.0` and macOS package `1.1.0`. This local evidence does not replace the final-SHA native matrix or Human localization review.

The following sections retain older maintenance evidence where still relevant. The project resumed on 2026-09-28 after more than one month of limited activity.

The pre-resume dirty worktree was stashed with untracked files, the governance branch was checked out, and the stash was reapplied. A full safety copy remains in `stash@{0}`; the local-only `main` commit `d6f8231e` also remains reachable. Do not drop the stash until the restored changes are reviewed and committed deliberately.

A new triage rule is now active: unfinished porting/planned capability is not automatically a bug. Check `feature-roadmap.md` before acting on issue reports.

Upstream Seal activity observed after the pause is mostly README sponsor automation. The latest stable yt-dlp release observed on 2026-09-28 is [`2026.08.19`](https://github.com/yt-dlp/yt-dlp/releases/tag/2026.08.19). Packaging and in-app download paths still intentionally allow moving `latest`/nightly URLs, but Full workflow artifacts now record the actual tool version, SHA256, source type/URL and build commit in `THIRD_PARTY_VERSIONS.txt`.

## P0 — Verify Before New Feature Work

### Native release evidence

Current pre-freeze evidence on `c34e88e7d1d02d5eaec8a5234b83231979bed58e`:

| Platform | Workflow evidence | Result |
| --- | --- | --- |
| Windows x64 | run `39284835064` | portable/package passed |
| Linux x64 | run `39284835054` | portable/package passed |
| macOS x64 | run `39284835186` | portable/package passed |
| macOS arm64 | run `39284835186` | portable/package passed |

The final `0.1.0-rc.1` localization/version commit is a new SHA and must not inherit those results by implication. Run all four native targets plus `desktop-dependency-download-smoke` again on the final SHA, and keep all release artifacts tied to that same commit.

#### Superseded native investigation retained for history

Earlier inspected native packaging evidence at the then-current main baseline (`2a7d1c45`, 2026-08-17):

- [Windows x64 portable/package run 32002024682](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024682): success;
- [Linux x64 portable/package run 32002024745](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024745): success;
- [macOS package run 32002024659](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024659): arm64 failed at `Smoke test Lite app and installed PKG`; x64 was canceled by that matrix failure.

The latest scheduled dependency smoke inspected this round is [run 36698411228](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/36698411228), dated 2026-09-30, on **main at `2a7d1c45`**, not the active branch:

| Platform | Actual evidence | Classification |
| --- | --- | --- |
| Windows x64 | JDK setup failed with a JetBrains API rate-limit error; source tests/download never ran | Third-party/setup blocker, not JVM launch evidence |
| Linux x64 | Dependency smoke succeeded | Older main only |
| macOS x64 | Dependency smoke succeeded | Older main only |
| macOS arm64 | JDK setup and source tests succeeded; ffprobe ZIP extraction failed with `Unexpected end of ZLIB input stream` | External transfer/archive failure exposed unsafe direct extraction |

The branch already contains the earlier Temurin/Bash/matrix repairs. This round adds safeguards without weakening acceptance:

- dependency smoke uses Temurin 21 instead of JetBrains Runtime because that job tests command-line dependencies, not Compose rendering;
- the macOS/Linux app-image smoke no longer uses Bash 4-only lowercase expansion, which is unavailable in macOS system Bash 3.2;
- the macOS packaging matrix has `fail-fast: false`, preserving x64 and arm64 evidence independently;
- truncated/corrupt ZIP payloads are staged and size/CRC checked before promotion; transient IO failures retry at most three times without replacing old tools on extraction failure;
- native dependency smoke now runs the complete Desktop and Shared Desktop tests plus json/dual/sqlite self-checks on each runner;
- Lite/Full app images and installed/extracted packages validate absence of tools or actual SHA256, version and build-commit provenance respectively;
- Windows smoke no longer silently substitutes a BAT for a requested EXE; Linux Full DEB gained the same extracted-package startup check as Lite.

The Actions API returned zero runs for `chore/agent-governance-refresh`. The available connector is read-only, this shell has neither `gh` nor an Actions token, and the available browser is logged out. No workflow was dispatched. GitHub fetch succeeded; GitLab fetch failed for missing shell credentials. No merge, release, force-push or user-data replacement occurred.

Local Linux x64 package work on 2026-10-19 produced both negative and positive evidence:

- The first Full app-image proved that marking `prepareAppResources` files executable was insufficient: jpackage copied yt-dlp/ffmpeg/ffprobe into the final image as `0644`, and direct execution failed with `Permission denied`.
- The corrected build repairs the final release app-image to `0755` and makes native package tasks consume that image. The rebuilt Full app-image and extracted Full DEB contained executable yt-dlp, ffmpeg and ffprobe, and each binary ran successfully.
- The Full DEB was approximately 83 MiB with an approximately 172 MiB extracted app. The Lite DEB was approximately 47 MiB with an approximately 135 MiB extracted app and contained none of the three bundled tools or their provenance manifest.
- Extracted Lite and Full DEB launchers each remained alive for a 12-second isolated smoke and initialized SQLite. The forced timeout emitted Skiko/native termination noise, so this is startup evidence, not normal-close evidence.
- Local RPM creation is blocked because this host lacks RPM packaging tools. Windows x64 and macOS x64/arm64 behavior remains unverified; workflow checks now fail if a final Unix package contains non-executable tools instead of mutating the package during verification.

Action:

- run the updated dependency smoke on Windows x64, Linux x64, macOS x64 and macOS arm64;
- run the updated macOS package workflow for both architectures and inspect Lite app plus installed PKG launch separately from JDK setup;
- keep OS/architecture results separate;
- record new run links/results here, not as timeless claims in project memory.

### Release provenance is partially closed; native workflow evidence pending

Windows, Linux and both macOS architecture workflows now generate and smoke-check a bundled `THIRD_PARTY_VERSIONS.txt` for yt-dlp, ffmpeg and ffprobe. The manifest records the binaries actually packaged, including version output, SHA256, size, moving/pinned source classification, configured source URL and build commit.

The release workflow no longer combines each platform's unrelated latest successful run. It resolves one target commit, requires all platform workflows to have succeeded at that exact `head_sha`, and publishes `BUILD_PROVENANCE.txt` plus `SHA256SUMS`. YAML, Bash blocks, the async github-script JavaScript and the manifest generator passed local static checks on 2026-09-29. A real Actions build/release dry run is still required; moving dependency URLs and action SHA pinning remain open policy work.

### Latest local baseline verified; native matrix remains pending

The 2026-10-19 local baseline completed without repository/network-source workarounds:

- `:shared:syncAndroidStringsToComposeResources :desktop:compileKotlin :shared:allTests :desktop:test`: passed; 195 Desktop tests and 22 tests on each Shared target completed with zero failures/errors/skips;
- `:app:compileGenericDebugKotlin`: passed;
- metadata process tests cover normal JSON, warning stderr, non-zero exit, oversized stderr, timeout, coroutine cancellation, invalid JSON and spawn failure;
- focused tests cover stale-format isolation, metadata runtime options, directory resolution, input policy, queue projection, History import, command-template import and dependency presentation;
- `desktopStorageSelfCheck` passed independently for json, dual and sqlite in isolated temporary state; expected corruption-injection diagnostics in Dual mode ended in successful SQLite fallback;
- packaged-tool verifier Python tests: 8 passed;
- affected workflow YAML parsed locally and shell blocks were reviewed; `actionlint`, `pwsh`, `shellcheck` and `ruby` are unavailable on this host, so matching native/static CI remains required;
- default/Simplified Chinese/Traditional Chinese XML and generated Compose Resources are synchronized; other locales deliberately use default fallback.

Exact commands and evidence boundaries are recorded in the linked maintenance note. Existing AGP/Kotlin compatibility and Gradle 9 deprecation warnings remain; the toolchain was not upgraded.

## P1 — Issue #4 Triage

GitHub Issue #4 contains four useful reports, but they are not one class of problem.

### Cookies unified context — backend verified, native browser review pending

The Desktop-specific backend contract is now implemented and locally verified:

- `DesktopCookieResolver` produces one fail-closed runtime context for metadata, Custom Format, normal downloads, custom commands and retry;
- browser extraction is outside Compose, supports timeout/cancel, preserves bounded sanitized diagnostics and atomically replaces the cache only after valid Netscape output;
- normal and `#HttpOnly_` cookie lines, empty values, duplicate-domain statistics and malformed input are covered by tests;
- browser/source, validation host/URL, generation time and last result persist in `DesktopAppSettings`; cookie values remain only in the dedicated cache file;
- imports and generated caches use best-effort owner-only Unix permissions, while Windows keeps normal user-directory ACL behavior;
- the fake User-Agent checkbox was removed; the existing persisted `userAgentString` is applied consistently through the unified context;
- clearing only removes Seal's cache, immediately invalidates stats and disables runtime Cookies without touching browser login state.
- `DesktopBrowserDetector` discovers supported installations and initialized profiles through platform adapters for Windows, Linux and macOS; filesystem and process detection runs on `Dispatchers.IO`, while only stable hashed profile IDs and display names persist;
- source preference and local-cache provenance are independent: an unavailable or removed browser disables refresh without invalidating an existing usable cache, and importing/clearing a cache does not silently replace the selected browser source;
- the page is split into Cookies source, local cache and verification layers, with explicit re-detection, profile selection, refresh, import/export/location/clear actions and installed-without-profile feedback;
- default verification is an offline domain match against parsed Netscape data; actual media URL verification is a separate advanced yt-dlp path with typed authentication/network/media/extractor failures;
- default English, Simplified Chinese and Traditional Chinese product copy was updated and synchronized to Compose Resources.

Phase A local validation on 2026-09-29:

- `./gradlew :shared:syncAndroidStringsToComposeResources --stacktrace`: passed;
- `./gradlew :desktop:compileKotlin --stacktrace`: passed after correcting one UI field reference;
- focused Cookies, app-settings serialization and metadata command tests: passed;
- `./gradlew :shared:allTests :desktop:test --stacktrace`: passed (`BUILD SUCCESSFUL`, 64 tasks);
- `git diff --check` and the staged secret-pattern scan passed before both Phase A commits.

The capability remains `Partial / Desktop adaptation` until real Chrome/Chromium, Firefox and Edge sessions verify extraction, metadata, Custom Format, final download, retry and failure feedback on native systems. Safari remains a separate macOS-only review.

### Retry stale preferences — locally verified, native branch run pending

The active worktree now merges current live runtime settings into the original request before retry.

Preserved task intent includes format, subtitles, output directories and title overrides. Refreshed runtime context includes cookies/browser source, aria2c, fragment concurrency, debug, proxy, user agent, rate limit, IPv4 and download-archive policy. Privacy is fail-closed: either snapshot enabling `privateMode` keeps the retry private, and the merged request controls queue persistence.

`DesktopDownloadRetryPreferencesTest` now explicitly locks request URL/type, custom-command directory, subtitle switches, format fields, video/audio directories, output template, title override, clip ranges and split-by-chapter as task intent. The focused retry test and the full Desktop test suite passed on 2026-09-29.

### SponsorBlock empty category — locally verified

`DownloadPlanFactory` now omits the SponsorBlock option for a blank category and preserves explicit category values. It does not silently broaden blank to `all`.

Focused shared tests cover blank and explicit categories. The focused `DownloadPlanFactoryTest` and full Shared test suite passed on 2026-09-29.

### Dependency health and source ownership — locally verified, native matrix pending

Dependency resolution now distinguishes `Missing`, `Healthy` and `Broken` instead of trusting file existence. yt-dlp, ffmpeg and aria2c are probed with bounded version commands; stdout, stderr, exit code and timeout diagnostics are retained. Results are cached by tool name, absolute path, modification time, size and executable state, with explicit invalidation after app-managed downloads.

Ownership remains explicit:

- broken `system` dependencies are never overwritten by Seal and remain package-manager repairs;
- broken `selfhost` dependencies are eligible for app-private repair;
- `packaged` dependencies are read-only and updates install an app-private replacement;
- `auto` prefers a healthy private dependency, then a healthy system dependency, and never treats a broken PATH shim as usable;
- aria2c remains optional and a temporary failed probe no longer rewrites the saved user preference.

Focused fake-runner tests cover success, non-zero exit, timeout, missing/non-executable files, cache invalidation, binary replacement in both directions, path replacement races and ownership policy. `DesktopDependencyHealthProbeTest`, `DesktopDependencyPolicyTest` and the full Desktop test suite passed locally on 2026-09-29. The updated Windows/Linux/macOS native smoke matrix still needs a branch run before cross-platform verification is complete.

### DownloadPreferences storage and DesktopAppPaths — locally verified; native launch evidence pending

`DesktopPreferencesStorage` now follows the same `Json` / `DualWrite` / `Sqlite` selection as queue, history and app settings. Existing `settings.json` remains readable, missing legacy fields are filled from Desktop defaults, corrupt JSON is quarantined, and JSON writes are atomic. Dual mode treats JSON as the compatibility source and mirrors SQLite; SQLite mode migrates JSON when the new `preferences_state` row is absent and falls back to JSON if a SQLite write fails.

Focused migration/backend tests passed, and `desktopStorageSelfCheck` passed independently for json, dual and sqlite on 2026-09-29.

`DesktopAppPaths` is now the sole application-path policy for state, cache, data, database, settings, Cookies, download archive, app-managed binaries and temporary files. Linux retains XDG behavior. Windows resolves writable state/data below Local AppData and macOS below Application Support, with platform cache roots. When those native state locations have no data, known files from the historical `~/.local/state/seal` are copied without deleting the source; a copy failure keeps the legacy directory active. Pure path and migration tests pass, but real packaged Windows/macOS launch and migration evidence remains pending.

## P1 — Current User-Visible Gaps Confirmed In Code

### Desktop application updates are an honest manual flow

The inactive auto-update switch, update-channel choices and no-op check button have been removed from Desktop UI. The About and update pages now state that Seal Desktop does not install application updates automatically and open this repository's GitHub Releases page. The old `autoUpdateEnabled` and `updateChannel` fields remain readable only for app-settings schema compatibility.

This closes the misleading-entry defect without claiming a three-platform self-updater. Automatic release checks, package selection and platform-native replacement remain separately deferred product work.

### Download archive management is implemented; UI acceptance remains

`DesktopDownloadArchiveService` now owns exact entry parsing, count/contains/precheck, editable reads, atomic save and clear operations. Normal and custom-command downloads no longer report yt-dlp's exit-0 archive skip as an ordinary completion; normal downloads also precheck known extractor/media IDs and surface the localized archive explanation without adding history.

`Settings -> General -> Manage download archive` now exposes path/count/content, refresh, opening the containing folder, explicit edit/save and confirmed clear. A session-only editor consumes the existing service without changing its atomic backend. It retains drafts on failures, confirms discarding on close/refresh, blocks writes while normal/custom-command tasks are active, and rejects a stale snapshot after a detected external change. Clearing removes archive IDs only, not downloaded files; existing archive-skip feedback remains untouched.

The focused service/editor tests passed locally (16 tests). The capability stays **Partial / Human-check pending** until compact/wide layout, themes/locales, selection/keyboard/scroll, confirmations and native folder-opening behavior are reviewed. Simultaneous external-process writes are not transactionally locked across the read/check/write boundary; do not edit the archive concurrently outside Seal.

### Desktop user-visible hard-coded string sweep — locally verified

The 2026-09-29 sweep moved the known Desktop runtime strings into the Android XML source and synchronized Compose resources. Covered areas include:

- custom-command validation and notifications;
- download queue status, notifications and dependency diagnostics;
- dependency source/health summaries, downloader and environment-setup logs;
- history error dialogs and import/export chooser titles;
- video/audio/custom-command directory chooser titles;
- Desktop unavailable placeholders, About package label, update channel labels and copy accessibility text.

Default, Simplified Chinese and Traditional Chinese values were added where translation was reliable. Other locales intentionally fall back to default instead of receiving machine-filled English copies. Technical identifiers such as `OPUS`, `M4A`, package IDs, URLs, SponsorBlock category tokens and issue-tracker names remain literal by design.

The Android-to-Compose resource sync plus Desktop compilation passed on 2026-09-29, and a fresh targeted scan found no remaining user-visible hard-coded English or Chinese text in the covered paths.

### Compose Resources reflection is isolated and locally verified

`Main.kt` no longer contains Compose Resources internal class names, method names or proxy construction. `DesktopResourceEnvironmentAdapter` now caches the original system environment before provider replacement and contains all reflection behind a controller/bridge boundary. Missing internal APIs, provider installation failures and locale qualifier failures fall back to the default or captured system environment instead of blocking startup.

Focused tests cover initialization order, the current Compose Resources API, locale overrides and failure fallback. Desktop compilation and the focused adapter test passed locally on 2026-09-29. Packaged language switching on Windows/macOS/Linux remains native Human-check evidence, especially after future Compose upgrades.

### Custom format type wiring and RC UI implementation

The worktree passes `downloadType` into `FormatPageImpl` through an explicit `CustomFormatSelectionPolicy`: Audio is audio-only and disables multi-audio selection, while Video/Playlist preserve video/mixed formats and the configured multi-audio behavior. The pure policy test, Desktop compilation and full Desktop test suite passed locally on 2026-09-29. Visual parity remains a separate human-check surface.

The 2026-10-08 iteration adds a finite-duration/non-live clip editor with RangeSlider, timestamp drafts, visible validation, reset and clip/chapter mode switching. Focused thumbs also support one-second arrows/Home/End with RTL-aware horizontal direction and non-crossing bounds. Invalid drafts preserve the last valid slider range and cannot silently submit a full-video download. Search retains original category indexes and hidden selections; format cards expose selection and the collapsed download FAB has a label. Empty/error/loading states preserve request context and retry/back actions. Additional Settings exposes only enabled options in a single-line collapsed summary, preserving header height during animation.

Remaining acceptance: actual shell/window navigation breakpoints, OS DPI, keyboard/focus/hover/scroll, animation feel and native media download behavior. Compose scene tests do not approve these human-only items. See the linked UI note for exact automated and unverified boundaries.

`YtDlpMetadataFetcher` now uses an injectable process runner that drains stdout/stderr concurrently, bounds diagnostics/output, enforces a timeout and destroys/reaps the process on timeout or coroutine cancellation. `MetadataRuntimeOptions` keeps proxy, Cookies/User-Agent, IPv4 and format sorting aligned with final runtime intent without leaking output/archive/post-processing options or a concrete format ID into `-J`. Fake-process tests cover success with warning stderr, non-zero exit, large stderr, timeout, cancellation, invalid JSON and spawn failure; native yt-dlp behavior remains part of the real-download Human-check.

### Subtitle language matching is shared and locally verified

Android and Desktop Custom Format now use the same platform-neutral `SubtitleLanguageMatcher` for the persisted comma-separated regex contract. Exact codes, patterns such as `en.*` / `.*-orig`, multiple patterns and malformed-pattern fail-safe behavior have pure Shared tests. Desktop applies the matcher consistently to normal and automatic captions and does not preselect captions when subtitle download is disabled.

The focused matcher test, Desktop compilation and Android `genericDebug` Kotlin compilation passed on 2026-09-29. The Android compile required only command-line JVM proxy properties for the host's known Gradle TLS/TUN issue; repository configuration was not changed.

## P2 — Planned Product Work Not To Mislabel As Bugs

See `feature-roadmap.md` for the full contract.

Important current examples:

- playlist item selection;
- multi-URL input and Saved URLs;
- download-history multi-select/bulk actions;
- automatic application update behavior beyond the implemented manual flow;
- archive management UI acceptance (implementation now present);
- remaining custom-format parity;
- private-directory product decision.

## P2 — Maintenance Debt

### Build toolchain

Current observed baseline:

- Gradle wrapper: `8.10.2`;
- Android Gradle Plugin: `8.7.2`;
- Kotlin: `2.0.20`;
- Compose Multiplatform: `1.7.0`.

The old audit already recorded Gradle 9 deprecation warnings and an AGP/Kotlin tested-version warning. Treat dependency/toolchain upgrades as a dedicated change, not incidental cleanup.

### Inno language generation

The audit still flags `EmitLanguagesSection` provenance/clarity as maintenance debt. Revalidate the current workflow before changing it.

## Recommended Resume Order

1. Review the complete uncommitted RC diff, choose one candidate SHA, then run dependency and native packaging workflows on Windows x64, Linux x64, macOS x64 and macOS arm64; record each result independently.
2. Re-test the reporter's exact Bilibili collection and compare standalone versus playlist-entry format metadata; capture sanitized `-f`/`-S` arguments without Cookies.
3. Run the dependency-dialog, clip/custom-format, History and archive Human Review Packets across compact/wide windows, light/dark themes and English/zh-CN/zh-TW.
4. Run the Desktop Cookies Human Review Packet against real Chrome/Chromium, Firefox and Edge sessions, with Safari as a separate macOS check; do not mark the capability Implemented from fake-process tests alone.
5. Verify packaged locale switching, legacy-state migration, SQLite startup, tool executable bits and `THIRD_PARTY_VERSIONS.txt` inclusion on every affected native package.
6. Keep playlist item selection, real multi-URL tasks, Saved URLs, History bulk actions, automatic updates, stable dependency pinning/action SHA policy and broad toolchain upgrades outside this RC reliability batch.

## Recently Completed Baseline Worth Preserving

Do not casually regress these previously closed areas:

- Desktop video/audio/custom-command directories reach execution.
- Desktop aria2c no longer uses Android `libaria2c.so` semantics.
- aria2c is optional and detected separately from required yt-dlp/ffmpeg.
- Desktop private mode skips persistent history and queue recovery data.
- crop artwork is wired into Desktop execution.
- embedded subtitles force effective MKV remux behavior.
- release packaging has explicit SQLite smoke coverage logic.

These are historical completion claims; rerun focused validation when modifying their paths.

## How To Update This Page

Keep this file short.

After meaningful work:

- remove or rewrite resolved current gaps;
- add newly discovered blockers;
- record the newest resume commit/date;
- move product capability status into `feature-roadmap.md`;
- move long diagnostics into a dated audit/history note;
- move durable decisions into `project-memory.md`.
