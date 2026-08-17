# Seal Desktop Development Guidelines

> Status: active engineering specification
>
> Updated: 2026-08-12
>
> Scope: Android, shared KMP, Desktop JVM, CI, packaging, resources, and project documentation

This document defines how changes are designed and accepted. It is not a progress tracker. Mandatory short rules are mirrored in the repository root [`AGENTS.md`](../AGENTS.md) so that a new contributor or coding agent sees them before touching code.

## 1. Document Authority

| Document | Role | May contain |
| --- | --- | --- |
| `AGENTS.md` | Mandatory execution contract | Hard boundaries, minimum validation, completion rules |
| `docs/development-guidelines.md` | Detailed engineering specification | Architecture, parity, i18n, testing, task templates |
| `docs/project-map.md` | Navigation | Module locations, key flows, stable entry points |
| `docs/desktop-project-audit-2026-06-15.md` | Current action list | Defects, priorities, checkboxes, dated verification evidence |
| `docs/android-desktop-progress-tracker.md` | Historical baseline | Old migration decisions and snapshots |
| `docs/android-desktop-backend-defects.md` | Compatibility link | Redirect to the current audit only |

Rules must not be invented inside a dated progress section. If an audit reveals a reusable boundary, add it here and keep only the defect/status entry in the audit.

## 2. Change Contract

Before implementation, establish this contract:

```md
### Change Contract
- User-visible outcome:
- Affected modules:
- Android reference behavior:
- Desktop classification: exact parity / platform adaptation / deferred / unsupported
- Data and settings flow:
- New or changed strings:
- Platforms and packages affected:
- Automated verification:
- Manual verification:
- Explicit non-goals:
```

The contract can remain in an issue, task description, or the implementation discussion. It does not need a permanent document for a small change, but every field must be considered.

### Scope rules

1. Fix the requested behavior end to end before opportunistic cleanup.
2. Keep unrelated refactors separate when they change architecture, schema, package output, or user-visible semantics.
3. Preserve unrelated dirty-worktree changes and generated files that belong to another task.
4. Do not call a UI placeholder, disabled control, or test-only adapter a completed feature.
5. If the current implementation contradicts this specification, record the mismatch in the audit before or with the fix.

## 3. Module Boundaries

| Concern | Owner | Allowed dependencies | Forbidden leakage |
| --- | --- | --- | --- |
| Android product integration | `app/` | AndroidX, Room, MMKV, services, WebView, youtubedl-android | Desktop paths and JVM process assumptions |
| Cross-platform domain | `shared/commonMain` | Kotlin, coroutines, serialization, platform-neutral Compose APIs | AndroidX APIs, Room, MMKV, `java.io`, OS paths, executable names |
| Platform implementations of shared contracts | `shared/androidMain`, `shared/desktopMain` | Target APIs required by `expect`/`actual` | Product orchestration that belongs in `app/` or `desktop/` |
| Desktop product integration | `desktop/` | JVM process APIs, SQLite, desktop file pickers, OS/package-manager paths | Android services, Android resource IDs, `.so` assumptions |
| Theme and color support | `color/` | Theme-specific code | Download, storage, or packaging logic |

### Shared-code admission test

Code belongs in `shared/commonMain` only when all answers are yes:

1. Does it describe product meaning rather than one platform's mechanism?
2. Can it be tested without Android, a desktop OS, a filesystem layout, or an executable?
3. Can Android and Desktop consume it without ignored fields or fake implementations?
4. Are platform-specific names supplied by adapters or semantic parameters?

If any answer is no, keep the code in a platform module or introduce a narrow contract first.

## 4. Android And Desktop Parity

Android is a behavioral reference, not an automatic source to copy. Every compared feature must receive one classification:

| Classification | Meaning | Required record |
| --- | --- | --- |
| Exact parity | User intent and result should match Android | Scenario, expected state, test or manual evidence |
| Desktop adaptation | Same intent, desktop-native mechanism | Why the Android mechanism is inappropriate and the Desktop replacement |
| Deferred | Required but not implemented in this iteration | Disabled/hidden UX and an audit item |
| Unsupported | Intentionally excluded from Desktop | Product reason and removal of misleading UI |

### End-to-end trace

For any setting or action, verify this chain:

`UI input -> state update -> persistence -> plan/domain mapping -> platform adapter -> process/storage side effect -> success/error feedback -> restart behavior`

