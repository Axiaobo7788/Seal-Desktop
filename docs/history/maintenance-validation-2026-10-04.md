# Native Validation And Archive Management Evidence

Date: 2026-10-04. This is dated evidence, not the current roadmap or durable project memory.

Resume sources: [current progress](../current-progress.md), [feature roadmap](../feature-roadmap.md), [engineering contract](../../AGENTS.md).

## Scope And Repository State

- Initial branch: `chore/agent-governance-refresh`; initial worktree was clean.
- Initial local and GitHub tracking HEAD: `c76512b8472e549190b2f135efa92ada7f364836`.
- GitHub fetch succeeded. GitLab fetch failed because the shell could not obtain a username; no credentials were written into the repository.
- No main merge, release, force-push, stash deletion or replacement of user state occurred.
- Local implementation commits: `b1222fca` (dependency transfer/ZIP repair), `4b48ce7a` (native CI integrity), `d1766eab` (archive management).
- Commits were not pushed. Phase A is access-blocked, not sealed. Phase B automatic validation passed; visual/native acceptance remains open. Phases C-L were not implemented in this round.

## Phase A: Native Evidence

The Actions API returned `total_count: 0` for the active branch. The available GitHub connector could read runs/logs but had no dispatch tool; `gh` and Actions tokens were absent, and the available browser was logged out. Therefore no current-branch native workflow was dispatched or reported as passing.

Latest inspected scheduled dependency run: [36698411228](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/36698411228), 2026-09-30, **main at `2a7d1c45b41ddbcede9c01d667a4757781a4e544`**.

| Platform | Actual stage/result | Evidence boundary |
| --- | --- | --- |
| Windows x64 | JetBrains JDK setup failed with an API rate-limit response; source tests/download were not executed | Not an application-launch failure and not evidence about this branch |
| Linux x64 | Dependency tests/download succeeded | Older main only |
| macOS x64 | Dependency tests/download succeeded | Older main only |
| macOS arm64 | JDK setup/tests succeeded; ffprobe ZIP extraction failed with `Unexpected end of ZLIB input stream` | Failed archive/transfer; no package/app/PKG verdict |

Older package evidence remains separate: [Windows 32002024682](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024682) and [Linux 32002024745](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024745) succeeded on main; [macOS 32002024659](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024659) failed installed Lite PKG startup on arm64 and canceled x64. None closes the current branch's native debt.

### Diagnosed Repair

The old ZIP installer wrote directly to final tool paths during extraction. A truncated ffprobe archive could leave a partial executable at that path.

The repair stages ZIP payloads, requires a readable central directory, checks nonzero size/entry size/CRC, checks required tools and only then promotes each file. A corrupt or missing payload leaves existing binaries untouched. IO transfer/extraction failures retry at most three times; cancellation and non-IO contract failures are not retried. HTTP connect/request limits and empty/Content-Length checks were added. Tool source ownership was not changed.

This is not a new upstream-signature verification scheme. ZIP CRC detects damaged payloads, not malicious upstream content. Promotions are atomic per file where supported, not a transaction over several binaries. The existing tar.xz path was not rewritten in response to an observed ZIP failure.

### Verification Improvements

- Native dependency smoke runs all Desktop tests and Shared Desktop tests, then json/dual/sqlite self-checks, before real dependency download/execution.
- Windows native CI parses the packaging PowerShell scripts. The local host has no `pwsh`; this check is prepared but has not run on this branch.
- Windows app-image smoke uses the actual configured EXE/BAT/CMD target. It no longer substitutes a sibling BAT when an EXE was requested.
- Lite smoke rejects bundled yt-dlp/ffmpeg/ffprobe or provenance. Full smoke checks one manifest and one binary per tool, size/SHA256, real version execution, source metadata and expected build commit.
- Windows staged/installed Lite and Full, macOS app/installed PKG Lite and Full, and Linux Lite/Full app images and extracted DEBs call that verifier.
- Linux Full extracted DEB startup now receives the same SQLite/liveness check as Lite.
- No smoke assertion was relaxed. Temurin dependency probes and macOS `fail-fast: false` were retained. Packaging JDK configuration was not changed.

### Local Native Result

Host: Manjaro Linux x86_64, kernel `6.18.49-1-MANJARO`, OpenJDK `21.0.12.1`. The existing desktop display was available.

```bash
./gradlew :desktop:createReleaseDistributable --stacktrace
GITHUB_SHA="$(git rev-parse HEAD)" bash .github/scripts/smoke_unix_app_image.sh \
  desktop/build/compose/binaries/main-release/app/Seal \
  desktop/build/compose/binaries/main-release/app/Seal/bin/Seal \
  'Local Linux x64 Lite shrunk app image' 12 true Lite
```

The shrink build passed (1m 28s). Lite payload verification passed. The actual launcher stayed alive for 12 seconds, initialized a 28,672-byte SQLite database in isolated temporary state, and emitted no matched SQLite driver/native-library failure. The smoke process was terminated and its isolated state was removed by the script.

