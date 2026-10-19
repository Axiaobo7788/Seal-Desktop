# RC UI Comparison And Human Review

Date: 2026-10-08. Scope: requested UI phases A–D; optional History selection is deferred. Status: implementation present, final validation evidence recorded below, visual/native acceptance pending.

## Baseline And Safety

- GitHub `origin/main` fetched at `eadc8b632e9b9521b144506d93a55070a2f80adf`; created `feat/rc-ui-polish` from that exact baseline.
- Local `main` fast-forward failed because its unique `d6f8231e` commit was preserved. No reset, merge, rebase, push or release.
- GitLab fetch failed for missing credentials. The earlier stash remains reachable.
- Local RC commits through `2a282ee1` are preserved on `chore/agent-governance-refresh`, not silently imported into this branch. Only the scoped clip pure-logic foundation was reused; no unrelated playlist/history/dependency/executor work was copied.

## Comparison And Changed UI

| Surface | Android reference | Desktop outcome / classification |
| --- | --- | --- |
| Clip | `FormatPage`: RangeSlider, duration/range, exact editing, mutually exclusive chapters | Desktop adaptation: inline slider and precise text fields; seconds/mm:ss/hh:mm:ss drafts, last-valid range, validation/reset; keyboard second-wise arrows/Home/End; unavailable for unknown/nonfinite/sub-second/live duration |
| Format lists | Suggested/audio/video categories and selection | Desktop adaptation: search for >12 formats or Show all; original category indexes, hidden selections retained, clear action, no-results feedback |
| Additional Settings | `DownloadDialogV2`: permanent mobile chip row | Desktop adaptation: existing fold + enabled-only single-line ellipsis summary; identical chip preferences, stable header height |
| Page states | Loading/format data/selection and Android navigation | Correctness repair: no blank formats page; retry/back for empty/error, blank-URL state, immediate loading, stale-result rejection and bounded scrollable diagnostic layout |
| History | `VideoListPage`: selection and mobile bottom actions | Deferred P2: a proper Desktop toolbar/filter-aware model is not smuggled into this four-surface diff |

Implementation areas:

- [Clip policy, parser and draft state](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/clip/ClipRangePolicy.kt) and [Clip editor](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/clip/ClipEditor.kt).
- [Format page, lifecycle/state rendering and selection](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatPage.kt) and [format matching](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/ui/page/downloadv2/configure/FormatSearchMatcher.kt).
- [Options sheet](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/DesktopDownloadSettingsSheet.kt) and [enabled summary policy](../../desktop/src/main/kotlin/com/junkfood/seal/desktop/download/configure/AdditionalSettingsSummary.kt).
- Product strings: default, Simplified Chinese and Traditional Chinese Android XML; generated Compose mirrors synchronized. Other locales deliberately fall back for new keys. No settings/storage schema, dependency policy or execution backend changed.
- Same-version Compose Desktop UI test dependency added, without a toolchain upgrade.

## Automated Validation

On Linux x64 / Java 21, the combined required baseline passed:

```bash
./gradlew :shared:syncAndroidStringsToComposeResources :desktop:compileKotlin :desktop:test :shared:allTests --stacktrace
```

After the keyboard fix and RTL/short-range regression were added, the final run also forced fresh execution of all three Shared test targets without rebuilding every dependency:

```bash
./gradlew :shared:syncAndroidStringsToComposeResources :desktop:compileKotlin :desktop:test \
  :shared:desktopTest --rerun :shared:testDebugUnitTest --rerun \
  :shared:testReleaseUnitTest --rerun :shared:allTests --stacktrace
```

Result: **BUILD SUCCESSFUL**, Desktop **162 tests**, Shared Desktop **20**, Android Debug **20**, Android Release **20**; zero failures/errors/skips in every target. The Shared counts are the same common tests run on three targets, not 60 distinct cases. `RcUiInteractionTest` contributes 11 actual Compose interaction/state tests; `RcUiLayoutTest` contributes one test iterating the 24 scenes described below. Focused interaction tests also passed after each coupled fix.

Default/zh-CN/zh-TW new keys and `%1$s` placeholders match their generated Compose mirrors. Changed documentation relative links resolve; `git diff --check` passed. Existing AGP/Kotlin compatibility and Gradle 9 deprecation warnings remain visible and were not hidden or addressed with a prohibited toolchain upgrade. No native package, workflow, full Android app compile or real-account/media acceptance is implied.

