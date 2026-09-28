# Agent Workflow Modes

> Purpose: define how a coding agent iterates autonomously while preserving explicit human judgement where automation is insufficient.

## 1. Mode Selection

Two modes are available:

- **Self-iteration** — default for routine implementation, refactoring, tests, documentation and reproducible build fixes.
- **Human-check** — used when correctness depends on judgement, native/manual evidence, credentials, destructive effects or a product choice.

A task can begin in self-iteration mode and transition to human-check mode.

## 2. Self-Iteration Mode

### Loop

1. Inspect current code, tests, docs and local state.
2. Write/confirm the change contract.
3. Make the smallest coherent change.
4. Run focused validation.
5. If validation fails, diagnose the failure.
6. Repair the implementation or test if the expected behavior is still correct.
7. Repeat until focused checks pass or a stop condition is reached.
8. Run the required broader validation row.
9. Synchronize current progress and durable docs.
10. Report remaining unverified items.

### What the agent may decide itself

- implementation details inside documented architecture boundaries;
- local refactoring needed to complete the requested behavior;
- adding focused tests;
- retrying a failed build/test after diagnosing the cause;
- updating stale current-progress documentation;
- removing temporary debug code created by the same task.

### Stop conditions

Switch to a human checkpoint when any of these occurs:

- two reasonable product behaviors exist and the repository does not define which one is intended;
- visual/animation quality cannot be established by automated tests;
- the next step is destructive or difficult to roll back;
- a schema migration can discard/transform user data;
- required verification needs a native OS, hardware, account, credential, signing key or external service unavailable to the agent;
- fixing the failure requires widening scope beyond the change contract;
- a test conflicts with documented product behavior and it is unclear which is stale;
- release/publish/merge is the next external action and was not explicitly requested.

Do not solve a stop condition by pretending the evidence exists.

## 3. Human-Check Mode

Human-check mode is not "stop doing work immediately".

The agent should first complete all safe preparation:

- compile and focused tests;
- deterministic screenshots or artifacts when possible;
- diff cleanup;
- migration dry-run or backup plan;
- exact reproduction steps;
- expected/failed states.

Then provide a Human Review Packet.

### Human Review Packet template

```md
## Human Review Packet

### Outcome
What user-visible result should now exist?

### Changed areas
- files/modules:
- behavior changed:

### Automated evidence
- command:
- result:

### Manual checks
1. Step:
   Expected:
2. Step:
   Expected:

### Capture
- screenshots/recordings/logs to keep:

### Remaining risk
- unverified platform/state:
- rollback:
```

### After human feedback

- If accepted, record the evidence and continue to completion.
- If rejected, treat the feedback as a new observed failure and return to self-iteration.
- If the human changes the product decision, update the relevant durable documentation before or with the implementation.

## 4. Review Gates For Seal-Desktop

### UI / animation gate

Use human-check mode for claims such as:

- "matches Android";
- "animation is smooth";
- "spacing/shape feels correct";
- "hover/focus behavior is right";
- "compact/wide layout is acceptable".

Automated tests may cover state; they do not replace visual review.

### Packaging gate

A local Linux pass cannot approve Windows/macOS output.

For each affected native target, capture:

- workflow/run;
- architecture;
- package type;
- install/launch result;
- SQLite/runtime smoke result when relevant.

### Persistence gate

For storage/schema changes:

- show old-data read behavior;
- show migration or compatibility path;
- show recovery behavior;
- identify irreversible steps.

### Update/dependency gate

For update mechanisms and downloaded binaries:

- source/provenance must be explicit;
- ownership (`system` vs `selfhost`) must stay correct;
- platform package/update behavior must not be copied blindly from Android.

### Privacy gate

Anything involving private mode, cookies, history, task recovery or sensitive URLs requires explicit persistence-path review.

## 5. Interaction With Documentation

- `AGENTS.md` contains the short mandatory rules.
- `project-memory.md` stores durable decisions.
- `current-progress.md` stores the current resume point and open verification debt.
- detailed historical evidence may remain in dated audit documents.

A successful self-iteration should not append a diary entry to project memory.
A human decision that changes product semantics usually should.
