# Controller Cross-OS Evaluation (Post-Windows)

## Context and decision model
- Windows remains the implementation and quality baseline.
- Linux and macOS are evaluated only after Windows reaches stable controller support.
- Primary target on every OS remains Xbox One / Xbox-compatible controllers.
- Each OS gets an independent feasibility decision (accept, defer, or reject).

## Shared Windows exit criteria before cross-OS work
- Windows controller path is stable across wired and Bluetooth Xbox-compatible devices.
- No critical controller regressions for two consecutive planned releases.
- Input mapping, reconnect handling, and deadzone behavior are documented and repeatable.

---

## Linux evaluation

### Controller backend expectations
- Primary backend expectation: SDL-based controller mapping path with reliable Xbox mapping normalization.
- Must detect wired and Bluetooth Xbox-compatible controllers without custom per-device hacks.
- Must expose stable button/axis identity equivalent to Windows gameplay expectations.
- Reconnect events must preserve playable state (no stuck input, no crash).

### Smoke test matrix (Linux)
| Scenario | Device/Path | Expected Result | Gate Type |
|---|---|---|---|
| Device discovery | Xbox One wired USB | Device appears and binds to player slot | Required |
| Device discovery | Xbox-compatible Bluetooth | Device appears within timeout and binds | Required |
| Gameplay input | D-pad + left stick movement | Consistent directional movement, no axis inversion | Required |
| Gameplay input | Face buttons + triggers | Correct action mapping, analog trigger range stable | Required |
| Session resilience | Disconnect/reconnect during gameplay | Input recovers, no restart required | Required |
| Multi-controller sanity | Two Xbox-compatible devices | Correct player assignment and no cross-talk | Recommended |

### Compatibility risks (Linux)
- Distribution and desktop stack variance (udev/permissions/Bluetooth stack differences).
- SDL/gamepad database inconsistencies for clone devices.
- Driver-level axis noise requiring per-device deadzone tuning.
- Wayland/X11 environment differences affecting focus/input acquisition.

### Go/No-Go gates (Linux)
- **Go** if all required smoke tests pass on at least two representative Linux environments and no critical gameplay/input bugs remain.
- **Conditional Go** if only non-critical clone-device issues remain and clear known-issues guidance exists.
- **No-Go** if Xbox One baseline devices fail discovery, mapping correctness, or reconnect reliability.

### Rollout strategy if accepted (Linux)
1. Release as opt-in platform support behind explicit release-note scope.
2. Publish tested distro/environment list and known unsupported controller scenarios.
3. Run one stabilization release cycle with enhanced controller telemetry/log collection.
4. Promote to standard supported platform after one cycle without critical controller regressions.

---

## macOS evaluation

### Controller backend expectations
- Primary backend expectation: SDL + native HID path compatibility for Xbox-compatible controllers.
- Must support modern macOS versions used by active player base.
- Wired and Bluetooth controller sessions must maintain stable mapping and reconnect behavior.
- Permission and security prompts must not block normal controller gameplay after setup.

### Smoke test matrix (macOS)
| Scenario | Device/Path | Expected Result | Gate Type |
|---|---|---|---|
| Device discovery | Xbox controller wired (USB-C/adapter) | Device detected and mapped correctly | Required |
| Device discovery | Xbox controller Bluetooth | Pairing + runtime detection succeed | Required |
| Gameplay input | Movement + actions in active gameplay | Controls match Windows behavior | Required |
| Trigger/axis fidelity | Analog trigger and stick sweep | No clipping/inversion; deadzone acceptable | Required |
| Session resilience | Sleep/wake with controller connected | Input remains functional after wake | Required |
| Session resilience | Disconnect/reconnect in gameplay | Rebind succeeds without process restart | Required |
| Multi-controller sanity | Two controllers connected | Stable assignments, no ghost inputs | Recommended |

### Compatibility risks (macOS)
- HID/Bluetooth behavior differences across macOS versions.
- Apple security/privacy permission prompts impacting first-run input.
- Hardware and adapter variability (Intel vs Apple Silicon environments).
- Potential controller firmware-specific quirks on Bluetooth reconnect.

### Go/No-Go gates (macOS)
- **Go** if all required smoke tests pass on at least one Intel and one Apple Silicon system (or equivalent validated coverage) with no critical controller defects.
- **Conditional Go** if only minor, documented issues remain and do not affect Xbox baseline playability.
- **No-Go** if baseline Xbox discovery, mapping parity, or reconnect reliability fails.

### Rollout strategy if accepted (macOS)
1. Launch as limited support tier with explicit tested hardware/macOS matrix.
2. Document required setup/permissions in release notes.
3. Track first-release controller incidents as a dedicated triage category.
4. Graduate to full support after one stable release cycle meeting controller reliability targets.

---

## Final acceptance governance
- Linux and macOS decisions are independent; one platform may ship while the other is deferred.
- Any accepted platform must preserve Xbox One/Xbox-compatible controller baseline behavior established on Windows.
- Deferred platforms remain in evaluation backlog with explicit unresolved gate failures.
