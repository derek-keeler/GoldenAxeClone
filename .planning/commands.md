# Command Baseline (Windows)

Run from repo root: `C:\dev\GoldenAxeClone-Copilot`

## Environment checks
```powershell
java -version
ant -version
```

## Build commands
```powershell
ant -noinput -buildfile JavaGoldenAxe\build.xml
ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml
```

## Run commands (after build)
```powershell
java -jar JavaGoldenAxe\dist\JavaGoldenAxe.jar
java -jar JavaGoldenAxeAnimationEditor2\dist\JavaGoldenAxeAnimationEditor2.jar
```

## Current status in this environment
- `java -version`: works (OpenJDK 1.8.0_432)
- `ant -version`: fails (`ant` not recognized)
- Both build commands: blocked by missing Ant
- Both run commands: blocked because target JARs are not yet produced

## CI hint
- `.github\workflows\ant.yml` builds `JavaGoldenAxe` with:
  - JDK 17
  - `ant -noinput -buildfile JavaGoldenAxe/build.xml`