The build included this round's archive product code before its commit. This is local worktree evidence, not a CI artifact attributed to a later docs commit. No manual theme/locale/animation/UI check occurred. Full packages, DEB/RPM, Windows EXE and macOS app/PKG still lack current native evidence.

## Phase B: Archive Management

Classification: **Partial capability / implementation and automatic checks complete / Human-check pending**.

`Settings -> General -> Manage download archive` now provides path and count, raw content/malformed-line feedback, refresh, containing-folder opening, explicit edit/save, confirmed clear and readable failures. Clearing removes IDs, not downloaded files. The existing archive skip result and `DesktopDownloadArchiveService` were not rewritten.

Responsibility split:

- `desktop/download/archive/DesktopDownloadArchiveEditor.kt`: session draft, operation serialization, typed error/notice state, detected stale-snapshot guard and active-download mutation guard.
- `desktop/settings/general/DownloadArchiveDialog.kt`: presentation, edit mode and close/refresh/clear confirmations; native file-manager opening runs on IO.
- `Main.kt`, `SettingsScreen.kt`, `GeneralSettingsPage.kt`: routing and live normal/custom-command activity checks.
- Default, Simplified Chinese and Traditional Chinese Android XML: 18 UI keys; generated Compose Resources were synchronized. Other locales use deliberate default fallback.
- `DesktopDownloadArchiveEditorTest.kt`: missing archive, saved count/content, unsaved refresh, confirmed close/reopen, external append, active jobs, clear, read failure and separate write save/clear failures.

No storage schema or download intent/runtime policy changed. A detected external change rejects the write and retains the draft, but the read/check/write sequence is not locked against a simultaneous independent process. Avoid editing the archive while an external yt-dlp/editor is writing it.

## Automated Validation

| Check | Result |
| --- | --- |
| `./gradlew :shared:syncAndroidStringsToComposeResources :desktop:compileKotlin :shared:allTests :desktop:test --stacktrace` | Passed; 64 tasks, 1m 35s |
| Final `./gradlew :desktop:compileKotlin :shared:allTests :desktop:test --stacktrace` | Passed after the last test addition; 64 tasks, 18s |
| Desktop XML test results | 138 tests, zero failures/errors/skips |
| Shared Desktop/debug/release XML results | 20 tests per target, zero failures/errors/skips |
| `:desktop:test --tests 'com.junkfood.seal.desktop.ytdlp.DesktopAuxiliaryDownloaderTest'` | Passed; 13 tests in final full run |
| `:desktop:test --tests 'com.junkfood.seal.desktop.download.archive.*'` | Passed; 16 tests (8 service + 8 editor) |
| `PYTHONDONTWRITEBYTECODE=1 python .github/scripts/test_verify_packaged_tools.py` | Passed; 8 synthetic/mock tests, no real tools/network required |
| `:desktop:desktopStorageSelfCheck -PstorageBackend=json -PstorageStateDir=<temporary root>` | Passed; isolated state, 16s |
| Same storage self-check with `dual` | Passed; expected corrupt JSON quarantine and SQLite fallback, 15s |
| Same storage self-check with `sqlite` | Passed; isolated SQLite initialization, 16s |
| `actionlint` on the four affected workflows | Passed via `go run github.com/rhysd/actionlint/cmd/actionlint@latest` |
| YAML parse, Python AST, Unix script/workflow Bash syntax | Passed |
| Resource diff | Only intended default/zh-rCN/zh-rTW additions; Android XML and generated Compose synchronized |
| `git diff --check`, staged secret-pattern scans | Passed before implementation commits |
| Local PowerShell / remote native matrix | Not executed; required evidence remains open |

The final Shared targets reuse the earlier passing test outputs when Gradle reports `UP-TO-DATE`; the full Desktop task ran again after focused filtering. No test was deleted or weakened. AGP/Kotlin compatibility, ProGuard duplicate-resource/reflection notes and Gradle 9 deprecation warnings remain visible; no toolchain upgrade or warning suppression was attempted.

## Human Review Packet

### Changed Behavior

Archive management is now reachable in General settings. Dependency installation preserves existing tools when a ZIP is corrupt, and native smoke verifies the actual launcher plus Lite/Full payload identity. All affected code and resource areas are listed above.

### Environment Required

- For native evidence: authorized GitHub Actions access and the pushed governance branch, without running the release workflow.
- Windows x64, Linux x64, macOS x64 and macOS arm64 independently; test Lite and Full app images and installers/packages.
- For UI: mouse, wheel and keyboard; approximately 800x600 and 1440x900 windows; light/dark themes; English, Simplified/Traditional Chinese and one fallback locale.
- For Cookies/native authentication debt: Windows Chrome/Chromium, Firefox and Edge with an authorized test account; Safari is a separate macOS check, not Windows coverage.
- Use disposable OS accounts/VMs for native legacy migration. Back up any real archive before editing/clearing, or use isolated state as below. No cookie values belong in screenshots/logs.

