# Seal Desktop Engineering Contract

This file is the mandatory entry point for code and documentation changes in this repository.

The repository separates **durable project memory**, **current progress**, **engineering rules**, and **historical evidence**. Do not collapse them back into one growing audit file.

## Read Order

1. Read this file before editing.
2. Read [`docs/project-memory.md`](docs/project-memory.md) for durable project context and known traps.
3. Read [`docs/current-progress.md`](docs/current-progress.md) for the current resume point, priorities, and verification debt.
4. Read [`docs/development-guidelines.md`](docs/development-guidelines.md) for the affected engineering area.
5. Use [`docs/project-map.md`](docs/project-map.md) to locate modules and flows.
6. Read [`docs/agent-workflow.md`](docs/agent-workflow.md) when deciding iteration/review behavior.
7. Use [`docs/desktop-project-audit-2026-06-15.md`](docs/desktop-project-audit-2026-06-15.md) as historical evidence and detailed backlog context, not as the first source of current truth.
8. Treat [`docs/android-desktop-progress-tracker.md`](docs/android-desktop-progress-tracker.md) as historical migration context only.

When documents disagree, current code and tests win. Update the stale current-state document in the same change.

## Operating Mode

Every task runs in one of two modes.

### Self-iteration mode

This is the default for normal implementation and maintenance work.

The agent may repeat this loop without asking for approval between each attempt:

`inspect -> change contract -> implement -> focused validation -> diagnose -> repair -> broader validation -> docs sync`

Rules:

- Keep the loop scoped to the requested outcome and affected modules.
- Prefer the smallest focused check while iterating; run the required validation row before completion.
- A failed validation is a reason to diagnose and retry, not a reason to silently weaken the check.
- Do not rewrite unrelated code merely because it is nearby.
- Do not merge, release, publish, force-push, delete user data, rotate secrets, or make irreversible external changes unless explicitly requested.
- Stop self-iteration and switch to a human checkpoint when a product decision, visual judgement, destructive migration, platform-only verification, credential, or unclear scope boundary is required.
- Record what remains unverified instead of inventing evidence.

### Human-check mode

Use this mode when the user explicitly asks for manual review/checking, or when a human judgement is part of the Definition of Done.

Typical triggers:

- UI appearance, animation feel, interaction parity, accessibility feel, or screenshot/recording comparison.
- Choosing between Desktop adaptation and exact Android parity when product intent is not already documented.
- Destructive or non-trivial data migration.
- Installer/package behavior that requires a native OS not available to the agent.
- New dependency/update provenance, signing, release, or security-sensitive behavior.
- Any change whose correctness depends on a real account, credential, hardware device, or external service state.

Before the checkpoint, finish all safe automated work that can reduce the review burden. Then produce a **Human Review Packet** containing:

- outcome and affected path;
- exact files/areas changed;
- automated checks and results;
- exact manual steps;
- expected result for each step;
- screenshots/recordings/logs worth capturing;
- unresolved risk and rollback path.

Do not mark a human-only item verified until the human evidence exists.

Detailed mode behavior lives in [`docs/agent-workflow.md`](docs/agent-workflow.md).

## Before Editing

- Inspect `git status` and preserve unrelated or user-authored changes.
- State the requested outcome and affected modules before widening scope.
- Trace the complete path from UI to persisted settings, plan generation, platform adapter, execution, and user feedback.
- Compare Android behavior when parity is requested, but classify the result as exact parity, Desktop adaptation, intentionally deferred, or unsupported.
- Identify affected strings, storage schema, dependency source, packaging, and platform workflows before implementation.
- Do not turn an audit-only or review-only request into code changes without approval.
- Check [`docs/current-progress.md`](docs/current-progress.md) before reviving an old TODO: stale audit items must be revalidated against current code first.

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

- Update [`docs/current-progress.md`](docs/current-progress.md) whenever the current resume point, priority, or verification debt changes.
- Add durable architecture/product facts to [`docs/project-memory.md`](docs/project-memory.md), not to the rolling progress page.
- Add reusable engineering rules here or in [`docs/development-guidelines.md`](docs/development-guidelines.md).
- Keep long historical investigations and dated evidence in the audit/history documents; do not make new agents read them first.
- A completed change must include implementation, failure handling, targeted tests, affected localization, platform verification, and documentation synchronization. Explicitly list anything that remains unverified.
