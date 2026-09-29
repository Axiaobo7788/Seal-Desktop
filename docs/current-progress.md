# Seal-Desktop Current Progress

> Role: short current-state page for resuming work.
>
> Refreshed: 2026-09-29
>
> Main branch last observed commit: `2a7d1c45b41ddbcede9c01d667a4757781a4e544` — `fix(desktop): harden dependency setup and release packaging` (2026-08-17).
>
> Active resume branch: `chore/agent-governance-refresh`. Last reviewed implementation commit: `152162c8` (`feat(desktop): refine cookie source and validation UI`). The two Phase A implementation commits are local-only; this branch has not been pushed or validated by fresh native workflows.
>
> This page is the current resume point. Revalidate entries against code before changing behavior. Product capability status lives in `feature-roadmap.md`.

## Resume Summary

The project resumed on 2026-09-28 after more than one month of limited activity.

The latest work closed the local backend/model phases for Cookies, retry semantics, dependency health, DownloadPreferences storage, application paths, download archive handling, subtitle matching, resource-environment isolation and release provenance. Desktop Cookies now also has real browser/profile discovery and its intended three-layer product UI, but native browser/account and visual acceptance remain open. Archive management UI, remaining Custom Format/UI parity and native package evidence are also still open.

The pre-resume dirty worktree was stashed with untracked files, the governance branch was checked out, and the stash was reapplied. A full safety copy remains in `stash@{0}`; the local-only `main` commit `d6f8231e` also remains reachable. Do not drop the stash until the restored changes are reviewed and committed deliberately.

A new triage rule is now active: unfinished porting/planned capability is not automatically a bug. Check `feature-roadmap.md` before acting on issue reports.