An optional PowerShell launch setup isolates archive, preferences, Cookies and SQLite state (it does not relocate the platform's entire cache/data directory):

```powershell
$env:SEAL_DESKTOP_STORAGE_STATE_DIR = Join-Path $env:TEMP ("seal-review-" + [guid]::NewGuid().ToString("N"))
# Launch the actual installed Seal target from this same shell.
```

On Linux/macOS, use `SEAL_DESKTOP_STORAGE_STATE_DIR="$(mktemp -d)" <actual-launcher>`. The archive path should be below `<isolated-root>/seal/yt-dlp/download-archive.txt`. Isolated state intentionally bypasses legacy migration; test migration separately in a disposable native account.

### Manual Steps And Expected Results

| Step | Expected result |
| --- | --- |
| Push only `chore/agent-governance-refresh` using an authenticated IDE/session, then dispatch `desktop_dependency_smoke.yml` on that branch | Four independent OS/architecture jobs; selected SHA matches the reviewed branch, not main |
| Dispatch `windows_x64_portable.yml`, `linux_x64_portable.yml`, `macos_portable.yml` on the same SHA; do not dispatch release | Inspect JDK, dependency/source tests, payload verification and app/installed package launch separately; both Mac arches retain evidence |
| Inspect Lite/Full manifests and smoke output | Lite has no tool payload; Full SHA256/version/build commit match actual packaged binaries; EXE failures cannot be hidden by BAT substitution |
| Open Archive management with a missing file in isolated state | Path and zero count appear, view is initially read-only, no false read error |
| Edit to `youtube seal-review-1` plus a second line `unrecognized`, then save and reopen | One recognized entry and one malformed line; both lines persist; saved feedback appears |
| Modify the draft, then refresh/close and cancel confirmation | Draft stays intact; confirmed discard loads disk state or closes without saving |
| Append a second valid ID from a separate editor while idle, then try saving the stale draft | Visible external-change error, newer disk content intact, draft still available to copy |
| Start a normal download or custom command, then open the archive dialog | Reads/refresh remain available; edit/save/clear are disabled while tasks are active; available again when tasks stop |
| Click Clear, cancel, then confirm on test state | Cancel changes nothing; confirm empties archive/count only; downloaded test files remain |
| Make the test archive or parent unreadable/unwritable, refresh/save/clear; restore permissions | Readable failure; no false success, save draft retained, refresh/retry recovers |
| Click Open folder; repeat when the test archive does not yet exist | Native file manager opens the containing/nearest existing folder, or shows readable failure and copyable path |
| Test compact/wide, themes/locales, wheel, select/copy/paste, Tab, Escape and dialog enter/exit | No clipped actions/content, correct localized text/fallback, usable editing/scroll, no surprise draft loss; capture actual behavior rather than assume parity |
| In a disposable account, seed a legacy state fixture and launch a native package without override | Known state copies to the native root, legacy source remains, SQLite/preferences/queue survive; failed migration stays on legacy |
| Change app language, including Follow System, zh variants, Hebrew and Indonesian, in packaged apps | Stay on the current settings page; correct resource qualifiers/fallback; no provider recursion |
| On native Windows browsers, extract an authorized test session, fetch metadata/Custom Format, download and retry; then clear only Seal cache and test extraction failure | All execution paths use the same current auth context; missing/invalid cache fails visibly, browser login is not erased, error diagnostics contain no cookie values |
| Download a small authorized public video with archive enabled, then repeat it | Existing localized archive-skip result remains; skipped work is not recorded as a fresh successful history item |

### Evidence To Capture

Record OS/architecture, package flavor, workflow URL and `head_sha`, JDK/dependency versions, installer/app launch logs, payload verifier output and resulting SQLite existence. For UI, record window size/theme/locale/input method, screenshots and short confirmation/scroll/animation recordings. For Cookies, record browser/version and sanitized errors only; never cookies.txt contents, tokens or private URLs.

### Remaining Risk And Rollback

- Phase A access remains blocked; historical main success and local Linux Lite do not establish the four-platform branch matrix.
- Real folder handlers, native launchers/PKG installers, legacy migration and browser permissions still need platform evidence. Linux RPM has no new installed smoke.
- UI appearance, animation feel, accessibility/keyboard interaction and large-archive performance were not manually reviewed.
- The external-change guard is optimistic, not an OS file lock; do not write concurrently with an external process. Multi-tool promotion is not group-transactional.
- Moving upstream URLs remain non-reproducible even with recorded provenance. No signing, stable pin policy or upstream authenticity guarantee was added.
- Code rollback is isolated by the three local commits; revert only the relevant commit if necessary. No schema migration is required. Restore only a backed-up **test** archive after destructive review; do not reset user data or delete browser login state.

Next implementation after this checkpoint: Playlist item selection. SponsorBlock default/all/custom and privateDirectory remain product decisions; toolchain migration remains deferred.