A feature is incomplete if any required link is absent. This rule specifically applies to toggles, custom commands, download directories, cookies, dependency source selection, format/subtitle choices, archive behavior, and update actions.

### UI acceptance

Parity review must cover:

- Initial, loading, empty, success, error, disabled, and restored states.
- Navigation destination and back-stack preservation after language/theme changes.
- Mouse, keyboard, focus, hover, scroll wheel, scrollbar drag, and touch behavior where applicable.
- Compact and wide windows, high DPI, light/dark theme, long translations, and RTL text.
- Enter/exit transitions, content size animation, interruption, and rapid repeated input.
- Text labels, helper text, accessibility descriptions, and destructive-action confirmation.

Screenshots establish appearance only. Animation and interaction parity require a recording or hands-on check.

## 5. Internationalization Contract

### 5.1 Sources and generated mirrors

The authoritative source is:

`app/src/main/res/values*/strings.xml`

The resource chain is:

`app values*/strings.xml -> :shared:syncAndroidStringsToComposeResources -> shared/src/commonMain/composeResources -> Compose Res`

Desktop also copies the same Android XML into its build directory for `AndroidStrings`:

`app values*/strings.xml -> :desktop:syncAndroidStringsXml -> desktop build resources -> AndroidStrings`

Therefore:

1. Add and edit product strings under `app/src/main/res` first.
2. Do not hand-edit only the shared mirror.
3. Run the shared sync task and review its diff before committing.
4. Keep the 62 current `values*` directories mirrored between `app` and `shared`; directory equality does not imply every key is translated.

### 5.2 Adding or changing a string

1. Search for an existing semantically correct key.
2. Add a concise English fallback in `values/strings.xml` when a new key is necessary.
3. Add verified translations only to locale files whose translation is known. Do not duplicate English across all locale files to simulate completeness.
4. Preserve placeholder count, order, types, XML escaping, and newline intent in every override.
5. Use `stringResource(Res.string.<key>)` in Compose code.
6. Pass already-resolved text into non-composable platform APIs where practical; otherwise use `AndroidStrings` with the same locale mapping.
7. Include notifications, tray messages, logs shown in UI, process setup output, picker titles, validation messages, and content descriptions in the scan.

Fallback to `values/strings.xml` is acceptable and explicit. A hard-coded English or Chinese literal in product UI is not fallback.

### 5.3 Locale identity and qualifiers

Persisted language tags and resource qualifiers are different concepts:

| Persisted tag | Resource qualifier | Notes |
| --- | --- | --- |
| `zh-Hans` | `values-zh-rCN` | Also infer Hans from CN and SG |
| `zh-Hant` | `values-zh-rTW` | Also infer Hant from TW, HK, and MO |
| `he` | `values-iw` | Java may canonicalize legacy `iw` to `he` |
| `id` | `values-in` | Java may canonicalize legacy `in` to `id` |
| `null` | Original system environment | Desktop Follow System, not a fixed copy of the current tag |

The same mapping must be used by Compose Resources, `AndroidStrings`, language suggestions, and display-language selection. Do not derive Suggested from an app-overridden `Locale.getDefault()`.

Resource directory coverage and language-picker coverage are separate. Adding a selectable locale requires all of these checks:

- Android `LanguageSettings` option and label.
- Desktop `DesktopLocaleOptions` persisted tag and resource override.
- Resource directory and default fallback.
- Script/region matching for Suggested.
- Follow System behavior.
- Restart persistence and immediate UI refresh without navigation reset.

### 5.4 Resource verification

Run:

```bash
./gradlew :shared:syncAndroidStringsToComposeResources :desktop:compileKotlin
```

Then verify:

```bash
diff \
  <(find app/src/main/res -maxdepth 1 -type d -name 'values*' -printf '%f\n' | sort) \
  <(find shared/src/commonMain/composeResources -maxdepth 1 -type d -name 'values*' -printf '%f\n' | sort)
```

For a changed formatted key, also compare placeholders in every locale override. Compilation catches missing generated accessors, but it does not prove translation quality, RTL layout, script selection, or runtime fallback.

## 6. Desktop Dependency Policy

