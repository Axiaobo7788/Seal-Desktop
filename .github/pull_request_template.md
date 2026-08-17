## Outcome

Describe the user-visible result, not only the files or refactor performed.

## Scope

- Affected modules:
- Android reference behavior:
- Desktop classification: exact parity / platform adaptation / deferred / unsupported
- Explicit non-goals:

## Completion Checklist

- [ ] The complete UI -> state -> persistence -> plan -> adapter -> side-effect -> feedback path was checked, or marked not applicable.
- [ ] Unsupported or deferred behavior is hidden, disabled, or clearly explained.
- [ ] User-visible text uses resources; default fallback and locale qualifier mappings were checked.
- [ ] Persistent data remains backward compatible, or a migration is included.
- [ ] Focused automated tests cover the changed behavior.
- [ ] Affected Gradle compile/test tasks pass.
- [ ] Workflow or packaging changes were validated on each affected native runner.
- [ ] UI changes were checked for state, window size, theme, locale, input, scrolling, and animation, or gaps are listed below.
- [ ] The current audit checkbox and validation evidence were updated when applicable.
- [ ] The diff contains no unrelated reversions, generated noise, secrets, local paths, or temporary debug behavior.

## Verification

List exact commands, target operating systems, architectures, package types, and manual scenarios.

## Remaining Risk

List every unverified platform, locale, visual state, migration path, or follow-up. Write `None` only when all applicable checks are complete.
