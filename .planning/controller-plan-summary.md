# Controller Support Plan Summary (Execution-Ready)

## Final decision
- **Primary library:** `input4j` (Windows-first Xbox support).
- **Fallback library:** `jinput` (if JDK/toolchain constraints block `input4j` adoption).
- **Architecture:** provider-based input system behind existing `infra.Input` API to preserve gameplay call sites and keyboard behavior.

## Phased rollout
1. **Phase 0 (complete): Planning/selection**
   - Library evaluation and selection completed.
   - Input abstraction, Windows implementation, test strategy, and cross-OS evaluation plans completed.
2. **Phase 1 (implementation, Windows)**
   - Add input abstraction types (`InputAction`, `InputSnapshot`, provider/aggregator contracts).
   - Bridge existing static `infra.Input` methods to action-level semantics.
   - Integrate Windows controller provider with Xbox default mappings and deadzone hysteresis.
   - Add focus-loss and hot-plug handling; keep keyboard fallback active.
3. **Phase 1.5 (validation hardening)**
   - Add virtual controller provider + deterministic semantics tests.
   - Run Windows smoke matrix (wired/Bluetooth, 1P/2P, reconnect, alt-tab).
4. **Phase 2 (cross-OS evaluation gates)**
   - Evaluate Linux and macOS independently against required smoke gates.
   - Ship only platforms that meet go criteria; defer failing platforms with explicit known issues.

## Dependencies and sequencing
- **D1:** Runtime/toolchain compatibility for primary path (`input4j` may require modern JDK).
- **D2:** Ant/NetBeans dependency + native packaging wiring for Windows distribution.
- **D3:** Input abstraction must land before controller provider to avoid gameplay churn.
- **D4:** Deterministic virtual-input harness should precede broad regression cycles.
- **D5:** Windows acceptance must pass before Linux/macOS promotion.

## Key risks and mitigations
- **JDK/tooling mismatch for primary library** -> maintain `jinput` fallback lane.
- **Native packaging/runtime issues on Windows** -> explicit packaging checklist + clean-machine verification.
- **Mapping drift vs keyboard semantics** -> centralize semantics in aggregator and preserve `pressed/justPressed/doublePressed` behavior.
- **Axis jitter / analog instability** -> deadzone hysteresis defaults (enter 0.35 / exit 0.25) with diagnostics.
- **Disconnect/reconnect/focus-loss edge bugs** -> force-release and transition reset policies with smoke coverage.
- **Cross-OS backend variance** -> independent Linux/macOS go/no-go gates and staged rollout.

## Acceptance criteria
### Windows Phase 1 (must pass)
1. Keyboard behavior remains unchanged in title/select/gameplay/pause flows.
2. One Xbox controller fully controls P1 (move/attack/jump/magic/start/cancel).
3. Two controllers support stable P1/P2 assignment.
4. Double-press behavior remains equivalent to existing semantics.
5. Deadzone/hysteresis prevents neutral jitter while preserving responsiveness.
6. Disconnect/reconnect and focus-loss do not cause stuck or phantom inputs.
7. Diagnostics expose connection, assignment, mappings, and raw-vs-filtered state.
8. Build/package outputs include required controller dependencies.

### Cross-OS promotion (post-Windows)
- Linux/macOS evaluated separately; each must pass its required smoke matrix before being promoted from limited/opt-in support.

## Immediate implementation-start checklist
1. Create `infra.input` abstraction classes and aggregator.
2. Refactor `infra.Input` into compatibility facade over `InputSystem`.
3. Add Windows `ControllerInputProvider` + Xbox mapping defaults + deadzone config.
4. Wire update/focus lifecycle in `GoldenAxeGame` and `GameCanvas`.
5. Add virtual provider and deterministic tests (`ant -noinput -buildfile JavaGoldenAxe\build.xml test` once implementation exists).
