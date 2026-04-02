# CI Migration Plan: JDK17 -> JDK25 with both Ant projects

## Goal
Update GitHub Actions workflow `.github\workflows\ant.yml` to:
- use JDK 25
- build both projects in CI:
  - `JavaGoldenAxe`
  - `JavaGoldenAxeAnimationEditor2`

## Current baseline (observed)
- Workflow currently uses `actions/setup-java@v3` with `java-version: '17'`.
- Workflow currently builds only `JavaGoldenAxe`:
  - `ant -noinput -buildfile JavaGoldenAxe/build.xml`
- Both projects are NetBeans Ant projects with root `build.xml` files:
  - `JavaGoldenAxe\build.xml`
  - `JavaGoldenAxeAnimationEditor2\build.xml`
- Java levels in project configs:
  - `JavaGoldenAxe\nbproject\project.properties`: `javac.source/target=16`
  - `JavaGoldenAxeAnimationEditor2\nbproject\project.properties`: `javac.source/target=1.8`

## Proposed workflow changes (exact)
Apply these changes in `.github\workflows\ant.yml`:

1) Keep a single `build` job on `ubuntu-latest`.

2) Upgrade Java setup step:
- from `actions/setup-java@v3` to `actions/setup-java@v4`
- from `java-version: '17'` to `java-version: '25'`
- keep distribution `microsoft` (or switch to `temurin` if org standard requires it)
- add Ant dependency cache via setup-java:
  - `cache: 'ant'`

3) Replace single build command with deterministic two-project sequence (separate steps):
- Build game project from project directory
- Build animation editor project from project directory

4) Add artifact existence checks after each build for explicit failure messages.

5) (Optional transition hardening) temporary matrix:
- `strategy.matrix.java: [17, 25]` during migration PR validation
- after stable results, remove 17 and keep only 25

## Proposed YAML shape
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    # Optional temporary matrix:
    # strategy:
    #   fail-fast: false
    #   matrix:
    #     java: [17, 25]
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: microsoft
          java-version: '25'
          cache: ant
          # Optional matrix variant:
          # java-version: ${{ matrix.java }}

      - name: Build JavaGoldenAxe
        working-directory: JavaGoldenAxe
        run: ant -noinput clean jar

      - name: Verify JavaGoldenAxe artifact
        run: test -f JavaGoldenAxe/dist/JavaGoldenAxe.jar

      - name: Build JavaGoldenAxeAnimationEditor2
        working-directory: JavaGoldenAxeAnimationEditor2
        run: ant -noinput clean jar

      - name: Verify JavaGoldenAxeAnimationEditor2 artifact
        run: test -f JavaGoldenAxeAnimationEditor2/dist/JavaGoldenAxeAnimationEditor2.jar
```

## Deterministic CI command sequence (both projects)
From repo root in CI:

1. `cd JavaGoldenAxe`
2. `ant -noinput clean jar`
3. `cd ..`
4. `test -f JavaGoldenAxe/dist/JavaGoldenAxe.jar`
5. `cd JavaGoldenAxeAnimationEditor2`
6. `ant -noinput clean jar`
7. `cd ..`
8. `test -f JavaGoldenAxeAnimationEditor2/dist/JavaGoldenAxeAnimationEditor2.jar`

Why this sequence:
- `clean jar` avoids stale classes and gives reproducible artifact checks.
- Separate project dirs avoid path separator and shell differences.

## Failure triage guide (repo-specific)
When CI fails, classify quickly:

1) **JDK/toolchain setup failures**
- Symptoms: setup-java errors, Java not found, unsupported distribution/version.
- Check: setup-java step values (`distribution`, `java-version`, action version).

2) **Ant missing / invocation errors**
- Symptoms: `ant: command not found`, target not found.
- Check: runner image tools, command form, working-directory values.

3) **Project-level compile failures**
- `JavaGoldenAxe` likely tied to Java 16 source assumptions.
- `JavaGoldenAxeAnimationEditor2` likely tied to legacy 1.8 settings.
- Check first compiler error in Ant logs; confirm no accidental config edits under `nbproject`.

4) **Resource/layout issues**
- Symptoms: jar step fails or runtime packaging missing expected resources.
- Check generated `dist\` outputs and project resource paths:
  - `JavaGoldenAxe\assets\...`
  - `JavaGoldenAxeAnimationEditor2\res\...`

5) **Artifact verification failures**
- Symptoms: build step passes but `test -f ...jar` fails.
- Check actual jar name/location emitted by NetBeans Ant targets in `dist\`.

## Rollout recommendation
1. First PR: move workflow to JDK25 + add second project build steps.
2. Optional short-lived matrix PR (`17` + `25`) if risk appetite is low.
3. After green runs on main, keep single JDK25 lane.

## Notes from this planning task
- This task intentionally did not edit `.github\workflows\ant.yml`.
- Local command validation in this environment is limited because `ant` is not installed in the current shell.
