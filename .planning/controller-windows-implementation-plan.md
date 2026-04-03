# Windows Controller Implementation Plan (Phase 1)

## Scope

Phase 1 delivers Windows Xbox-controller support while preserving existing keyboard behavior and existing `Input.isKeyPressed/isKeyJustPressed/isKeyDoublePressed` call sites.

Primary backend: `input4j` (with `jinput` fallback plan if JDK/tooling constraints block rollout).

---

## 1) Implementation sequence (file-level plan)

1. **Dependency wiring / runtime packaging**
   - `JavaGoldenAxe\build.xml`
   - `JavaGoldenAxe\nbproject\project.properties` (if classpath/native config is needed there)
   - Goal: add controller library jars and native loading expectations for Windows.

2. **Action abstraction types**
   - `JavaGoldenAxe\src\infra\input\InputAction.java` (new)
   - `JavaGoldenAxe\src\infra\input\InputSnapshot.java` (new)
   - `JavaGoldenAxe\src\infra\input\InputUpdateContext.java` (new)
   - Goal: represent game actions and frame snapshots independent of keycodes.

3. **Provider contracts + aggregator**
   - `JavaGoldenAxe\src\infra\input\InputProvider.java` (new)
   - `JavaGoldenAxe\src\infra\input\ProviderSample.java` (new)
   - `JavaGoldenAxe\src\infra\input\InputAggregator.java` (new)
   - Goal: union provider state and preserve current pressed/just-pressed/double-pressed semantics.

4. **Keyboard provider migration**
   - `JavaGoldenAxe\src\infra\input\KeyboardInputProvider.java` (new)
   - `JavaGoldenAxe\src\infra\Input.java` (bridge old static API to action system)
   - Goal: preserve current behavior with no gameplay call-site changes.

5. **Windows controller provider**
   - `JavaGoldenAxe\src\infra\input\ControllerInputProvider.java` (new)
   - `JavaGoldenAxe\src\infra\input\ControllerMappings.java` (new)
   - `JavaGoldenAxe\src\infra\input\ControllerConfig.java` (new)
   - Goal: map Xbox inputs to `InputAction` and publish per-player action states.

6. **Runtime integration points**
   - `JavaGoldenAxe\src\infra\GoldenAxeGame.java` (call `InputSystem.update()` at start of `update`)
   - `JavaGoldenAxe\src\infra\GameCanvas.java` (focus lost/gained hooks)
   - `JavaGoldenAxe\src\infra\Input.java` (initialize/register providers)
   - Goal: ensure one stable input snapshot per frame and robust focus handling.

7. **Diagnostics and guardrails**
   - `JavaGoldenAxe\src\infra\input\InputDiagnostics.java` (new)
   - optional debug toggle location: `JavaGoldenAxe\src\infra\Settings.java`
   - Goal: troubleshooting visibility for mapping, deadzones, assignment, and disconnects.

---

## 2) Xbox mapping defaults (Windows)

Default profile targets standard Xbox layout (XInput naming):

### Global/UI
- `START_1`: **Menu / Start** button on Controller 1
- `START_2`: **Menu / Start** button on Controller 2
- `CANCEL`: **B** button (both controllers)
- `TOGGLE_ASPECT`: keyboard-only (F11) for phase 1
- `TOGGLE_FULLSCREEN`: keyboard-only (F12) for phase 1

### Player 1 (Controller 1)
- Movement:
  - Left stick X/Y digitalized to `P1_LEFT/RIGHT/UP/DOWN`
  - D-pad mapped directly to `P1_LEFT/RIGHT/UP/DOWN`
- Actions:
  - `P1_ATTACK` = **X**
  - `P1_JUMP` = **A**
  - `P1_MAGIC` = **Y**
- Optional mirrors:
  - `P1_ATTACK` also **RB**
  - `P1_JUMP` also **A** only (no duplicate default)
  - `P1_MAGIC` also **LB**

