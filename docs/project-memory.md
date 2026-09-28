# Seal-Desktop Project Memory

> Role: durable project context for humans and coding agents.
>
> Last reviewed: 2026-09-28
>
> Do not put short-lived TODOs, one-off command output, or temporary CI failures here. Those belong in `current-progress.md` or dated audit/history documents.

## 1. Project Identity

Seal-Desktop is a Desktop port and cross-platform evolution of Seal around yt-dlp.

The project is not a literal Android clone. Android is a behavioral reference, while Desktop may use native platform adaptations where Android mechanisms do not make sense.

Core product goal:

`URL/input -> metadata/configuration -> platform-neutral download plan -> platform adapter -> yt-dlp/ffmpeg execution -> queue/history/storage -> user feedback`

A feature is not complete just because its UI exists.

## 2. Architecture Memory

- `app/`: Android product integration, services, Room, MMKV, Android filesystem and youtubedl-android integration.
- `desktop/`: Compose Desktop UI, JVM process execution, desktop settings/storage, file pickers, dependency resolution and native packaging.
- `shared/`: platform-neutral models, download plans, reusable business rules and cross-platform UI contracts.
- `color/`: theme/color support.
- Product strings originate in `app/src/main/res/values*/strings.xml` and are synchronized into Compose resources.
- Desktop currently supports JSON/dual/SQLite storage compatibility; SQLite is the structured target, while compatibility paths must remain migration-safe until intentionally removed.

## 3. Product Decisions That Must Survive Context Loss

### Platform parity

Every Android/Desktop comparison should be classified as one of:

- exact parity;
- Desktop adaptation;
- deferred;
- unsupported.

Do not copy Android-only behavior merely to make a Desktop screen look complete.

### Dependency ownership

- `system`: user/OS-owned PATH or package-manager binary; detect but never overwrite.
- `selfhost`: Seal-owned app-private binary; in-app downloader may update it.
- packaged app-private resources: read-only at runtime and replaced with app updates.
- `auto`: resolve available sources and download only missing required components.
- `yt-dlp + ffmpeg` are required for a complete Desktop download environment.
- `aria2c` remains optional.

### Privacy

`privateMode` is a persistence/history policy, not merely a UI label.

Do not treat `privateDirectory` as equivalent to `privateMode`. Android private-directory semantics do not automatically transfer to Desktop.

### UI parity

Visual similarity is insufficient. Parity review includes state transitions, error/loading states, keyboard/mouse behavior, scrolling, animation feel, responsive layout and localization.

Human evidence is expected for visual/animation claims.

## 4. Known Historical Traps

These have repeatedly caused false-completion or maintenance problems:

- visible settings whose values never reach execution;
- Android-specific binary/path assumptions leaking into Desktop;
- platform adaptations being judged only by screenshots;
- hard-coded user-visible English/Chinese in Desktop notifications, dialogs, picker titles and status text;
- old migration/compatibility fields attracting new callers;
- local Linux packaging success being treated as proof of Windows/macOS behavior;
- stale dated validation evidence being read as a current pass;
- one very large audit document mixing current priorities with historical investigation.

Before acting on an old audit item, re-check current code.

## 5. Packaging And Release Memory

Native packaging is part of runtime correctness, not merely artifact creation.

For release/shrinking work:

- a package that builds but does not launch is a failure;
- SQLite must be exercised after packaging because ProGuard/JNI/ServiceLoader issues may appear only at runtime;
- Windows, Linux and macOS evidence are independent;
- macOS Intel and arm64 are independent packaging targets when both are supported;
- never infer a native runner result from another OS.

## 6. Documentation Model

The project uses four documentation layers:

1. `AGENTS.md`: mandatory short execution contract.
2. `docs/project-memory.md`: durable project facts and decisions.
3. `docs/current-progress.md`: current resume point, priorities and verification debt.
4. dated audit/history documents: detailed evidence, old investigations and migration history.

`docs/development-guidelines.md` contains detailed engineering rules.
`docs/project-map.md` contains navigation and stable code entry points.

This separation is intentional. Avoid growing a single "everything" document again.

## 7. Resume Protocol

After a long pause:

1. read `AGENTS.md`, this file, and `current-progress.md`;
2. inspect the current branch/head and recent commits;
3. revalidate active P0/P1 items against current code before implementing them;
4. compare relevant upstream changes when parity or dependencies may have moved;
5. run the smallest useful compile/test smoke before trusting old status;
6. update `current-progress.md` with the new resume point.

## 8. Updating This Memory

Add something here only when it is likely to remain useful across many tasks.

Good examples:

- module ownership;
- a durable product decision;
- a migration invariant;
- a recurring platform trap;
- a validation rule that future work must remember.

Bad examples:

- "test X passed today";
- "issue Y is next";
- a temporary error log;
- a one-off workaround;
- today's dependency version check.
