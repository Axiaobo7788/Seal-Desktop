# Seal-Desktop Current Progress

> Role: short current-state page for resuming work.
>
> Refreshed: 2026-09-28
>
> Main branch last observed commit: `2a7d1c45b41ddbcede9c01d667a4757781a4e544` — `fix(desktop): harden dependency setup and release packaging` (2026-08-17).
>
> This page is the current resume point. Revalidate entries against code before changing behavior.

## Resume Summary

The project has been mostly idle for more than one month.

The latest work concentrated on Desktop dependency setup, release shrinking, SQLite runtime safety and native packaging smoke checks. The architecture and build are not assumed broken, but the last audit still contains a mix of unresolved product parity, i18n and native verification debt.

Upstream Seal activity observed after the pause is mostly README sponsor automation; do not assume this means there are no dependency or ecosystem changes. Refresh upstream/dependencies when work touches them.

## P0 — Verify Before New Feature Work

### Native release evidence

The 2026-08-17 audit records that Linux local app-image SQLite smoke passed, but Windows/macOS and Linux DEB native CI paths still required revalidation after the latest shrink/SQLite changes.

Action:

- run/inspect the affected native workflows before calling the release pipeline fully green;
- keep OS/architecture results separate;
- record new run links/results here, not as timeless claims in project memory.

## P1 — Current User-Visible Gaps Confirmed In Code

### Desktop app update page is still a placeholder

`desktop/.../settings/about/UpdateSettingsPage.kt` still exposes:

- auto-update toggle;
- update channel choice;
- "Check for updates" button;

but the button remains `/* TODO desktop check for update */`.

Decision needed:

- either implement a Desktop-safe release check;
- or make the page explicitly manual/unsupported and remove misleading active controls.

This is a product decision and should use human-check mode if behavior is being redefined.

### Download archive has an enable switch but no management UI

`GeneralSettingsPage.kt` currently exposes the archive toggle, but no equivalent view/edit/clear/open management flow.

Before implementation, re-check execution semantics and duplicate feedback so "already archived" cannot look like a normal successful download.

### Desktop i18n debt remains real

Confirmed examples include:

- `FormatPage.kt`: hard-coded Chinese loading/error/empty-state strings;
- `DesktopCustomCommandTaskManager.kt`: `URL is empty`, `Command Started`, `Command Completed`, `Command Error`;
- `GeneralSettingsPage.kt`: dependency detection summary strings such as `missing`, `optional`, and `Detecting dependencies...`.

Sweep user-visible Desktop strings before declaring localization complete.

### Custom format parity still needs revalidation

`CustomFormatSelectionSheet` receives `downloadType`, but the current `FormatPageImpl` signature does not receive it. The page therefore still needs a deliberate review of audio/video format visibility and parity semantics.

Other historical high-risk areas to re-check when touching this page:

- regex-like subtitle preference matching;
- clip range editing;
- search field and selection animation parity.

These are strong human-check candidates.

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

1. Run a clean focused compile/test smoke on current main.
2. Inspect latest native packaging workflow results after the 2026-08-17 change.
3. Fix misleading user-visible gaps before broad parity work:
   - Desktop app update page;
   - download archive management/feedback.
4. Sweep confirmed Desktop hard-coded user-visible strings.
5. Re-enter custom format / playlist / history parity work with human checkpoints for visual and product decisions.
6. Only then do broad toolchain upgrades unless a security/compatibility issue makes them urgent.

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
- move long diagnostics into a dated audit/history note;
- move durable decisions into `project-memory.md`.