| Source | Ownership | Detection | Update behavior |
| --- | --- | --- | --- |
| `system` | OS package manager or user PATH | PATH plus known package-manager locations | Re-detect or instruct the package manager; never overwrite |
| `selfhost` | Seal | App-private auxiliary directory | In-app downloader may install or update |
| packaged app-private | Seal package | Application resource/code-adjacent roots | Read-only at runtime; replaced by app package update |
| `auto` | Resolver | App-private first, then system | Download only unresolved required components |

Platform-specific app-private locations are owned by `DesktopDependencyPaths`; generic `~/.local/bin`, Homebrew prefixes, WinGet links, Scoop shims, and Chocolatey bins are system locations. Finding a usable system tool must not trigger a duplicate download into a Seal-owned location.

`yt-dlp` and `ffmpeg` form the required complete environment. `ffprobe` is distributed with the selected ffmpeg package when needed. `aria2c` is optional and must not make the base environment incomplete.

Changes to this policy require resolver tests for Linux, Windows, macOS Intel, and macOS arm64, plus the dependency smoke workflow. CI may test command construction without mutating a hosted runner through `winget`, Homebrew, or privileged Linux package installation.

## 7. Persistence And Migration

- New persistent fields require defaults, serialization/storage changes, backward-compatible reads, and restart tests.
- Compatibility fields must be marked migration-only and must not gain new callers.
- JSON, dual, and SQLite backends must retain equivalent user-visible behavior while more than one is supported.
- Native installers must not pre-create `seal.db` in an installation directory. SQLite is initialized on first run in the user-writable state directory.
- Corrupt data handling must preserve recoverable user data and expose a diagnosable failure.
- Private mode must not leave URLs or task payloads in history, queue recovery, logs, or temporary exports beyond the documented session boundary.

Release shrinking and platform-native dependency trimming require a packaged runtime check. Launch the app with `SEAL_DESKTOP_STORAGE_BACKEND=sqlite` and an isolated `SEAL_DESKTOP_STORAGE_STATE_DIR`, then require both a live process and a non-empty `seal/seal.db`. A startup-only smoke is insufficient because ProGuard can remove JDBC `ServiceLoader` or JNI callback classes while the window still opens.

## 8. Validation Matrix

| Change area | Minimum automated validation | Additional evidence |
| --- | --- | --- |
| Documentation | `git diff --check`; changed-link review | None unless claims changed |
| Desktop Kotlin | `:desktop:compileKotlin`; focused tests | Manual UI check for visible changes |
| Shared domain/UI | `:shared:allTests :desktop:compileKotlin`; focused tests | Android compile/test when Android behavior is affected |
| Resources/language | Sync task; Desktop compile; locale/placeholder checks | Switch locale and verify fallback/navigation |
| Storage | Focused tests; affected storage self-check modes | Restart/recovery scenario |
| Dependency resolution | Desktop yt-dlp test package; dependency smoke workflow | Native runner logs for each OS/arch |
| Workflow/scripts | `actionlint`; script syntax check | Real workflow run |
| Native packaging | Relevant package/app-image tasks | Installed or launched artifact on the target OS |
| UI/animation | Compile and state tests where practical | Recording or hands-on matrix; note unavailable platforms |

Use the smallest sufficient command during iteration, then run the full affected row before marking the audit complete. A Linux-only pass never proves Windows or macOS packaging.

## 9. Progress And Evidence

The audit document records state, not rules. Each completed checkbox should include:

```md
- [x] Outcome stated in user terms.
  - Implementation: affected flow and files.
  - Automated: exact command and result.
  - Manual/platform: exact environments checked.
  - Remaining: explicit unverified or deferred cases.
```

Use `[ ]` when implementation, verification, localization, or required platform evidence is still missing. Use dated notes for historical commands so an old pass is not mistaken for current evidence.

## 10. Definition Of Done

A change is complete only when all applicable statements are true:

- The requested user-visible outcome works through the full data and execution path.
- Unsupported or deferred paths do not remain as misleading active UI.
- Android parity or Desktop adaptation is explicitly classified.
- New failure paths produce actionable, localized feedback.
- Persisted data survives restart and older data remains readable when affected.
- Required strings use the resource chain and qualifier mapping.
- Focused automated tests and the affected validation matrix pass.
- Required native platforms are verified, or the missing evidence is disclosed.
- Audit status and durable documentation are synchronized.
- The diff contains no unrelated reversions, generated noise, secrets, machine-local paths, or temporary debugging behavior.
