# Regression Test Strategy for JDK25 Upgrade

## Scope and architecture focus
`JavaGoldenAxe` is a mostly data-driven game loop with heavy GUI/rendering, but key behavior is controlled by deterministic parsers and state-machine logic:

- **Resource parsing and object construction** in `JavaGoldenAxe\src\infra\Resource.java`
  - Character animation parsing (`getAnimationPlayer`)
  - Animator script parsing (`getAnimators`)
  - Stage script parsing (`loadStage`)
  - Music/text/sound metadata loading (`loadMusics`, `getText`, `getSoundId`)
- **State machine mechanics** in `JavaGoldenAxe\src\infra\StateManager.java`
- **Animation/animator frame progression** in `JavaGoldenAxe\src\infra\Animation.java` and `JavaGoldenAxe\src\infra\Animator.java`
- **Command execution logic** in `JavaGoldenAxe\src\infra\Command.java` (stage-triggered actions)
- **Stage orchestration** in `JavaGoldenAxe\src\scene\Stage.java` (`fixedUpdate`, animator lifecycle)

These are high-value and deterministic enough to guard upgrade regressions without relying on pixel-perfect GUI assertions.

## Practical constraints
- Current repo has **no committed tests** yet (`**/*Test*.java` not present).
- Build system is NetBeans/Ant; `build-impl.xml` exposes standard test targets (`test`, `test-single`, `test-single-method`, `compile-test`, `compile-test-single`).
- GUI-heavy runtime (AWT/Swing/Java2D/audio) makes full end-to-end automation expensive/flaky. Use focused logic tests + scripted smoke checks.

## Phased strategy

## 1) Baseline (pre-upgrade, JDK17-compatible)
Goal: capture current behavior before changing toolchain/runtime.

1. **Create deterministic unit tests first** (priority order):
   - `Resource` parser behavior
   - `StateManager` transition semantics
   - `Animation`/`Animator` progression and completion rules
   - `Command` trigger execution and one-shot behavior
2. **Snapshot expected outputs** as assertions (counts, IDs, flags, state transitions), not screenshots.
3. **Run and store baseline test results** as the reference gate for migration.

Suggested baseline test classes:
- `infra\ResourceParsingTest`
- `infra\StateManagerTest`
- `infra\AnimationProgressionTest`
- `infra\AnimatorProgressionTest`
- `infra\CommandExecutionTest`

## 2) Migration gate (during JDK25 migration)
Goal: block merge on any behavioral drift in core logic.

- CI/local gate should run:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml clean test`
- Required pass criteria:
  - 100% pass on baseline regression suite
  - No parser exceptions on canonical resource files
  - No state transition ordering regressions (`onExit`/`onEnter`/`fixedUpdate`)
- Recommended strictness:
  - Keep assertions exact for deterministic values (frame index boundaries, loop flags, event order).

## 3) Post-upgrade confidence
Goal: increase confidence beyond unit tests with targeted smoke checks.

1. Add small integration checks around scene progression and command-triggered stage effects.
2. Execute manual/assisted smoke pass covering:
   - startup/title flow
   - stage load and first enemy spawns
   - one background transition and one animator cutscene
   - game over / continue prompt path
3. Keep this smoke pass as a release checklist for future Java upgrades.

## Candidate regression test cases (concrete)

## A. Resource parsing (`Resource.java`)
1. **Animation parsing happy path**
   - Load known character resource; assert animation IDs exist, frame counts > 0, key parameters parsed.
2. **Animator parsing happy path**
   - `getAnimators(res)` returns expected animator IDs and actor/keyframe/event presence.
3. **Stage command parsing**
   - `loadStage(stage, stageScene)` adds `Command` actors for `command ...` lines.
4. **Comments/empty lines ignored**
   - Files with `#`/`;` and blanks do not alter parsed counts.
5. **Type conversion stability**
   - Numeric/boolean fields parse exactly (camera/background/transition config).

## B. State machine behavior (`StateManager.java`)
1. **Initial switch enters immediately**
   - First `switchTo` calls `onEnter` on target state.
2. **Deferred transition**
   - Non-initial `switchTo` sets `nextState`; actual swap occurs in `fixedUpdate`.
3. **Exit/enter ordering**
   - On transition, old `onExit` occurs before new `onEnter`.
4. **No-state safety**
   - `fixedUpdate` with no state does not throw.

## C. Animation/animator progression
1. **Animation non-loop completion**
   - `Animation.update()` clamps near last frame and marks finished.
2. **Animation loop wrap**
   - Looping animation wraps to frame `0.0` and remains unfinished.
3. **Animator tween interpolation**
   - `AnimatorFrame.lerp` returns expected interpolated wx/wy/wz/scale/angle.
4. **Animator event ordering**
   - Events trigger in ascending frame order, exactly once per play cycle.
5. **Animator finish semantics**
   - Non-loop animator reaches `finished=true` when keyframes/dialog/events are complete.

## D. Command execution logic (`Command.java`)
1. **Trigger threshold**
   - Command executes only when `Camera.getX() >= triggerX`.
2. **One-shot behavior**
   - Command destroys itself after execution (no repeated side effects).
3. **Command type dispatch**
   - Verify representative types: `BACKGROUND_ENABLED`, `ADD_BLOCKER`, `REMOVE_BLOCKER`, `ANIMATOR`, `SPAWN`.
4. **Mount guard clauses**
   - `MOUNT` exits safely when actor IDs missing or invalid states.

## E. Stage update integration (`Stage.java`)
1. **Animator lifecycle**
   - `playAnimator` starts animator; `fixedUpdate` clears `currentAnimator` once finished.
2. **Actor add/remove queues**
   - `addActors` merged then started; destroyed actors removed from both list and ID map.

## How to run tests with existing Ant targets
From repo root (`C:\dev\GoldenAxeClone-Copilot`):

## JavaGoldenAxe
- Full suite:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml test`
- Compile tests only:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml compile-test`
- Single test class:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml -Dtest.includes=infra\ResourceParsingTest.java test-single`
- Single test method:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml -Dtest.class=infra.ResourceParsingTest -Dtest.method=testAnimatorParsing test-single-method`

## JavaGoldenAxeAnimationEditor2
- Full suite:
  - `ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml test`
- Single test class:
  - `ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml -Dtest.includes=infra\SpriteSheetTest.java test-single`

Notes:
- In this environment, `ant` is currently not installed/on PATH, so execution is blocked until tooling is installed.

## GUI-heavy smoke-test approach (pragmatic)
Because visual/audio behavior is difficult to assert headlessly, keep smoke checks lightweight and repeatable:

1. **Scripted startup smoke**
   - Launch jar, wait for title scene, verify process remains stable (no startup exception).
2. **Input-driven scene smoke**
   - Simulate or manually perform minimal key inputs to start game/demo and reach first stage updates.
3. **Log/exception capture**
   - Treat uncaught exceptions, resource load failures, and deadlocks as failures.
4. **Deterministic acceptance checklist**
   - Title loads, stage loads, first command trigger observed, first animator/cutscene plays, game loop continues.

This balances effort and confidence while parser/state-machine regression tests provide the hard migration gate.
