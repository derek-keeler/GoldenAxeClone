# Controller input abstraction design

## Goals

- Preserve current keyboard semantics used by `Input.isKeyPressed`, `Input.isKeyJustPressed`, and `Input.isKeyDoublePressed`.
- Add provider-based input support so keyboard, controller, and future virtual/on-screen sources can coexist.
- Minimize gameplay code churn by keeping the existing static `Input` query surface during migration.

## Proposed architecture

### 1) `InputAction` (enum)

Canonical game-level actions (not device-level codes), e.g.:

- Global/UI: `START_1`, `START_2`, `CANCEL`, `TOGGLE_ASPECT`, `TOGGLE_FULLSCREEN`
- Player 1: `P1_UP`, `P1_DOWN`, `P1_LEFT`, `P1_RIGHT`, `P1_ATTACK`, `P1_JUMP`, `P1_MAGIC`
- Player 2: `P2_UP`, `P2_DOWN`, `P2_LEFT`, `P2_RIGHT`, `P2_ATTACK`, `P2_JUMP`, `P2_MAGIC`

`Settings` remains the source of keyboard keycodes, while mapping tables translate keycodes/buttons/axes to `InputAction`.

### 2) `InputSnapshot` (immutable frame snapshot)

Represents resolved action state for one update frame:

- `pressed: EnumSet<InputAction>`
- `justPressed: EnumSet<InputAction>`
- `doublePressed: EnumSet<InputAction>`
- `timestampNanos`
- Optional per-provider diagnostics (connected flags, raw axis values) for debugging only

The snapshot is built once per frame and read many times by gameplay code.

### 3) `InputProvider` (interface)

Device/source abstraction:

- `String id()`
- `int priority()` (tie-breaks/conflict ordering when needed)
- `void poll(InputUpdateContext ctx)` (collect latest raw state)
- `ProviderSample sample()` (current sampled actions + metadata)
- `boolean isConnected()`
- `void onFocusLost()` / `void onFocusGained()`

Implementations:

- `KeyboardInputProvider` (AWT `KeyListener` backed)
- `ControllerInputProvider` (library-backed, e.g. input4j/jinput wrapper)
- `VirtualInputProvider` (future replay, AI, touchscreen, network, scripted demo)

### 4) `InputAggregator` (composition + semantics engine)

Responsibilities:

- Poll all providers each frame.
- Merge provider action sets into one logical action state.
- Track per-action transitions and timing history.
- Compute `pressed/justPressed/doublePressed` semantics.
- Handle provider disconnect/focus-loss reset.

### 5) `InputSystem` (runtime owner)

Single owner used by `infra.Input` facade:

- Holds registered providers.
- Owns current/previous `InputSnapshot`.
- Exposes `update()` called once per game update tick.
- Exposes query methods used by `Input` static methods.

## Preserving existing key semantics

Current behavior to preserve exactly:

1. **Pressed**: true while key/action is held.
2. **Just pressed**: true once after transition to pressed; consumed until release.
3. **Double pressed**: requires press-release-press sequence within `DOUBLE_PRESSED_TIME` (250ms) and currently held on second press; returns true once and resets sequence.

### Equivalent action-level algorithm

For each `InputAction`, store:

- `isDown`
- `justPressConsumed`
- `firstPressTime`
- `releaseTimeAfterFirstPress`

Rules:

- On transition up->down:
  - if no valid first press or timeout exceeded: set `firstPressTime=now`, clear release marker.
  - else keep previous release marker for possible double-press completion.
  - set `isDown=true`.
- On transition down->up:
  - if `now - firstPressTime <= DOUBLE_PRESSED_TIME`, set `releaseTimeAfterFirstPress=now`; else reset both markers.
  - clear `justPressConsumed`.
  - set `isDown=false`.
- `isActionJustPressed(action)`:
  - true only if `isDown` and not consumed; then mark consumed.
- `isActionDoublePressed(action)`:
  - true when `firstPressTime>0`, `releaseTimeAfterFirstPress>0`, `now-releaseTimeAfterFirstPress <= DOUBLE_PRESSED_TIME`, and `isDown=true`; then reset both markers.

