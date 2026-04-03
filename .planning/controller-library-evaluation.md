# Controller library evaluation (Windows-first Xbox support)

Data gathered on 2026-04-03 from public repository metadata (`pushed_at`), README/license files, and host searches.

## OSS updated <= 1 year

Ranked shortlist for this project (Ant-based Java game, Windows-first Xbox controller support):

| library | host | latest update date | Windows Xbox support quality | Linux/macOS support | license | integration complexity (Ant project) | risks | recommendation |
|---|---|---|---|---|---|---|---|---|
| **1) [input4j](https://github.com/gurkenlabs/input4j)** | GitHub | 2026-04-01 | **High** (explicit XInput + DirectInput support) | Yes (evdev + IOKit) | MIT | **Medium** (Maven Central artifact; but requires Java 22+) | Requires modern JDK; migration impact if runtime/toolchain not upgraded | **Top choice** if project moves to modern JDK (22+ / planned JDK upgrade path). |
| **2) [derek-keeler/jinput](https://github.com/derek-keeler/jinput/tree/master)** (fork of jinput/jinput) | GitHub | 2026-04-03 | **Medium** (works, but older controller model/mapping ergonomics vs SDL/Input4j stacks) | Yes | BSD-style (per README) | **Low** (Maven artifacts and long Java ecosystem history; Ant-friendly) | Fork longevity risk; likely better to track upstream `jinput/jinput` unless fork-specific fixes are needed | **Good conservative fallback** for minimal integration friction. |
| **3) [gdx-controllers](https://github.com/libgdx/gdx-controllers)** | GitHub | 2025-06-08 | **Medium** (usable, but optimized for libGDX stack vs standalone Ant game core) | Yes (desktop/mobile targets) | Apache-2.0 | **High** (best in LibGDX-centric architecture; extra adaptation in this codebase) | Framework coupling risk; less direct fit for non-libGDX architecture | **Use only if adopting more libGDX controller stack patterns.** |

### Alternatives reviewed but excluded from `<= 1 year` shortlist

- **[Jamepad](https://github.com/williamahartman/Jamepad)**: repository `pushed_at` is 2020-04-12 (not within 1 year), despite README still describing solid Xbox/XInput support.
- **[sdl2gdx](https://github.com/electronstudio/sdl2gdx)**: repository `pushed_at` is 2024-10-01 (outside <= 1 year window from 2026-04-03).

### GitLab / SourceForge check

- GitLab public search for Java gamepad/controller libraries did **not** surface a clear, actively maintained Java controller library with strong Windows Xbox focus and `pushed_at <= 1 year` comparable to the GitHub options above.
- SourceForge results were mostly mirrors or unrelated/end-user projects; no clearly superior actively updated Java controller library was identified over the shortlist.

## Recommendation summary

1. **Primary path:** pilot **input4j** (best Xbox/Windows quality + modern cross-platform backend), if current upgrade plan accepts Java 22+.
2. **Fallback path:** use **jinput** (prefer upstream unless this fork has required fixes) for lower migration risk in existing Ant workflows.
3. Keep Jamepad/sdl2gdx as historical references, not primary candidates for “recently updated <= 1 year”.