Upstream Seal activity observed after the pause is mostly README sponsor automation. The latest stable yt-dlp release observed on 2026-09-28 is [`2026.08.19`](https://github.com/yt-dlp/yt-dlp/releases/tag/2026.08.19). Packaging and in-app download paths still intentionally allow moving `latest`/nightly URLs, but Full workflow artifacts now record the actual tool version, SHA256, source type/URL and build commit in `THIRD_PARTY_VERSIONS.txt`.

## P0 — Verify Before New Feature Work

### Native release evidence

Latest inspected native packaging evidence at the expected main baseline (`2a7d1c45`, 2026-08-17):

- [Windows x64 portable/package run 32002024682](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024682): success;
- [Linux x64 portable/package run 32002024745](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024745): success;
- [macOS package run 32002024659](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024659): arm64 failed at `Smoke test Lite app and installed PKG`; x64 was canceled by that matrix failure.

The latest scheduled dependency-download smoke inspected on 2026-09-28 is [run 35838426850](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/35838426850): Linux x64 and macOS x64 succeeded, macOS arm64 failed while setting up JDK 21, and Windows x64 failed in the dependency source/package-manager policy tests. The last all-green matrix remains [run 31594392569](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/31594392569) from 2026-08-12.

The 2026-09-29 recovery iteration contains two targeted CI repairs, but no fresh native run exists yet:

- dependency smoke uses Temurin 21 instead of JetBrains Runtime because that job tests command-line dependencies, not Compose rendering;
- the macOS/Linux app-image smoke no longer uses Bash 4-only lowercase expansion, which is unavailable in macOS system Bash 3.2;
- the macOS packaging matrix has `fail-fast: false`, preserving x64 and arm64 evidence independently;
- the latest failed Windows policy run predates the current Room/KSP and dependency-policy fixes, so its old failure was not suppressed or weakened.

The active local branch contains commits that are not on its remote tracking branch, so the current implementation has no native workflow evidence yet. Do not reuse older main/branch runs as proof for these changes. Pushing or dispatching workflows was outside this local implementation round.

Action:

- run the updated dependency smoke on Windows x64, Linux x64, macOS x64 and macOS arm64;
- run the updated macOS package workflow for both architectures and inspect Lite app plus installed PKG launch separately from JDK setup;
- keep OS/architecture results separate;
- record new run links/results here, not as timeless claims in project memory.

### Release provenance is partially closed; native workflow evidence pending

Windows, Linux and both macOS architecture workflows now generate and smoke-check a bundled `THIRD_PARTY_VERSIONS.txt` for yt-dlp, ffmpeg and ffprobe. The manifest records the binaries actually packaged, including version output, SHA256, size, moving/pinned source classification, configured source URL and build commit.

The release workflow no longer combines each platform's unrelated latest successful run. It resolves one target commit, requires all platform workflows to have succeeded at that exact `head_sha`, and publishes `BUILD_PROVENANCE.txt` plus `SHA256SUMS`. YAML, Bash blocks, the async github-script JavaScript and the manifest generator passed local static checks on 2026-09-29. A real Actions build/release dry run is still required; moving dependency URLs and action SHA pinning remain open policy work.

### Local baseline validation is verified; native evidence remains pending

The direct 2026-09-29 Gradle retry again failed during root-project classpath resolution because Gradle's Apache HTTP client received `SSLHandshakeException: Remote host terminated the handshake` from Google Maven. A standalone Java HTTPS probe and `curl` both returned HTTP 200, isolating the failure to the host Gradle/TUN route rather than source code.

Without changing repository sources or Gradle repository configuration, the build was rerun through the host's existing local mixed proxy using command-line JVM proxy properties. This reached source compilation and completed the required local validation:

- `:desktop:compileKotlin`: passed (`BUILD SUCCESSFUL`, 19 tasks);
- `:shared:allTests`: passed (`BUILD SUCCESSFUL`, 51 tasks, including Desktop and Android debug/release tests);
- `:desktop:test`: passed (`BUILD SUCCESSFUL`, 28 tasks);
- focused `DownloadPlanFactoryTest` + `DesktopDownloadRetryPreferencesTest`: passed;
- focused `DesktopDependencyHealthProbeTest`: passed;
- focused `CustomFormatSelectionPolicyTest`: passed;
- focused `DesktopDependencyPolicyTest` + `AndroidStringsTest`: passed;
- `:shared:syncAndroidStringsToComposeResources :desktop:compileKotlin`: passed and the sync task was up-to-date.

The retry-focused test now explicitly locks the request URL/type, custom-command directory, subtitle switches, format fields, output paths/template, title, clips and split-by-chapter while refreshing only the documented runtime context. No product behavior changed; the request merge was extracted as an internal testable helper.

Static checks completed on 2026-09-29:

- Android and Compose default/Simplified Chinese/Traditional Chinese string resources remain synchronized;
- the user-visible hard-coded-string rescan found only allowed codec names, URLs, package IDs, CLI/category tokens, filenames and internal self-check/debug text;
- changed workflows parse as YAML, their Bash blocks pass `bash -n`, and github-script JavaScript passes an async-wrapper syntax check;
- `actionlint` passes for the four affected workflows via `go run github.com/rhysd/actionlint/cmd/actionlint@latest`;
- `git diff --check` passes;
- PowerShell scripts were not parsed by a local `pwsh` because this host does not provide it; Windows Actions remains the required native check.

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

### Download archive backend is verified; management UI remains

`DesktopDownloadArchiveService` now owns exact entry parsing, count/contains/precheck, editable reads, atomic save and clear operations. Normal and custom-command downloads no longer report yt-dlp's exit-0 archive skip as an ordinary completion; normal downloads also precheck known extractor/media IDs and surface the localized archive explanation without adding history.

Focused archive tests cover missing/empty files, exact duplicate detection, malformed lines, atomic edit/clear and concurrent read/write snapshots. The full Desktop test suite and compilation passed locally on 2026-09-29. `GeneralSettingsPage.kt` still lacks the planned view/edit/clear/open management UI, so the product capability remains Partial.

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

### Custom format type wiring is locally verified

The worktree passes `downloadType` into `FormatPageImpl` through an explicit `CustomFormatSelectionPolicy`: Audio is audio-only and disables multi-audio selection, while Video/Playlist preserve video/mixed formats and the configured multi-audio behavior. The pure policy test, Desktop compilation and full Desktop test suite passed locally on 2026-09-29. Visual parity remains a separate human-check surface.

Remaining high-risk areas to re-check when touching this page:

- clip range editing;
- search field and selection animation parity.

These are strong human-check candidates.

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
- download archive management;
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

1. Push the focused branch only after review, then run dependency smoke and native packaging on Windows x64, Linux x64, macOS x64 and macOS arm64; record each result independently.
2. Run the Desktop Cookies Human Review Packet against real Chrome/Chromium, Firefox and Edge sessions, with Safari as a separate macOS check; do not mark the capability Implemented from fake-process tests alone.
3. Verify packaged locale switching, legacy-state migration, SQLite startup and `THIRD_PARTY_VERSIONS.txt` inclusion on every affected native package.
4. Implement the download-archive management UI without reopening the already-tested backend contract.
5. Re-enter playlist/input/history/custom-format parity work from `feature-roadmap.md`, using human checkpoints for visual and animation decisions.
6. Treat automatic updates, stable dependency pinning/action SHA policy and broad toolchain upgrades as separate scoped work.

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
