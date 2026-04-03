# Controller Mimic and Testing Strategy

## Objective
Define a deterministic, testable controller path that preserves existing `Input` semantics while enabling:
- automated edge/combo verification
- manual Xbox-like controller mimic via keyboard
- CI-safe functional validation without physical gamepads

## 1) Virtual controller API shape proposal
Introduce a test-focused virtual provider that plugs into the planned `InputProvider`/`InputAggregator` model.

### `VirtualControllerProvider` (concept)
- `String id()` (example: `virtual-p1`)
- `int priority()` (default below keyboard unless explicitly overridden for tests)
- `void poll(InputUpdateContext ctx)` (applies scheduled events for current frame/time)
- `ProviderSample sample()` (returns current `EnumSet<InputAction>` + optional diagnostics)
- `boolean isConnected()` (always true unless intentionally toggled for disconnect tests)
- `void onFocusLost()` / `void onFocusGained()` (clear held state and edge trackers)

### Test control surface (deterministic injection)
- `void connect()` / `void disconnect()`
- `void setAction(InputAction action, boolean down)` (immediate state override)
- `void queueEvent(long atNanos, InputAction action, boolean down)` (time-scripted)
- `void queueFrameEvent(int frameIndex, InputAction action, boolean down)` (frame-scripted)
- `void clearQueue()`
- `void resetState()`

### `VirtualInputScript` DTO (for reusable scenarios)
- `name`
- `tickNanos` (fixed step used by test harness)
- `List<VirtualInputEvent>` where event = `{frame|nanos, action, down}`
- optional assertions metadata (expected pressed/justPressed/doublePressed checkpoints)

## 2) Deterministic test scenarios (action edges + combos)
All scenarios should validate `pressed`, `justPressed`, and `doublePressed` behavior at action level.

### Edge semantics
1. **Single press edge**
   - Frame N: `P1_ATTACK down=true`
   - Expect: `pressed=true`, `justPressed=true` once, then `justPressed=false` while held.
2. **Release reset**
   - After release, expect `pressed=false`; next fresh press can emit `justPressed=true`.
3. **Double-press success**
   - press -> release -> press within `DOUBLE_PRESSED_TIME` (250ms)
   - Expect one `doublePressed=true` pulse on second hold.
4. **Double-press timeout**
   - same sequence but second press after timeout
   - Expect no `doublePressed`.
5. **Focus-loss cleanup**
   - hold direction, trigger focus lost
   - Expect held state cleared; no phantom `justPressed` on regain.
6. **Disconnect cleanup**
   - hold action, disconnect virtual provider
   - Expect immediate release and no stale edge events on reconnect.

### Combo semantics
1. **Two-button combo window**
   - `P1_JUMP` then `P1_ATTACK` within configured combo window.
   - Assert both action edges occur in-order and remain deterministic by frame index.
2. **Simultaneous combo**
   - same-frame `P1_JUMP + P1_ATTACK`.
   - Assert both are visible as pressed; ordering handled by combo resolver rules.
3. **Directional + action combo**
   - hold `P1_RIGHT` while tapping `P1_ATTACK`.
   - Assert movement hold continuity and attack edge pulse.
4. **Opposite-direction stress**
   - inject `P1_LEFT` and `P1_RIGHT` overlaps.
   - Validate current policy (both exposed; gameplay if/else precedence decides).

## 3) Manual mimic flow (keyboard-to-virtual mapping)
Add a debug-only adapter that maps keyboard events to virtual Xbox-like actions, without requiring hardware.

### `KeyboardToVirtualAdapter` concept
- Listens to keyboard provider raw key transitions.
- Translates keys to virtual gamepad controls and forwards to `VirtualControllerProvider`.
- Toggle enable/disable at runtime (example debug flag: `input.mimic.enabled`).

### Suggested default mapping (P1, Xbox-like)
- D-Pad / Left Stick digital:
  - `W/A/S/D` -> `P1_UP/LEFT/DOWN/RIGHT`
- Face buttons:
  - `J` -> `P1_ATTACK` (X)
  - `K` -> `P1_JUMP` (A)
  - `L` -> `P1_MAGIC` (Y/B equivalent per game semantics)
- System:
  - `Enter` -> `START_1`
  - `Esc` -> `CANCEL`
- Optional shoulder mimic:
  - `U`/`I` reserved for future LB/RB actions

### Manual validation flow
1. Start game with mimic mode enabled.
2. Use mapped keys to navigate title/start flow.
3. Verify movement, attack, jump, magic, and start/cancel interactions.
4. Run edge checks manually:
   - tap vs hold attack
   - rapid double-tap direction/action
   - mixed directional+action sequences
5. Capture outcomes in a repeatable checklist for regressions.

## 4) Programmatic functional testing strategy
Build tests around a headless input harness, not full rendering assertions.

### Harness design
- `InputSystemTestHarness` with fixed time-step (`tickNanos`) and deterministic frame advancement.
- Register providers:
  - `KeyboardInputProvider` optional
  - `VirtualControllerProvider` required
- For each frame:
  1. apply queued virtual events
  2. call `InputSystem.update()`
  3. assert snapshot/action queries

### Test layers
1. **Unit-level semantics tests**
   - validate edge and double-press behavior per action.
2. **Provider composition tests**
   - union policy correctness when keyboard and virtual provider press same action.
3. **Functional scene-input tests (minimal integration)**
   - scripted input drives title/start and first controllable state transitions.
   - assert logical state changes/events, not pixels/audio.

### Execution approach with current repo constraints
- No current test classes exist; add tests incrementally under existing Ant `test` targets when implementation begins.
- Primary command target for game module:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml test`

## 5) CI compatibility notes and limitations
1. **No physical controller dependency in CI**
   - CI should run only keyboard + virtual provider tests.
2. **Ant/JUnit path already available in project structure**
   - NetBeans/Ant `test` target is the integration point.
3. **Current environment limitation**
   - local environment may be blocked when `ant` is unavailable; CI agent must provide Ant.
4. **Timing stability**
   - avoid wall-clock sleeps; use frame/time injection to eliminate flaky timing.
5. **Backend decoupling**
   - tests should not depend on `input4j`/`jinput` runtime presence for core semantics.
6. **Manual-only coverage areas**
   - real hardware quirks (Bluetooth reconnect, vendor-specific axis noise) remain smoke-test/manual until dedicated hardware lanes are added.

## Recommended rollout
1. Implement virtual provider + keyboard-to-virtual adapter behind debug/test flags.
2. Add deterministic input semantics tests first.
3. Add minimal scripted functional tests for title/start/control readiness.
4. Keep hardware-controller validation as a separate Windows smoke matrix.
