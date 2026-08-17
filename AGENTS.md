# Seal Desktop Engineering Contract

This file is the mandatory entry point for code changes in this repository. Detailed rationale, matrices, and examples live in [`docs/development-guidelines.md`](docs/development-guidelines.md).

## Read Order

1. Read this file before editing.
2. Read [`docs/development-guidelines.md`](docs/development-guidelines.md) for the affected area.
3. Use [`docs/project-map.md`](docs/project-map.md) to locate modules and flows.
4. Use [`docs/desktop-project-audit-2026-06-15.md`](docs/desktop-project-audit-2026-06-15.md) only for current status and unfinished work.
5. Treat [`docs/android-desktop-progress-tracker.md`](docs/android-desktop-progress-tracker.md) as historical context, not current truth.

When documents disagree, current code and tests win over progress notes. Update the stale document in the same change.

## Before Editing

- Inspect `git status` and preserve unrelated or user-authored changes.
- State the requested outcome and affected modules before widening scope.
- Trace the complete path from UI to persisted settings, plan generation, platform adapter, execution, and user feedback.
- Compare Android behavior when parity is requested, but classify the result as exact parity, Desktop adaptation, intentionally deferred, or unsupported.
- Identify affected strings, storage schema, dependency source, packaging, and platform workflows before implementation.
- Do not turn an audit-only request into code changes without approval.

## Module Boundaries

- `app/` owns Android UI, services, Room, MMKV, Android paths, and Android binary integration.
- `desktop/` owns JVM/Compose Desktop UI, process execution, file pickers, desktop storage, system paths, and packaging behavior.
- `shared/` owns platform-neutral models, pure business rules, download plans, and reusable UI contracts.
- `color/` owns color and theme support shared by the products.
- Do not place AndroidX, Room, MMKV, `libaria2c.so`, Android filesystem assumptions, JVM process APIs, or OS-specific paths in `shared/commonMain`.
- Put platform differences behind parameters, adapters, or `expect`/`actual`; do not branch on OS names in shared business logic.

## Product Boundaries

- A visible setting or action is not complete until its value reaches the execution layer and its result or failure is visible to the user.
- Do not copy Android-only behavior merely to make Desktop look complete. Hide, disable, or explain unsupported behavior.
- Preserve navigation and transient state during resource/theme recomposition unless a reset is part of the requirement.
- UI parity includes semantics, loading/empty/error states, keyboard and mouse behavior, scroll behavior, animation timing, and responsive layout. Similar screenshots alone are insufficient.
- Persisted schema changes require backward-compatible reads, a migration decision, and recovery tests.

## Internationalization

- `app/src/main/res/values*/strings.xml` is the single source of truth for product strings.
- Do not manually maintain product strings only in `shared/src/commonMain/composeResources`; run `:shared:syncAndroidStringsToComposeResources` after editing Android resources.
- Compose UI must use `stringResource(Res.string.<key>)`. Non-composable Desktop paths may use `AndroidStrings`, but must preserve the same qualifier and fallback semantics.
- Reuse an existing key before adding one. Every new key requires a default `values/strings.xml` value.
- Do not mass-copy English or machine translations into locale files to make coverage counts pass. Missing translations must fall back to the default language deliberately.
- Keep formatting placeholders, escaping, plurals, and line breaks compatible across every locale that overrides a key.
- Locale work must account for `zh-Hans -> zh-rCN`, `zh-Hant -> zh-rTW`, `he -> iw`, and `id -> in`, plus `null` as Desktop Follow System.
- Adding a selectable language requires checking Android language options, `DesktopLocaleOptions`, resource qualifiers, display names, persisted tags, and fallback behavior separately.
- User-visible logs, notifications, file chooser titles, validation errors, and dependency setup output are strings too.

## Dependency Sources

- `system` means package-manager or PATH-owned tools. Detect and use them, but never overwrite or shadow them with an automatic app download.
- `selfhost` means Seal-managed binaries in the app-private directory. Only this source may be updated by the in-app downloader.
- Packaged resources are app-private and read-only at runtime.
- `auto` may combine sources according to resolver policy, but must download only missing components.
- `aria2c` is optional; `yt-dlp` and `ffmpeg` are required for the complete download environment. Keep platform executable names out of shared plans.

## Validation

- Documentation only: run `git diff --check` and verify every changed relative link.
- Desktop Kotlin: run `./gradlew :desktop:compileKotlin` plus focused tests for the changed behavior.
- Shared logic or shared UI: run `./gradlew :shared:allTests :desktop:compileKotlin` plus focused Desktop tests where applicable.
- String resources: run `./gradlew :shared:syncAndroidStringsToComposeResources :desktop:compileKotlin` and inspect the generated resource diff.
- Workflow changes: run `actionlint` and validate affected scripts on the matching runner where possible.
- Storage changes: run focused tests and the affected `desktopStorageSelfCheck` backends.
- Packaging or dependency changes: run the relevant app-image/package smoke test on every affected OS and architecture. Do not infer macOS or Windows success from Linux.
- UI changes: report which platform, window size, theme, locale, input method, and animations were manually checked. If not checked, state that gap.
- Never report a platform, package, translation, or visual behavior as verified when only compilation passed.

## Documentation And Completion

- Update the audit checkbox only after the implementation and required validation are complete.
- Record the exact validation command and result; keep historical results dated and separate from current claims.
- Add durable rules here or in `docs/development-guidelines.md`, not in the progress checklist.
- Add current defects and completion status only to `docs/desktop-project-audit-2026-06-15.md`.
- A completed change must include implementation, failure handling, targeted tests, affected localization, platform verification, and documentation synchronization. Explicitly list anything that remains unverified.
