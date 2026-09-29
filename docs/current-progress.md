# Seal-Desktop Current Progress

> Role: short current-state page for resuming work.
>
> Refreshed: 2026-09-28
>
> Main branch last observed commit: `2a7d1c45b41ddbcede9c01d667a4757781a4e544` — `fix(desktop): harden dependency setup and release packaging` (2026-08-17).
>
> Active resume branch: `chore/agent-governance-refresh` at `b2846b196f67dce1e9f46708588d520779c33c01` (2026-09-28), based on the expected main baseline.
>
> This page is the current resume point. Revalidate entries against code before changing behavior. Product capability status lives in `feature-roadmap.md`.

## Resume Summary

The project resumed on 2026-09-28 after more than one month of limited activity.

The latest work concentrated on Desktop dependency setup, release shrinking, SQLite runtime safety and native packaging smoke checks. The architecture and build are not assumed broken, but the last audit still contains a mix of unresolved product parity, i18n and native verification debt.

The pre-resume dirty worktree was stashed with untracked files, the governance branch was checked out, and the stash was reapplied. A full safety copy remains in `stash@{0}`; the local-only `main` commit `d6f8231e` also remains reachable. Do not drop the stash until the restored changes are reviewed and committed deliberately.

A new triage rule is now active: unfinished porting/planned capability is not automatically a bug. Check `feature-roadmap.md` before acting on issue reports.

