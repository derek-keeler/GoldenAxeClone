# JDK25 Easy Wins Shortlist (Performance, Low Risk)

## Scope
Post-upgrade, behavior-preserving optimizations only (game runtime + editor tools), with small/medium implementation effort.

## Hotspot surfaces considered
- Main game loop and frame rendering: `JavaGoldenAxe\src\infra\GameCanvas.java`
- Stage update/draw/collision path: `JavaGoldenAxe\src\scene\Stage.java`
- Enemy target strategy updates: `JavaGoldenAxe\src\infra\EnemiesManager.java`
- Resource loading/cache path: `JavaGoldenAxe\src\infra\Resource.java`
- Editor repaint loops:  
  - `JavaGoldenAxeAnimationEditor2\src\infra\SpriteSheetEditorView.java`  
  - `JavaGoldenAxeAnimationEditor2\src\infra\SpriteCenterEditorView.java`  
  - `JavaGoldenAxeAnimationEditor2\src\infra\ColliderEditorView.java`

## Easy-win candidates

### 1) Fix `Resource.getImage()` cache fast-path before stream open
- **Why low risk:** Pure cache-path refactor; keep same returned image and error handling.
- **Expected impact category:** Allocation/I/O reduction (startup + stage load), small-to-medium.
- **Acceptance criteria:**  
  - Cached calls no longer open resource streams.  
  - JFR/allocation profile shows fewer `InputStream`/`ImageIO` activity after first load.  
  - No visual/regression differences in stage/character art.
- **Complexity:** **small**

### 2) Remove redundant `Collections.sort(actors)` in `Stage.fixedUpdate()`
- **Why low risk:** Keep draw-time sort intact (`Stage.draw()`), preserving render order while avoiding duplicate per-frame sort.
- **Expected impact category:** CPU frame-time reduction (runtime), small.
- **Acceptance criteria:**  
  - One sort remains in draw path; update path no longer sorts actors.  
  - 60-second gameplay sample shows equal or lower CPU/frame time (same scene/enemy count).  
  - No gameplay/render ordering regressions.
- **Complexity:** **small**

### 3) Replace redundant `Collections.sort(SPOT_ORDER_2)` calls in `EnemiesManager`
- **Why low risk:** `SPOT_ORDER_2` is static `[0,1]`; sorting each call is a no-op.
- **Expected impact category:** CPU micro-optimization (AI update), small.
- **Acceptance criteria:**  
  - No `Collections.sort(SPOT_ORDER_2)` in hot update path.  
  - Enemy targeting behavior remains unchanged in manual play checks.
- **Complexity:** **small**

### 4) Convert editor frame pacing from `Thread.sleep()` in `paintComponent()` to Swing `Timer`
- **Why low risk:** Standard Swing pattern; keeps ~60 FPS target while avoiding EDT blocking.
- **Expected impact category:** Editor responsiveness/jank reduction, medium.
- **Acceptance criteria:**  
  - `paintComponent()` contains no sleep call.  
  - Timer-driven repaint/update keeps controls responsive during drag/edit.  
  - No functional changes to editor tools.
- **Complexity:** **medium**

### 5) Replace recursive flood-fill in sprite extraction with iterative stack/queue
- **Why low risk:** Algorithmic equivalent in editor-only path; preserves selected region output.
- **Expected impact category:** Editor stability/performance (large regions), medium.
- **Acceptance criteria:**  
  - Extracted sprite bounds match current behavior on sample sprites.  
  - No stack overflow on large connected areas.  
  - Equal or faster extraction time on large test sprite.
- **Complexity:** **medium**

## Defer list (not for upgrade phase)
- Collision-system redesign (broad-phase partitioning, spatial hashing, or parallel collision checks).
- Rendering pipeline changes (new backend, batching rewrites, major AWT/Swing architectural shifts).
- Large AI behavior refactors beyond micro-optimizations.
- Build system migration (Ant/NetBeans project structure changes).
- Aggressive JVM tuning changes as defaults (GC flags, experimental VM options) before baseline parity is proven.

## Suggested rollout order
1. `Resource.getImage()` fast-path
2. Remove duplicate sort in `Stage.fixedUpdate()`
3. Remove redundant `SPOT_ORDER_2` sort
4. Editor `Timer` repaint loop
5. Iterative flood-fill in editor
