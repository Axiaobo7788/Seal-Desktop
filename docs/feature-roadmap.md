# Seal-Desktop Feature Roadmap

> Role: product capability status and planned Desktop adaptations.
>
> Refreshed: 2026-09-28
>
> This document answers a question that bug trackers do not answer well: **is a behavior broken, or has the planned Desktop feature never been completed?**
>
> Do not use this file as a release promise or fixed schedule. `current-progress.md` decides what is being worked on now.

## Status Model

Each capability should use one of these classifications:

| Status | Meaning |
| --- | --- |
| **Implemented** | End-to-end product behavior exists and has appropriate verification. |
| **Partial** | UI/model/backend pieces exist, but the product flow is not complete enough to call the feature implemented. |
| **Planned** | Product behavior is agreed, but implementation has not substantially started. |
| **Bug** | An already-implemented behavior violates its defined semantics. |
| **Decision needed** | More than one reasonable product behavior exists and a human product decision is required. |
| **Deferred / Unsupported** | Intentionally not implemented for now, or intentionally excluded on Desktop. |

A GitHub issue title or reporter wording does not decide this classification. Re-check product intent, current code, and this roadmap first.

## Desktop Platform Principle

Android is the behavioral reference, not the mechanism reference.

When Android uses a mobile-only mechanism such as WebView, SAF, foreground services, Android notification APIs, MediaStore, or app-private storage semantics, Desktop should preserve the user intent with a desktop-native mechanism rather than copying the Android implementation.

## Current Product Roadmap

### Cookies — **Partial / Desktop adaptation**

**Product decision**

Desktop cookies must use the **system browser / installed browser session** as the primary authentication source. Do **not** port Android's embedded `WebViewPage` login flow to Desktop.

Android may continue to use Cookie Profiles + embedded WebView. Desktop is intentionally different.

**Current code**

- `CookiesSettingsPage.kt` already exposes a browser-based flow.
- It invokes yt-dlp with `--cookies-from-browser <browser>` and writes a Netscape cookies file.
- Manual Netscape-cookie import/export exists as a fallback.
- `BrowserCookieExtractor.kt` currently only defines supported browser names.
- The generated cookies file is consumed by the final download plan when `preferences.cookies` is enabled.
- Metadata fetch currently does not consume the resolved cookies context.
- Retry currently reuses the original request preference snapshot.

Therefore the feature has a visible skeleton but is **not end-to-end complete**.

**Target Desktop flow**

`choose/open system browser source -> user is authenticated in that browser -> resolve/extract cookies -> optional cached Netscape file -> metadata fetch -> format/configuration flow -> final download/retry all use the same cookie context -> visible success/error feedback`

Acceptance points:

- no embedded Desktop WebView login implementation;
- system-browser extraction is the normal path;
- manual cookies-file import remains a fallback, not the primary UX;
- browser/source selection and extraction failures are visible to the user;
- metadata lookup and final download use the same resolved cookies state;
- retry/resume must not silently use stale cookie preferences;
- clearing cookies must have clear semantics about cached files versus the browser's own cookies;
- browser/profile differences on Windows, macOS and Linux must be treated as platform adapter behavior.

### Application update — **Partial / Decision needed**

Current Desktop settings expose auto-update/channel/check controls, but the check button remains a TODO.

Android APK self-update must not be copied directly.

Likely Desktop-native stages:

1. release check + user notification;
2. open/download the correct native package;
3. only later consider platform-specific self-update where safe.

Until behavior is implemented, active controls must not pretend automatic update works.

### Download archive management — **Partial**

The execution layer can use `download-archive.txt`, but Desktop lacks the planned management experience.

Planned user capabilities:

- show archive location and count;
- open/view entries;
- edit or clear with confirmation;
- make "already archived" a readable user outcome instead of a mysterious skipped/completed task.

### Playlist item selection — **Planned**

Current Desktop Playlist behavior effectively means "download the whole playlist".

Planned Desktop behavior should preserve Android's user intent:

- fetch playlist entries;
- let the user choose entries;
- map selected indexes into shared playlist-selection semantics / `--playlist-items`;
- create understandable queue items.