This reproduces current `Input` contract while moving from keycode state to action state.

## Provider composition (keyboard + controller)

Default strategy: **union by action**.

- If any connected provider reports an action pressed, aggregated action is pressed.
- `justPressed` is triggered by aggregated transition up->down (not per provider duplication).
- Prevent duplicate edge generation when keyboard and controller press same action on same frame.

Optional policy hook (future): `InputConflictPolicy`

- `UNION` (default)
- `PRIORITY_FIRST` (higher-priority provider wins for directional actions)
- `LAST_SOURCE_WINS` (debug/testing scenarios)

Recommended initial rollout: `UNION` everywhere for least surprise and least code churn.

## Poll/update lifecycle integration

Current loop in `GameCanvas`:

- `goldenAxeGame.update(delta)` every iteration
- `fixedUpdate()` at fixed step

Integration plan:

1. Keep AWT event capture in `KeyboardInputProvider` (event-driven raw state).
2. Call `InputSystem.update()` exactly once at start of `GoldenAxeGame.update(delta)` (or start of `SceneManager.update`), before scene logic reads input.
3. Keep `fixedUpdate()` input-agnostic unless deterministic replay mode is introduced.
4. On window/canvas focus lost, invoke `InputSystem.onFocusLost()` to clear pressed state and consumption flags.

This guarantees a stable per-frame snapshot and avoids mid-frame races from asynchronous event callbacks.

## Backward-compatible migration plan (low churn)

### Phase 1: Introduce abstraction behind existing API

- Keep `infra.Input` static methods and signatures unchanged.
- Internally route:
  - `isKeyPressed(int keyCode)` -> map keycode to `InputAction` -> query `InputSystem`.
  - same for `isKeyJustPressed` and `isKeyDoublePressed`.
- Register only `KeyboardInputProvider`.

Result: zero gameplay call-site changes.

### Phase 2: Add controller mappings

- Add controller-to-action map with sensible defaults for Xbox layout.
- Register `ControllerInputProvider`.
- Continue serving existing key-based APIs through action mapping.

Result: keyboard behavior unchanged; controller starts working in parallel.

### Phase 3: Optional action-native API

- Add `Input.isActionPressed(InputAction)` etc.
- Gradually migrate selected gameplay code to action-native calls where clearer.
- Keep keycode methods as compatibility layer until full migration is desirable.

Result: incremental modernization without forced churn.

## Edge-case handling

### 1) Controller disconnect/reconnect

- On disconnect: provider reports `isConnected=false`; aggregator treats all actions from that provider as released immediately.
- Clear transitional flags to avoid phantom `justPressed`/`doublePressed`.
- On reconnect: require fresh up->down transition before `justPressed`/`doublePressed` can fire.

### 2) Analog deadzones

- Convert axes to digital directional actions with configurable thresholds:
  - enter threshold (e.g. 0.35)
  - exit threshold (e.g. 0.25) for hysteresis
- Hysteresis prevents rapid flicker near neutral.
- Deadzone config should be per-axis/per-provider overridable.

### 3) Multiple controllers

- Enumerate devices with stable provider IDs.
- Default assignment:
  - first active controller -> Player 1 action group
  - second active controller -> Player 2 action group
- If >2 controllers, extras ignored by default (or exposed for remap UI later).

### 4) Focus loss / alt-tab

- On focus lost: force release all actions and clear consumed/double-press trackers.
- Prevents stuck movement and accidental trigger on focus regain.

### 5) Simultaneous opposite directions

- Preserve current behavior by exposing both pressed states at action layer and letting existing gameplay precedence (`if/else` ordering) decide movement.
- Optional later policy: neutralize opposite direction pair at aggregator level if desired.

## Notes aligned with controller-library evaluation

- Design isolates provider implementation from game logic, so backend can be swapped (`input4j` primary candidate, `jinput` fallback) with minimal impact.
- Keeping action semantics centralized in `InputAggregator` prevents library-specific quirks from leaking into scenes/actors.
