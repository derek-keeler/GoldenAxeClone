# Final Validation Gates for JDK25 Upgrade

## Objective
Define the final go/no-go criteria to declare the JDK25 upgrade complete for:
- `JavaGoldenAxe`
- `JavaGoldenAxeAnimationEditor2`

## Go/No-Go Gates

### Gate 0: Toolchain readiness (mandatory)
**Go when all are true:**
- JDK 17 and JDK 25 are both installed and selectable.
- `JAVA_HOME` is explicitly set to the active lane (17 or 25).
- `ant` 1.10+ is installed and on `PATH`.
- `java -version`, `javac -version`, `ant -version`, `git --version` all succeed.

**No-go if any tool/version check fails.**

### Gate 1: Baseline reproducibility (JDK17 lane)
**Go when all are true (same commands used later for JDK25):**
- `ant -noinput -buildfile JavaGoldenAxe\build.xml clean jar` succeeds.
- `ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml clean jar` succeeds.
- (If tests exist) `ant -noinput -buildfile <project>\build.xml test` succeeds for both projects.
- Both jars are produced:
  - `JavaGoldenAxe\dist\JavaGoldenAxe.jar`
  - `JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar`

**No-go if baseline cannot be reproduced first.**

### Gate 2: JDK25 build/test parity
**Go when all are true under JDK25:**
- Exact same build/test commands from Gate 1 succeed for both projects.
- No new compile errors, linkage errors, or build target failures.
- Regression test outputs (if present) show no failures versus baseline.

**No-go on any new functional or build regression.**

### Gate 3: Runtime smoke validation (both projects)
**Go when all are true under JDK25:**
- `java -jar JavaGoldenAxe\dist\JavaGoldenAxe.jar` launches and stays stable long enough to validate startup/title flow.
- `java -jar JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar` launches and stays stable long enough to validate editor startup.
- No uncaught startup exceptions, immediate crashes, or missing-resource failures.

**No-go if either app fails smoke startup/stability.**

### Gate 4: CI green criteria
**Go when all are true:**
- CI runs on JDK25.
- CI must build **both** projects (not only `JavaGoldenAxe`).
- All required checks for the upgrade PR are green on latest commit.
- No flaky/retried failures required to get green.

**No-go if any required CI check is red or incomplete.**

## Required Evidence Artifacts
Capture and attach/share these artifacts for final sign-off:

1. **Environment evidence**
   - Console output for:
     - `java -version`
     - `javac -version`
     - `ant -version`
     - `git --version`
     - `Get-Command java,javac,ant,git | Select-Object Name,Source`
   - `JAVA_HOME` value for JDK17 and JDK25 runs.

2. **Build evidence (both lanes, both projects)**
   - Full console logs for each Ant build command.
   - Confirmation of jar file creation paths.

3. **Test evidence**
   - Full test output logs (or explicit “no tests present” statement with command output).
   - Any generated test result files from Ant (`build\test\results\` when available).

4. **Smoke evidence**
   - Timestamped run logs for both jar launches.
   - Outcome note: pass/fail, observed behavior, exception summary (if any).

5. **CI evidence**
   - Workflow run URL(s) for the final commit.
   - Check summary showing all required checks green.
   - Job logs proving both projects were built under JDK25.

## Environment Reproducibility Requirements
- Execute from repo root: `C:\dev\GoldenAxeClone-Copilot`.
- Use Windows PowerShell with Windows-style paths.
- Keep command sequence identical between JDK17 baseline and JDK25 candidate; only `JAVA_HOME` changes.
- Record versions and command outputs in each lane before build/test/smoke steps.
- Do not mix partial outputs from different machines without labeling host and timestamp.

## Implementation-Phase Execution Checklist
- [ ] Set JDK17 lane (`JAVA_HOME`) and record tool versions.
- [ ] Build both projects on JDK17; collect logs and jar evidence.
- [ ] Run tests on JDK17 (if present); collect outputs.
- [ ] Switch to JDK25 lane (`JAVA_HOME`) and record tool versions.
- [ ] Re-run exact same build/test commands on JDK25 for both projects.
- [ ] Launch both jars on JDK25 and record smoke outcomes.
- [ ] Verify CI (JDK25) is green and includes both projects.
- [ ] Confirm all evidence artifacts are attached and traceable.
- [ ] Declare **GO** only if all gates pass; otherwise declare **NO-GO** with failing gate and remediation owner.
