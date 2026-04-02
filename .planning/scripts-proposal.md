# Deterministic scripts proposal (repo root)

## Scope and constraints
- Commands run from `C:\dev\GoldenAxeClone-Copilot` (repo root).
- Reuse existing Ant targets from each project (`clean`, `compile`, `jar`, `test`, `default`, `run`).
- No new build tooling is proposed.
- Proposed script location: `scripts\` (wrapper `.cmd` files), not implemented in this task.

## Environment assumptions
- `JAVA_HOME` points to the JDK under test (JDK17 baseline or JDK25 migration run).
- `ant` is installed and available on `PATH`.
- Project build files:
  - `JavaGoldenAxe\build.xml`
  - `JavaGoldenAxeAnimationEditor2\build.xml`

## Proposed deterministic script set

### 1) `scripts\env-check.cmd`
- **Purpose:** Fail fast if toolchain is missing and print versions used for reproducibility.
- **Command body:**
  ```bat
  @echo off
  setlocal
  where java || exit /b 1
  where ant || exit /b 1
  echo JAVA_HOME=%JAVA_HOME%
  java -version
  ant -version
  endlocal
  ```
- **Expected inputs:** `JAVA_HOME`, `PATH`.
- **Outputs/artifacts:** Console output with Java/Ant versions (capturable by CI logs).

### 2) `scripts\build-all.cmd`
- **Purpose:** Deterministic clean + jar build for both projects.
- **Command body:**
  ```bat
  @echo off
  setlocal
  ant -f JavaGoldenAxe\build.xml clean jar || exit /b 1
  ant -f JavaGoldenAxeAnimationEditor2\build.xml clean jar || exit /b 1
  endlocal
  ```
- **Expected inputs:** Ant project files and project sources.
- **Outputs/artifacts:**
  - `JavaGoldenAxe\dist\JavaGoldenAxe.jar`
  - `JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar`
  - project-local `build\` and `dist\` folders refreshed.

### 3) `scripts\test-all.cmd`
- **Purpose:** Run Ant `test` target for both projects (even if no tests exist yet, command remains stable).
- **Command body:**
  ```bat
  @echo off
  setlocal
  ant -f JavaGoldenAxe\build.xml test || exit /b 1
  ant -f JavaGoldenAxeAnimationEditor2\build.xml test || exit /b 1
  endlocal
  ```
- **Expected inputs:** Any files under each project `test\` source tree.
- **Outputs/artifacts:**
  - `build\test\classes\` and `build\test\results\` under each project (if tests exist).
  - Non-zero exit code on failure.

### 4) `scripts\verify-default-all.cmd`
- **Purpose:** Full NetBeans-generated default pipeline (`test,jar,javadoc`) for both projects.
- **Command body:**
  ```bat
  @echo off
  setlocal
  ant -f JavaGoldenAxe\build.xml default || exit /b 1
  ant -f JavaGoldenAxeAnimationEditor2\build.xml default || exit /b 1
  endlocal
  ```
- **Expected inputs:** Source trees and Ant metadata in each project.
- **Outputs/artifacts:**
  - JARs in each `dist\`.
  - Javadocs in each `dist\javadoc\`.
  - Test/build outputs from Ant default dependency chain.

### 5) `scripts\smoke-jars.cmd`
- **Purpose:** Smoke gate focused on deliverables existence after build (deterministic, non-interactive).
- **Command body:**
  ```bat
  @echo off
  setlocal
  call scripts\build-all.cmd || exit /b 1
  if not exist JavaGoldenAxe\dist\JavaGoldenAxe.jar exit /b 1
  if not exist JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar exit /b 1
  echo Smoke check passed: both jars exist.
  endlocal
  ```
- **Expected inputs:** Successful `build-all` execution.
- **Outputs/artifacts:** Exit code and jar existence confirmation line.

## JDK17 baseline and JDK25 migration verification flow
Use the same scripts with only `JAVA_HOME` changed, so verification is reproducible:

1. **Baseline (JDK17):**
   - Set `JAVA_HOME` to JDK17.
   - Run `scripts\env-check.cmd`.
   - Run `scripts\build-all.cmd`, `scripts\test-all.cmd`, `scripts\smoke-jars.cmd`.
2. **Migration candidate (JDK25):**
   - Set `JAVA_HOME` to JDK25.
   - Run the same script sequence unchanged.
3. **Compare outcomes:**
   - All scripts must return exit code `0` on both JDKs.
   - Both JAR artifacts must be present in both runs.
   - Any differences in compiler/test output become migration action items.

This keeps verification deterministic by holding commands constant while varying only the JDK runtime.