Upstream Seal activity observed after the pause is mostly README sponsor automation. The latest stable yt-dlp release observed on 2026-09-28 is [`2026.08.19`](https://github.com/yt-dlp/yt-dlp/releases/tag/2026.08.19); packaging and in-app download paths still use moving `latest`/nightly URLs, so provenance and reproducibility remain maintenance work.

## P0 — Verify Before New Feature Work

### Native release evidence

Latest inspected native packaging evidence at the expected main baseline (`2a7d1c45`, 2026-08-17):

- [Windows x64 portable/package run 32002024682](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024682): success;
- [Linux x64 portable/package run 32002024745](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024745): success;
- [macOS package run 32002024659](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/32002024659): arm64 failed at `Smoke test Lite app and installed PKG`; x64 was canceled by that matrix failure.

The latest scheduled dependency-download smoke inspected on 2026-09-28 is [run 35838426850](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/35838426850): Linux x64 and macOS x64 succeeded, macOS arm64 failed while setting up JDK 21, and Windows x64 failed in the dependency source/package-manager policy tests. The last all-green matrix remains [run 31594392569](https://github.com/Axiaobo7788/Seal-Desktop/actions/runs/31594392569) from 2026-08-12.

Action:

- diagnose macOS arm64 Lite app/PKG launch smoke independently from the JDK setup failure;
- diagnose the current Windows dependency-policy test before trusting scheduled dependency downloads;
- keep OS/architecture results separate;
- record new run links/results here, not as timeless claims in project memory.

### Local baseline validation is environment-blocked

On 2026-09-28, `./gradlew :desktop:compileKotlin` was attempted repeatedly. The first attempt failed resolving a Compose/Kotlin plugin artifact from `plugins.gradle.org`; later attempts progressed through `buildSrc` but failed resolving Android Gradle Plugin and Room artifacts from `dl.google.com`. Java 17, 21 and 25 default HTTPS probes ended with `SSLHandshakeException: Remote host terminated the handshake`, while `curl` could reach the same URLs. A direct JDK 21 probe succeeded when forced to IPv6, but Gradle's Apache HTTP transport still failed after the running daemon was confirmed to have `java.net.preferIPv6Addresses=true`. The temporary repository setting used for that probe was removed.

Consequences:

- source compilation was not reached, so these failures are not evidence of a Kotlin source regression;
- `./gradlew --offline :shared:allTests :desktop:test --stacktrace` failed during root-project classpath resolution because Room 2.6.1 and AGP 8.7.2 artifacts are not cached;
- `./gradlew --offline :shared:desktopTest --tests com.junkfood.seal.download.DownloadPlanFactoryTest :desktop:test --tests com.junkfood.seal.desktop.download.DesktopDownloadRetryPreferencesTest --stacktrace` failed at the same configuration boundary;
- therefore `:shared:allTests`, `:desktop:test` and the new focused tests are not verified;
- the official Gradle 8.10.2 distribution was cached outside the repository only to restore the wrapper; no generated dependency cache was committed;
- rerun the required compile/tests on a host where Java can establish HTTPS connections before marking the active repairs complete.

## P1 — Issue #4 Triage

GitHub Issue #4 contains four useful reports, but they are not one class of problem.

### Cookies metadata failure — partial feature, not standalone bug work

Desktop cookies is still an unfinished Desktop-specific feature.

The 2026-09-28 path trace confirmed all of these breaks:

- the settings page launches yt-dlp to copy browser Cookies into a Netscape file, but success does not persist the chosen browser or enable Cookies;
- extraction stdout/stderr is discarded and failure closes the dialog without actionable feedback;
- the User-Agent checkbox on the Cookies page is local UI state and changes neither extraction nor persisted preferences;
- metadata fetch accepts proxy state only and does not consume Cookies;
- normal downloads use the global Netscape file while custom commands may use `--cookies-from-browser`, so there is no single Desktop cookie-context resolver;
- retry now refreshes cookie/browser preferences, but it still inherits the incomplete metadata/source contract.

The intended Desktop design is now explicit in project memory/roadmap:

- use the installed/system browser session;
- do not port Android's embedded WebView;
- keep file import only as fallback;
- make metadata + final download + retry share one resolved cookie context.

When implementing this, fix the end-to-end cookie contract rather than only adding one metadata argument.

### Retry stale preferences — implementation present, validation blocked

The active worktree now merges current live runtime settings into the original request before retry.

Preserved task intent includes format, subtitles, output directories and title overrides. Refreshed runtime context includes cookies/browser source, aria2c, fragment concurrency, debug, proxy, user agent, rate limit, IPv4 and download-archive policy. Privacy is fail-closed: either snapshot enabling `privateMode` keeps the retry private, and the merged request controls queue persistence.

Focused tests exist in `DesktopDownloadRetryPreferencesTest`, but Gradle dependency resolution blocked execution on 2026-09-28.

### SponsorBlock empty category — implementation present, validation blocked

`DownloadPlanFactory` now omits the SponsorBlock option for a blank category and preserves explicit category values. It does not silently broaden blank to `all`.

Focused shared tests were added, but Gradle dependency resolution blocked execution on 2026-09-28.

### Local createDistributable dependency bootstrap — tooling/packaging gap

Local contributor builds can lack the CI-populated app-private binaries and then fall through to system PATH.

Treat this as developer dependency/bootstrap/provenance work, not as proof that every external broken `yt-dlp` is an application runtime bug.

## P1 — Current User-Visible Gaps Confirmed In Code

### Desktop app update page is still a placeholder

`desktop/.../settings/about/UpdateSettingsPage.kt` still exposes:

- auto-update toggle;
- update channel choice;
- "Check for updates" button;

but the button remains `/* TODO desktop check for update */`.

This is already classified in `feature-roadmap.md` as Partial / Decision needed.

### Download archive has an enable switch but no management UI

`GeneralSettingsPage.kt` currently exposes the archive toggle, but no equivalent view/edit/clear/open management flow.

This is a planned partial feature rather than a regression.

### Desktop i18n debt remains real

Confirmed examples include:

- `DesktopCustomCommandTaskManager.kt`: `URL is empty`, `Command Started`, `Command Completed`, `Command Error`;
- `GeneralSettingsPage.kt`: dependency detection summary strings such as `missing`, `optional`, and `Detecting dependencies...`;
- download controller/notifier progress and error text still includes hard-coded Chinese/English strings.

Sweep user-visible Desktop strings before declaring localization complete.

### Custom format type wiring is present but unverified

The restored worktree now passes `downloadType` into `FormatPageImpl` through `audioOnly` and `allowMultiAudio`, and uses the type when creating the selected-format task. This corrects the prior document claim that the value stopped at `CustomFormatSelectionSheet`, but compilation and behavioral validation remain blocked by local dependency resolution.

Remaining high-risk areas to re-check when touching this page:

- regex-like subtitle preference matching;
- clip range editing;
- search field and selection animation parity.

These are strong human-check candidates.

## P2 — Planned Product Work Not To Mislabel As Bugs

See `feature-roadmap.md` for the full contract.

Important current examples:

- playlist item selection;
- multi-URL input and Saved URLs;
- download-history multi-select/bulk actions;
- application update behavior;
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

1. Rerun `:desktop:compileKotlin`, `:shared:allTests`, `:desktop:test` and the two new focused tests on a host without the Java HTTPS failure.
2. Diagnose the macOS arm64 package launch smoke and Windows dependency-policy test, keeping those failures separate from the unrelated macOS arm64 JDK setup failure.
3. Review and commit the focused retry/SponsorBlock repairs only after the tests pass.
4. Implement the Desktop Cookies feature as one end-to-end system-browser flow rather than a metadata-only patch.
5. Continue other misleading/partial product surfaces:
   - Desktop app update page;
   - download archive management/feedback.
6. Sweep confirmed Desktop hard-coded user-visible strings.
7. Re-enter playlist/input/history/custom-format parity work from `feature-roadmap.md`, using human checkpoints for visual/product decisions.
8. Only then do broad toolchain upgrades unless a security/compatibility issue makes them urgent.

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
