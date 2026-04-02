# Tooling prerequisites audit (JDK 17 baseline -> JDK 25 target)

## Scope and repo evidence

- CI uses JDK 17 + Ant for `JavaGoldenAxe` (`.github\workflows\ant.yml`).
- Both projects are NetBeans Ant projects (`nbproject\project.xml`, `build.xml`).
- `JavaGoldenAxe` compiles with `javac.source=16` / `javac.target=16` (`JavaGoldenAxe\nbproject\project.properties`).
- `JavaGoldenAxeAnimationEditor2` compiles with `javac.source=1.8` / `javac.target=1.8` (`JavaGoldenAxeAnimationEditor2\nbproject\project.properties`).
- Generated Ant script enforces `Ant >= 1.8.0` and requires `Ant >= 1.10.0` for Java 9+ support (`*\nbproject\build-impl.xml`).

## Required prerequisites (practical minimums)

### Baseline (current CI-compatible)

1. **JDK 17 (LTS), 64-bit**
   - Minimum: **17.0.x**
   - Why: current CI baseline and target runtime floor for upgrade work.
2. **Apache Ant**
   - Minimum for this repo: **1.10.x** (practical floor: 1.10.12+)
   - Why: `JavaGoldenAxe` is Java 16 source level; NetBeans-generated Ant logic requires 1.10+ for Java 9+ paths.
3. **Git**
   - Minimum practical: **2.30+** (for modern Actions/local workflows).

### Target lane (upgrade validation)

1. **JDK 25 (EA until GA, then 25.0.x), 64-bit**
   - Keep JDK 17 installed in parallel.
   - Use same vendor family as baseline where possible for consistent behavior.

## Recommended deterministic tool choices

Use pinned, scriptable tool installs and explicit version checks:

- **JDK vendor pinning**: choose one vendor family across lanes (e.g., Microsoft/OpenJDK as in CI, or Temurin) and keep it consistent.
- **CI setup pinning**: keep `actions/setup-java` config explicit (major + distribution), and print exact tool versions in logs.
- **Ant pinning**: install a fixed Ant 1.10.x release on developer machines/agents.
- **Environment pinning**: set `JAVA_HOME` deliberately per lane (17 vs 25), do not rely on ambient PATH order.

## Verification commands (Windows PowerShell)

### Core tools

```powershell
java -version
javac -version
ant -version
git --version
Get-Command java,javac,ant,git | Select-Object Name,Source
Write-Output $env:JAVA_HOME
```

### Known machine-specific Java paths (from user input)

- JDK8: `C:\Program Files\Eclipse Adoptium\jdk-8.0.432.6-hotspot\bin\java.exe`
- JDK17: `C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot\bin\java.exe`
- JDK25: `C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot\bin\java.exe`

Example lane switch (PowerShell, current session only):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;" + (($env:Path -split ';' | Where-Object {$_ -notmatch 'jdk-8.0.432.6-hotspot|jdk-17.0.18.8-hotspot|jdk-25.0.2.10-hotspot'}) -join ';')
java -version
javac -version
```

### Build checks (repo-grounded)

```powershell
ant -noinput -buildfile JavaGoldenAxe\build.xml
ant -noinput -buildfile JavaGoldenAxeAnimationEditor2\build.xml
```

### JDK lane switching sanity check

```powershell
# After selecting JDK 17:
java -version
javac -version

# After selecting JDK 25:
java -version
javac -version
```

## Ant compatibility guidance for this repo

- NetBeans-generated build files already enforce a minimum Ant version.
- Because one project is Java 16 source level, treat **Ant 1.10+ as mandatory** in practice.
- If Ant is missing, builds fail immediately (observed locally: `ant` command not found), so Ant installation is a hard prerequisite before any reproducible baseline/target validation.

## Optional but useful JDK tooling for upgrade work

These are optional for build, but recommended for JDK 17 -> 25 migration validation:

1. **`jdeps`** (dependency/module analysis)
   ```powershell
   jdeps --version
   jdeps -summary JavaGoldenAxe\dist\JavaGoldenAxe.jar
   ```
2. **`jdeprscan`** (deprecated API usage scan)
   ```powershell
   jdeprscan --version
   jdeprscan --release 25 JavaGoldenAxe\src
   ```
3. **JFR / `jcmd`** (runtime profiling/regression checks)
   ```powershell
   jcmd -l
   jcmd <pid> JFR.start name=GoldenAxe settings=profile duration=60s filename=goldenaxe.jfr
   ```

## Suggested acceptance checklist for tooling-prereqs

- [ ] JDK 17 and JDK 25 both installed and selectable.
- [ ] `java`, `javac`, `ant`, `git` version checks pass.
- [ ] Ant 1.10+ confirmed.
- [ ] Both Ant build commands execute successfully under JDK 17 baseline.
- [ ] Optional tools (`jdeps`, `jdeprscan`, `jcmd`) available for migration diagnostics.