The UI may be desktop-native rather than a literal copy of the Android page.

### Multi-URL input and Saved URLs — **Planned**

Desktop currently has a simplified single-line input flow.

Planned capability includes:

- multi-link input/clipboard recognition;
- confirmation of parsed links;
- saved URLs;
- add/remove management with desktop-native list actions.

Swipe gestures from Android are not required on Desktop; hover/context-menu/keyboard behavior is acceptable when semantically equivalent.

### Download history bulk workflow — **Partial**

Desktop already has history, search, import and export.

Still-planned parity/product work includes:

- multi-select;
- bulk delete;
- export selected entries;
- clearer detail view;
- appropriate entry/search transitions.

Desktop may use a side panel, dialog or context actions instead of Android's drawer/bottom bar where more natural.

### Custom format selection — **Partial**

The page exists but is not finished parity.

Known planned gaps include:

- pass download type into the actual format UI and filter audio/video choices correctly;
- regex-like subtitle preference matching;
- better clip-range editing;
- remaining visual/interaction parity and localization.

Visual parity items require human-check mode.

### Private download directory — **Decision needed**

`privateDirectory` exists in shared/Android semantics but Desktop does not have a defined product behavior.

Do not treat this field as an unfinished checkbox to blindly port.

A separate decision is required on whether Desktop should:

- have an application-private/hidden download location with desktop semantics; or
- intentionally omit the feature and prevent stale shared fields from surfacing in Desktop UI.

This is distinct from `privateMode`, which controls persistence/history behavior.

### Desktop UI parity component map — **Planned maintenance/product work**

Desktop currently mixes shared components, Desktop-native components and local Android-like recreations.

A future parity map should identify:

- must-share behavior;
- exact visual parity candidates;
- intentional Desktop adaptations;
- Android-only mechanics that must not be copied.

This is especially useful for dialogs, search bars, bottom sheets/panels, settings navigation, queue actions and animation tokens.

## GitHub Issue #4 Triage

Issue: [#4 Cookies not applied to metadata fetch or retry, plus SponsorBlock crash on empty category](https://github.com/Axiaobo7788/Seal-Desktop/issues/4)

The issue is useful, but its four reports are not all the same class.

### 1. Metadata fetch never sends cookies

**Classification: Partial planned feature / implementation gap**

The reporter found a real failure path, but the deeper problem is that Desktop cookies are not yet a completed end-to-end feature.

Do not implement this as an isolated `--cookies` patch and call Cookies complete. Metadata, extraction/source selection, final download, retry and user feedback must share one cookies contract.

### 2. Retry reuses stale preferences

**Classification: Bug**

This is broader than cookies. Retry can ignore newly changed debug/proxy/cookie/etc. preferences because it reuses the queued request snapshot.

Fix semantics must explicitly decide which settings should be refreshed on retry and which request-specific choices must remain frozen.

### 3. SponsorBlock empty category emits an invalid argument

**Classification: Bug**

SponsorBlock is already an implemented user-facing feature. Enabling it with the default/empty category state must not generate an invalid yt-dlp command.

The fix should be locked with a shared plan/CLI test.

### 4. Fresh local `createDistributable` can fall back to a broken PATH yt-dlp

**Classification: Developer tooling / packaging gap**

Release workflows and local contributor builds do not currently have the same dependency-population guarantees.

This should be solved as dependency bootstrap/provenance work, for example:

- an explicit contributor setup/fetch task;
- sanity-checking a resolved system binary before trusting it;
- clear diagnostics showing source/path/version;
- pinned/provenance-aware release dependency handling.

Do not classify every broken external PATH executable as an application runtime bug.

## How Issues Should Feed This Roadmap

When triaging a report:

1. identify the user's expected outcome;
2. check whether the feature is Implemented, Partial, Planned, Decision needed, or Deferred;
3. if Partial/Planned, update the feature contract here instead of creating a pile of isolated "bugs";
4. split true implementation defects into focused bug tasks;
5. update `current-progress.md` only when the item becomes active work;
6. preserve detailed logs and old investigations in dated audit/history documents.

The goal is to prevent an unfinished port from being mistaken for a mature product suffering dozens of unrelated regressions.
