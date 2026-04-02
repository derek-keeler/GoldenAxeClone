# JDK25 Config Upgrade Plan (build metadata + CI only)

## Scope inspected
- `JavaGoldenAxe\build.xml`
- `JavaGoldenAxe\nbproject\project.properties`
- `JavaGoldenAxe\nbproject\build-impl.xml` (NetBeans-generated, do not hand-edit)
- `JavaGoldenAxeAnimationEditor2\build.xml`
- `JavaGoldenAxeAnimationEditor2\nbproject\project.properties`
- `JavaGoldenAxeAnimationEditor2\nbproject\build-impl.xml` (NetBeans-generated, do not hand-edit)
- `.github\workflows\ant.yml`

## Current baseline (relevant keys)
- `JavaGoldenAxe\nbproject\project.properties`
  - `javac.source=16`
  - `javac.target=16`
  - `platform.active=default_platform`
- `JavaGoldenAxeAnimationEditor2\nbproject\project.properties`
  - `javac.source=1.8`
  - `javac.target=1.8`
  - `platform.active=default_platform`
- `.github\workflows\ant.yml`
  - `java-version: '17'`
  - only builds `JavaGoldenAxe` (`ant -noinput -buildfile JavaGoldenAxe/build.xml`)

## Proposed staged rollout (minimal risk)

### Stage 1: Move runtime/toolchain to JDK25 first, keep language levels unchanged
**Goal:** Validate JDK25 compatibility without changing Java language level yet.

#### File-level edits
1) `.github\workflows\ant.yml`
- Change:
  - `java-version: '17'` -> `java-version: '25'`
- Change build step so CI covers both projects:
  - from:
    - `ant -noinput -buildfile JavaGoldenAxe/build.xml`
  - to:
    - `ant -noinput -buildfile JavaGoldenAxe\build.xml`
    - `ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml`

2) No source/target edits yet in either `nbproject\project.properties`.

**Rationale:** isolates JDK runtime/toolchain risk from language-level risk.

---

### Stage 2: Adopt `javac.release` while preserving effective compatibility
**Goal:** reduce cross-compilation ambiguity on newer JDKs.

#### File-level edits
1) `JavaGoldenAxe\nbproject\project.properties`
- Add: `javac.release=16`
- Remove or comment out:
  - `javac.source=16`
  - `javac.target=16`

2) `JavaGoldenAxeAnimationEditor2\nbproject\project.properties`
- Add: `javac.release=8`
- Remove or comment out:
  - `javac.source=1.8`
  - `javac.target=1.8`

**Rationale:** `--release` is the safer modern compiler contract on JDK9+; behavior is closer to intended API/language targeting.

---

### Stage 3: Raise language level in controlled increments
**Goal:** complete migration toward JDK25-level source/target with bounded blast radius.

#### File-level edits (recommended sequence)
1) `JavaGoldenAxeAnimationEditor2\nbproject\project.properties`
- First raise: `javac.release=8` -> `javac.release=17`
- Later raise: `javac.release=17` -> `javac.release=25`

2) `JavaGoldenAxe\nbproject\project.properties`
- First raise: `javac.release=16` -> `javac.release=17`
- Later raise: `javac.release=17` -> `javac.release=25`

3) Optional CI hardening in `.github\workflows\ant.yml`
- temporary matrix to de-risk transition:
  - `java-version: [17, 25]`
- after stability: keep only `25`.

**Rationale:** stepwise jumps make regressions attributable and easy to revert.

## NetBeans-generated file guidance
- Do **not** hand-edit `*\nbproject\build-impl.xml`; it is generated and can be overwritten.
- Keep customizations in:
  - `*\nbproject\project.properties`
  - `*\build.xml` (only if truly needed; currently no custom target changes required).

## Risk controls (config-level)
1) **One-stage-per-PR** (Stage 1, then Stage 2, then Stage 3).
2) **CI gate per stage**: both projects must compile with Ant before merge.
3) **Runtime smoke check** after each stage (launch both jars).
4) **No simultaneous structural edits** (no source refactors in same PR as config changes).
5) **Preserve prior CI definition** during rollout (branch protection + required checks).

## Rollback strategy
- If Stage 1 fails:
  - revert `.github\workflows\ant.yml` to `java-version: '17'` and prior single-project build command.
- If Stage 2 fails:
  - revert `javac.release` additions and restore previous `javac.source`/`javac.target` pairs.
- If Stage 3 fails:
  - roll back only the most recent `javac.release` increase (e.g., `25` -> `17`).
- Keep each stage isolated in its own commit so rollback is a single `git revert`.

## Final recommended target state
- CI uses JDK25 and builds **both** projects.
- Both projects compile with `javac.release=25`.
- `javac.source`/`javac.target` removed from both projects.