Tests added: `ClipTimestampParserTest`, `ClipRangePolicyTest`, `FormatSearchMatcherTest`, `AdditionalSettingsSummaryTest`, `RcUiInteractionTest`, `RcUiLayoutTest`. Existing `DesktopDownloadRetryPreferencesTest` locks frozen ranges against current runtime preference refresh.

Coverage includes timestamp overflow/partial input, saved range/reset/mode switching, existing SelectionMerge -> DownloadPlan `--download-sections *42-497`, filtered original indexes, real Compose mouse/keyboard input, request retries/cancellation, empty/blank/thrown error states, long diagnostics, collapsed summary updates and animation geometry.

The layout test renders 24 **page-scene** combinations: all eight requested logical widths, each at Density 1/1.25/1.5. Light/dark and en/zh-Hans/zh-Hant/de/unknown-locale fallback are rotated across widths, **not their complete Cartesian product**. It checks field bounds and selection semantics. It does not render the full navigation shell or emulate native OS DPI. Two deterministic fixtures are generated at `desktop/build/reports/rc-ui/format-clip-640.png` and `format-clip-1300.png`; they use the Material test palette, not an approved Seal theme screenshot baseline.

These are Compose render/state/input tests, not screenshot-golden approval. A human still needs to judge smoothness, contrast, density and platform accessibility.

## Negative Evidence And Iteration

