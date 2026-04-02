# Build/Run Baseline (current environment)

## Environment snapshot
- OS: Windows
- `java -version`: OpenJDK 1.8.0_432 (Temurin)
- `ant`: not installed / not on `PATH` (`ant` command not recognized)
- `.github\workflows\ant.yml` CI hint: uses JDK 17 and `ant -noinput -buildfile JavaGoldenAxe/build.xml`

## Project baselines

### JavaGoldenAxe
- Build config: NetBeans Ant project (`JavaGoldenAxe\build.xml`)
- Java target in `nbproject\project.properties`: `javac.source=16`, `javac.target=16`
- Canonical build command:
  - `ant -noinput -buildfile JavaGoldenAxe\build.xml`
- Canonical run command (after successful build):
  - `java -jar JavaGoldenAxe\dist\JavaGoldenAxe.jar`
- Current result:
  - Build blocked (Ant missing)
  - Run blocked (`dist\JavaGoldenAxe.jar` does not exist yet)

### JavaGoldenAxeAnimationEditor2
- Build config: NetBeans Ant project (`JavaGoldenAxeAnimationEditor2\build.xml`)
- Java target in `nbproject\project.properties`: `javac.source=1.8`, `javac.target=1.8`
- Canonical build command:
  - `ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml`
- Canonical run command (after successful build):
  - `java -jar JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar`
- Current result:
  - Build blocked (Ant missing)
  - Run blocked (`dist\JavaGoldenAxeAnimationEditor2.jar` does not exist yet)

## Baseline assumptions to make builds deterministic
- Install Apache Ant and ensure `ant` is on `PATH`.
- Use JDK 17+ for `JavaGoldenAxe` (source/target 16; CI uses 17).
- Keep `JAVA_HOME`/`PATH` stable and verify with `java -version` and `ant -version`.
