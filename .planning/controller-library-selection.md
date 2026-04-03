# Controller Library Selection (Windows-first Xbox support)

## Final choice
- **Primary library:** **input4j** (`gurkenlabs/input4j`)
- **Fallback library:** **jinput** (prefer upstream `jinput/jinput`; use maintained fork only if required fixes are unavailable upstream)

## Decision rationale vs must-have criteria
Must-have criteria for this phase are Windows-first Xbox controller quality, active maintenance, permissive licensing, and feasible integration into the current Ant/NetBeans architecture.

1. **Windows Xbox support quality (highest priority):**
   - `input4j` is the strongest fit due to explicit XInput + DirectInput support and modern controller handling.
   - `jinput` remains acceptable but is less ergonomic/modern for Xbox-focused mappings.

2. **Maintenance signal (recent activity):**
   - `input4j` and the evaluated `jinput` fork both satisfy the “recently updated” expectation from research.
   - `input4j` has the best current momentum for this decision window.

3. **Cross-platform runway (Linux/macOS after Windows):**
   - `input4j` has clear Linux/macOS backend story (evdev + IOKit), matching the later cross-OS rollout model.
   - `jinput` can still serve as a conservative fallback if runtime/tooling constraints block primary adoption.

4. **Integration feasibility in this codebase:**
   - Current input architecture is centralized in `infra.Input` and key bindings in `infra.Settings`, making an adapter layer feasible without wide scene/actor rewrites.
   - Primary integration risk is JDK/toolchain compatibility, not call-site sprawl.

## Licensing note
- **input4j:** MIT (per evaluation)
- **jinput:** BSD-style (per evaluation)
- Both are permissive and compatible with this project’s distribution goals. Keep NOTICE/license attribution in release packaging when dependencies are introduced.

## Integration strategy for this Ant/NetBeans project
1. Add controller library jars (and any native artifacts required by the chosen library) into the Ant classpath strategy used by the NetBeans project.
2. Introduce a small controller adapter layer in infrastructure terms (conceptually adjacent to `infra.Input`) so gameplay code continues to consume project-level actions, not vendor APIs.
3. Map controller state to existing action semantics defined by `infra.Settings` key/action model (start/cancel, P1/P2 directional/actions).
4. Keep keyboard path as canonical fallback at runtime.
5. Gate Linux/macOS enablement behind the post-Windows go/no-go criteria already defined in cross-OS evaluation.

## Known risks and mitigations
- **Risk: input4j requires modern JDK (22+) while environment/tooling may lag.**
  - **Mitigation:** adopt input4j only on JDK-upgraded build lane; keep `jinput` fallback path ready for compatibility-constrained environments.

- **Risk: Native/controller backend behavior differences across devices (especially clone pads/Bluetooth reconnect).**
  - **Mitigation:** enforce Windows smoke matrix first (wired + Bluetooth Xbox baseline), then apply Linux/macOS gated rollout.

- **Risk: Dependency/native packaging complexity in Ant distribution outputs.**
  - **Mitigation:** define explicit packaging checklist for jars/natives and verify runnable jar distribution on clean Windows machines.

- **Risk: Mapping drift between keyboard and controller semantics.**
  - **Mitigation:** preserve project-level action mapping contract in adapter and add repeatable manual regression checklist around `infra.Input`-driven gameplay actions.

## Explicit platform recommendation
- **Windows-first recommendation:** proceed with **input4j as primary** for Xbox controller support on Windows, with **jinput as fallback** if JDK/toolchain constraints block rollout.
- **Linux implication:** do not commit to full support yet; evaluate only after Windows exit criteria pass, then ship Linux as opt-in/conditional per defined smoke gates.
- **macOS implication:** same staged policy as Linux; require baseline parity and reconnect stability before promoting from limited support to standard support.