- First focused run: 19 tests, one test-selector failure because a typed search query and its matching card both satisfied the selector. Fixed the selector to require selection semantics, without weakening selection assertions.
- Expanded full Desktop run: 157 tests, one **real UI failure**: enabling clip hid both edit modes in the menu, preventing switching/disabling. Both available actions now remain visible with selected indicators and toggle through the exclusive mode policy; integrated UI regression passed afterward.
- A later drag regression initially injected a press without placing the pointer on the thumb. Pointer placement alone still failed with a single movement sample: the first movement was consumed by drag slop. Two continuous movements now exercise actual dragging and the range/text synchronization assertion passes; the assertion was retained, not weakened.
- Adding an actual direction-key test after the first full pass exposed a **real keyboard gap**: thumb focus worked but an arrow did not change the range. The [Compose 1.7 RangeSlider implementation](https://raw.githubusercontent.com/JetBrains/compose-multiplatform-core/v1.7.0/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Slider.kt) exposes thumb focus/semantics without key adjustment. The editor now handles focused-thumb arrows/Home/End, one-second steps, RTL horizontal direction and non-crossing boundaries locally; this does not intercept text-field navigation or upgrade the library.
- Original empty-format return, null-message failure ambiguity and unlabeled collapsed FAB were removed. Long-error and fold-height checks were added after adjacent-state inspection.

## Human Review Packet

### Changed UI

The clip editor, format search/selection and state feedback, and collapsed Additional Settings summary are the changed production surfaces listed above. History selection is not included.

### Android Reference Behavior

Android `downloadv2/configure/FormatPage.kt` is the reference for clip range/duration, split mutual exclusion, categories and selection. `DownloadDialogV2.kt` is the reference for the existing chip preference meanings; `DownloadPageV2.kt` and `VideoListPage.kt` were compared without rewriting Queue or importing mobile History actions.

### Intentional Desktop Differences

Inline precise fields, keyboard slider adjustment, category-preserving conditional search and folding/summary remain Desktop adaptations. The final section lists explicitly excluded mobile mechanisms so they are not reopened as parity defects.

### Automated Validation

See the commands/results above. Compose scenes check production rendering/state/input, not native OS DPI, system themes, full-shell navigation, real accounts or media accuracy.

### Manual Test Matrix

| Dimension | Required evidence |
| --- | --- |
| OS | Linux x64, Windows x64, macOS x64 and arm64; report each independently |
| Window widths | 640, 720, 800, 1024, 1199, 1200, 1300, 1600; record actual logical content width as well as screenshot resolution |
| Scale | 100%, 125%, 150% native OS scale; distinguish scale from window logical width |
| Theme | Light, Dark, Follow System; use real Seal palette |
| Locale | English, zh-CN, zh-TW, a locale falling back for new keys, and a long-string language such as German/French |
| Input | Mouse, Tab/Shift+Tab, arrows/precise text, Enter/Space, Esc, wheel/trackpad and scrollbar drag |

### Expected Result

1. Enable video clipping in Settings -> Format, then open Custom Format for a known-duration non-live video. Enable Clip from the preview menu. Expect full initial range, valid start/end inputs and reset; selecting chapters instead must remove clipping, selecting Clip again must remove chapters. The current menu action must also be able to turn its mode off.
2. Drag both handles, focus each with Tab and use arrows/Home/End, enter `0:42` and `8:17`, then temporarily type `1:` or an out-of-duration/reversed range. Expect synchronized fields for slider changes, one-second keyboard adjustments without crossing endpoints, last-valid slider position for invalid drafts, visible validation and no invalid download submission. Correct it and reset; then check a 5–10 second video and an unknown-duration/live video with no usable clip action.
3. Search a long list by ID, `1080`, `1080p60`, `av1`, `vp9`, `opus`, `m4a`, `webm` or bitrate. Select a result, hide it with another query, clear search and submit. Expect category/format identity unchanged, selection retained and a clear button only when text exists. Empty results are not an empty-format error.
4. Fold/unfold Additional Settings repeatedly, toggle each existing chip and resize narrowly. Expect only enabled labels in the collapsed summary, at most one line, unchanged preferences and no header-size jump during collapse.
5. Cause a metadata error, Retry and Back. Expect the same URL/type/preferences and return to Options; error/empty pages must have actionable controls. Check long diagnostics, and cancel/back during loading; no stale first-result page may overwrite a later URL. Real child-process termination requires separate backend/native evidence.
6. Tab through clip/search/menu/cards/FAB, use Enter/Space, and Esc out of menu/rename/subtitle dialogs. Expect readable labels, visible focus, correct activation, no trapped focus or unintended mode changes. Wheel/scrollbar must still reach all formats and the final cards.
7. Resize across 720/1200 navigation boundaries without rewriting navigation, revisit Queue/Settings/History, then switch locale/theme. Expect the existing shell and navigation state to remain coherent; capture any clipping, overflow, jitter or focus loss.
8. Submit a valid `0:42`–`8:17` clip, inspect the command and downloaded artifact, then Retry after changing current clip defaults. Expect `--download-sections *42-497`, no simultaneous chapters and the original range retained. Existing yt-dlp/FFmpeg experimental cut-accuracy limitations are unchanged.

### Screenshots / Recordings To Capture

Capture compact/wide clip and search states, filtered/selected/empty/error states, long-locale summary, focus and selected contrast. Record slider/text editing, invalid draft recovery, rapid fold/unfold, resize across breakpoints and dialog Esc. Include OS/arch, app commit, scale, logical size, theme/locale, input method and relevant CLI logs. Never capture cookie values, browser profiles or private media URLs.

### Remaining Risk And Rollback

No deterministic failure remains in the scoped A–D automated checks after final diff review. Native shell/DPI/hover/accessibility/animation feel and real-media/real-browser evidence are pending. No native package or account acceptance was performed. The baseline's blocking metadata process implementation is unchanged: UI coroutine/stale-result checks do **not** prove a real child is killed or timeout-safe. That is remaining machine-actionable backend work **outside this UI boundary**: reconcile the preserved local RC implementation separately before release; do not redeclare it fixed by these UI tests. History selection remains deferred. There are no new data migrations; rollback is a focused source/UI revert, not deleting state or resetting the repository.

## Intentionally NOT Copied From Android

- Mobile permanent chip row: Desktop keeps folding and a summary, avoiding horizontal overflow and excessive configuration-page density.
- Mobile clip exact-input dialog: Desktop keeps precise editable fields inline next to the slider for keyboard/mouse work.
- Mobile BottomAppBar/swipe history interactions: future Desktop selection needs a toolbar and Desktop shortcuts, not a mechanical port.
- Mobile navigation: preserve Modal Drawer <720dp, Rail 720–1199dp and Permanent Drawer >=1200dp. Settings retains depth-aware transitions/state; no split-pane rewrite.
- Android WebView/Cookie Profiles: Desktop system-browser/profile/cache/domain contract is unchanged.
- Queue reimplementation and bulk spring/shared-axis animation copying: keep shared queue contracts and existing local short transitions; add only the requested selection check and fold geometry protection.

Completion mode: HUMAN-CHECK