### Player 2 (Controller 2)
- Movement:
  - Left stick X/Y digitalized to `P2_LEFT/RIGHT/UP/DOWN`
  - D-pad mapped directly to `P2_LEFT/RIGHT/UP/DOWN`
- Actions:
  - `P2_ATTACK` = **X**
  - `P2_JUMP` = **A**
  - `P2_MAGIC` = **Y**
- Optional mirrors:
  - `P2_ATTACK` also **RB**
  - `P2_MAGIC` also **LB**

### Trigger policy
- Phase 1 default: LT/RT are **not bound** to gameplay actions.
- Keep trigger-to-action mapping configurable for later remap/accessibility phase.

---

## 3) Deadzone and threshold defaults

Use axis hysteresis to avoid jitter near neutral.

- **Left stick movement**
  - Enter threshold: `0.35`
  - Exit threshold: `0.25`
  - Applies independently per axis (X and Y).

- **D-pad**
  - Digital source, no deadzone required.

- **Triggers**
  - If later used as digital actions: press threshold `>= 0.55`, release threshold `<= 0.45`.
  - Not active in phase 1 defaults.

- **Polling**
  - Poll every game frame; aggregate into one frame snapshot before scene update.

---

## 4) Hot-plug / disconnect handling plan

1. Poll device list each frame (or every N frames with cached list + immediate recheck on failure).
2. Assign first active controller to P1 and second active controller to P2.
3. Keep stable assignment by device identifier while connected.
4. On disconnect:
   - Mark provider `isConnected=false`.
   - Force-release all actions from that provider immediately.
   - Clear just-pressed and double-press transitional state for affected actions.
5. On reconnect:
   - Reassign by first-available slot policy.
   - Require fresh up->down transitions (no inherited pressed state).
6. If only one controller is connected, keyboard remains valid for both players per current behavior.
7. If more than two controllers are connected, extras are ignored in phase 1 (log warning in diagnostics).

---

## 5) Diagnostics and troubleshooting plan

### Runtime diagnostics (debug mode)
- Show connected controllers, IDs, backend, and assigned player slot.
- Show raw left-stick axes and post-deadzone digital direction states.
- Show button states for A/B/X/Y/LB/RB/Start.
- Log transitions:
  - connect/disconnect
  - assignment changes
  - mapping profile applied
  - fallback backend activation (if `jinput` used)

### Troubleshooting checklist
1. Verify controller appears in diagnostics as connected.
2. Verify player slot assignment (P1/P2) is correct.
3. Verify raw axis values move beyond enter threshold.
4. If no movement, temporarily reduce deadzone to `0.25/0.15` and retest.
5. If button mismatch, verify mapping profile is Xbox default (not generic/DirectInput variant).
6. Verify keyboard input still works in same scene to isolate provider failure.
7. Check disconnect/reconnect logs for stale assignment or duplicate devices.

### Logging expectations
- No per-frame spam in normal mode.
- Debug mode can include per-frame condensed state line.
- Errors should include controller ID, backend, and mapping context.

---

## 6) Acceptance criteria (Windows Phase 1)

1. Existing keyboard controls still pass manual smoke checks across title, select player, gameplay, pause/continue flows.
2. One Xbox controller can fully control Player 1 (movement, attack, jump, magic, start/cancel paths).
3. Two Xbox controllers can control Player 1 and Player 2 simultaneously with stable assignment.
4. Double-tap run behavior remains equivalent to current keyboard semantics.
5. Deadzone/hysteresis prevents neutral jitter while keeping movement responsive.
6. Disconnect during gameplay does not leave stuck movement/actions.
7. Reconnect requires fresh input transitions and does not generate phantom just-pressed events.
8. Focus loss/regain clears pressed state (no stuck controls after alt-tab).
9. Diagnostics clearly expose connection, mapping, assignment, and raw-vs-filtered input state.
10. Build/runtime packaging includes required controller dependencies for Windows test machines.
