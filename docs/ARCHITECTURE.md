# Cinder Vale — Cinder Engine Architecture (refined)

Refinement of `Cinder Vale — JVM/OpenGL Engine Architecture.md`. The original is a
strong skeleton; this version corrects the platform assumptions, scopes the
feature list to what Cinder Vale v1 actually needs, and reconciles it with what is
already running on disk. **Read §0 first — it changes several downstream choices.**

---

## 0. What this refinement changes (read first)

1. **macOS is OpenGL 4.1 max, not 4.6.** Confirmed by our Phase-0 run:
   `OpenGL Version: 4.1 Metal - 90.5`. Apple froze GL at 4.1 and routes it through
   Metal. Consequences the original doc must drop or gate behind non-Mac builds:
   - **No compute shaders** (GL 4.3). SSAO/culling/particles must be done in
     vertex/fragment passes or on the CPU.
   - **No persistent-mapped buffers** (GL 4.4) — use `glBufferSubData` / orphaning.
   - **No bindless textures, no `ARB_gpu_shader5` niceties.** Bind conventionally.
   - GLSL is capped at `#version 410 core`.
   Design the renderer to **GL 4.1 core** as the floor; treat 4.3+ features as
   optional fast-paths only compiled in on Windows/Linux.

2. **Custom engine vs jMonkeyEngine — an open decision.** The doc describes a
   *from-scratch* engine. A **jMonkeyEngine window already runs today** (`jvm/`,
   renders the graybox on the M1). Writing the renderer/scene/animation/physics by
   hand is months of work JME already gives you. This doc is written for the custom
   path (per the doc's intent) but flags every place where JME would collapse weeks
   of work into a dependency. **Recommendation:** decide this explicitly before
   Phase 2 (see §2); it is the highest-leverage decision in the project.

3. **Renderer is forward, not deferred.** On an 8 GB M1 through GL-on-Metal, a fat
   G-buffer is the wrong trade. Use **single-pass forward (optionally clustered)**
   with a depth pre-pass. Matches the current Godot Forward+ choice and the memory
   budget the whole project was scoped around.

4. **Scope is pruned.** The original lists a full Bethesda-scale RPG (weather, NPC
   daily schedules, dialogue trees, factions, save system, world/dialogue editors,
   PBR + SSR + TAA + volumetric fog). Cinder Vale v1 is a **small dense valley**.
   Each system below is tagged **[v1]**, **[later]**, or **[cut for v1]**.

5. **Launchers are part of the deliverable.** The JVM launcher (primary,
   Lunar-style) and the SwiftUI macOS fallback launcher from the prior decision are
   folded in (§13), plus the preserved `godot-reference/`.

---

## 1. Target platform reality

- **Primary:** Apple M1, 8 GB unified, macOS. OpenGL **4.1 core** via Metal.
- Secondary (free with LWJGL, higher GL cap): Windows/Linux GL 4.6.
- Budgets (same discipline as the Godot build, logged in `docs/PERFORMANCE.md`):
  **≥30 fps @ 1080p on the M1**, RAM < 5 GB, one directional light + baked/simple
  shadows, ≤8 on-screen characters, streamed tiles.
- JVM: heap capped (`-Xmx2g`) + LWJGL native buffers; watch total against 8 GB.

## 2. Engine decision (resolve before Phase 2)

| | Custom "Cinder Engine" (this doc) | jMonkeyEngine (already running) |
|---|---|---|
| Renderer/scene/anim/physics | You write it all | Provided |
| Control / learning | Total | High-level, overridable |
| Time to feature-parity w/ current game | Months | Weeks |
| Risk on GL 4.1 / M1 | You own every bug | Battle-tested |

Both use the same substrate (LWJGL3, GLFW, OpenAL, JOML-or-jme-math). If the goal
is *the game*, JME wins. If the goal is *owning an engine*, custom wins — accept the
timeline. Everything below is structured so the **game layer is identical either
way** (§12 engine/game separation), so this decision only swaps the lower half.

## 3. Repository structure (reconciled with disk)

```
cinder-vale/
├── godot-reference/     # the shipped Godot game — design spec + asset source (keep)
├── jvm/                 # the JVM game (Gradle). Currently JME graybox.
│   ├── engine/          # Cinder Engine (if custom) — core/render/physics/audio/...
│   ├── game/            # Cinder Vale game layer (world/player/npc/combat/...)
│   └── assets/          # glTF models, textures, shaders, audio, fonts
├── launcher-jvm/        # Lunar-style Java launcher (primary)
├── launcher-swift/      # SwiftUI macOS fallback launcher
└── docs/                # ARCHITECTURE.md, PERFORMANCE.md
```

Keep the original doc's `engine/ | game/ | renderer/` split **inside `jvm/`**, not at
repo root, so the Godot reference and launchers sit alongside.

## 4. Concrete technology picks (filling the doc's gaps)

- **Math:** JOML (as specified). If custom; JME has its own math if not.
- **Windowing/GL/Audio:** LWJGL3 (GLFW, OpenGL, OpenAL) — already resolved with
  **macOS arm64 natives** in `jvm/build.gradle`.
- **glTF loading:** reuse `jme3-plugins` glTF loader even on the custom path, or
  `cgltf`-style hand loader. Do **not** write an FBX loader — convert
  `assets_src/mixamo_assets` FBX → glTF once (§11 of the port plan).
- **Physics:** **libbulletjme (Minie's native Bullet)** — there is no official Jolt
  Java binding, so Bullet is the pragmatic JVM pick. Isolate behind the
  `PhysicsWorld` API (doc §16) so Jolt-JNI can swap in later.
- **UI:** **NanoVG** (ships in LWJGL) for the HUD/menus — vector, fast, exactly
  suits the HearthLink amber/mono chrome. Avoids a bespoke UI renderer.
- **Text:** NanoVG font atlas with the existing **Oswald + Share Tech Mono** TTFs.

## 5. Engine core & main loop [v1]

Keep the doc's loop, with the fixed-timestep correction made explicit:

```
accumulator += dt
pollInput(); processEvents()
while accumulator >= FIXED (1/60): stepPhysics(FIXED); stepAI(FIXED); accumulator -= FIXED
updateGame(dt); updateAnimations(dt)
cull(); render(); renderUI(); swapBuffers()
```

Physics/AI on a fixed step; render interpolated. **All GL calls on the main
thread** (doc §32 is right — enforce it; workers only build CPU-side data).

## 6. ECS [v1, minimal]

Adopt the doc's component list but keep it **lightweight** (archetype or simple
component-array store). Cinder Vale has hundreds, not millions, of entities — do
not over-build the ECS. Components needed v1: Transform, Render, Physics/Collider,
CharacterController, Camera, Health, Weapon, Animation, AI, Interactable.
[cut for v1]: Inventory/Quest/Faction/Schedule components (see §9).

## 7. Renderer [v1] (GL 4.1 forward)

Passes, trimmed to 4.1-forward:

```
frustum + distance cull → depth pre-pass → forward opaque (sun + few point lights)
→ single-cascade or simple shadow map → sky → transparent → HUD (NanoVG)
```

- **[v1]** VAO/VBO meshes, instanced scatter (poles/trees/rocks/cars — port the
  MultiMesh idea from `godot-reference/scripts/world/WorldScatter.gd`), one
  DirectionalLight, ≤2 point lights, a single shadow map, exp/height fog in-shader,
  Filmic tonemap + the wasteland colour grade.
- **[later]** clustered lighting, cascaded shadows, PBR maps, bloom.
- **[cut for v1]** SSR, TAA, volumetric fog, SSAO (needs compute → not on Mac 4.1),
  reflection/light probes. The current game shipped convincingly without any of
  these; do not gate v1 on them.

Shaders as assets with live reload in dev (doc §9 — keep, good).

## 8. World, terrain, streaming [v1]

Port directly from the reference — this is the cleanest carry-over:

- **`godot-reference/scripts/world/WorldConfig.gd`** — the pure height function,
  POI table, and bus-wreck spawn become a Java `WorldConfig`. Deterministic, so
  tiles seam exactly (same property the Godot version relies on).
- 400 m tiles, active + edge neighbours resident, diagonal unloaded (the
  `WorldStreamer.gd` policy). Terrain chunk = generated mesh + heightfield
  collider. LOD by ring (doc §13) is **[later]**; v1 ships single-LOD tiles.

## 9. Gameplay systems — scoped honestly

- **[v1]** Player controller (first/third, WASD, sprint/crouch/jump), gunplay
  (ray, ammo, reload, recoil), raider + irradiated-dog AI (chase/attack/death),
  HearthLink map + fast travel, compass HUD, the main coil→mill quest spine.
  All exist as GDScript in `godot-reference/` and port 1:1 in behaviour.
- **[later]** Inventory/loot, junk crafting + workbench, the farm settlement,
  companion Maren, faction reputation.
- **[cut for v1]** Dialogue trees, NPC daily schedules, weather system, day/night
  time system, world events, save-slot system, in-engine editors. These are the
  doc's Bethesda-scale ambitions; none are in Cinder Vale's acceptance list. Add a
  minimal JSON **save** only when there is progress worth saving.

## 10. AI, navigation [v1 minimal]

v1 AI is the current steering model (detect → face → chase → attack), no NavMesh —
the valley is open. **[later]** recast/detour NavMesh + A* (doc §19) when interiors
and cover matter. Behavior trees + schedules are **[cut for v1]**.

## 11. Asset pipeline [v1]

`AssetManager` with caching (doc §14, keep). One-time **FBX→glTF** conversion of the
Mixamo set; animation via the engine's `AnimComposer`/skeleton. Textures: the
current build is largely procedural/vertex-coloured, so v1 needs few image
textures — keep it that way to protect the 8 GB budget.

## 12. Engine/Game separation [v1, enforce day one]

Doc §36 is the most important principle — keep it verbatim. The `game/` layer talks
only to an `engine` API (Scene, Renderer, Physics, Audio, Input, ECS). Because the
game layer is engine-agnostic, the §2 JME-vs-custom decision does not touch it.
(The doc's "engine must not know about Vaults" is apt — and Cinder Vale has no
vault anyway.)

## 13. Launchers [v1]

- **`launcher-jvm/`** — the primary, Lunar-style Java launcher: news/dispatches,
  video + streaming settings, PLAY → spawns the game (same UX as the current
  in-game terminal launcher, now as the outer shell). Package the game as a
  runnable JAR; the launcher sets `-Xmx`, resolution, and GL options as args.
- **`launcher-swift/`** — SwiftUI macOS fallback launcher (built via the Xcode
  toolchain; note CLT SwiftPM is broken on this machine — compile through Xcode).
- Package with **`jpackage` + `jlink`** → `.app`/`.dmg` with a trimmed JRE bundled;
  ad-hoc codesign + de-quarantine, same flow that shipped the Godot `.app`.

## 14. Development order (revised, gated on the M1)

- **P0 ✅ Foundation** — JDK 21, Gradle, LWJGL3, window on the M1. *Done* (as JME;
  redo as raw-GL only if §2 chooses custom).
- **P1 Renderer floor** — shaders-as-assets, VAO/VBO, camera, textures, one light,
  depth test, forward opaque. Gate: textured lit mesh at ≥60 fps.
- **P2 World** — port `WorldConfig`, terrain tiles + streaming, frustum cull. Gate:
  walk bus→town, no hitch.
- **P3 Player** — controller, first/third camera, capsule physics, animation.
- **P4 Gunplay + HUD** — rifle, NanoVG HUD (compass/health/ammo), fonts.
- **P5 Enemies + dressing** — raider/dog AI, instanced scatter, poles/cars/mountains.
- **P6 HearthLink + grade** — bracer map, fast travel, fog/palette/sky.
- **P7 Launchers + package** — JVM launcher, Swift fallback, jpackage `.app`/`.dmg`.
- **[later]** advanced rendering (§7), crafting/settlement/companion, save, editors.

Every phase must **run on the M1** and hold the fps budget before the next starts.

## 15. Honest bottom line

The refined doc is buildable, but a custom GL-4.1 engine to reach the current
game's polish is a **multi-month, incremental** effort — materially more than the
JME path, which already renders today. Nothing in `godot-reference/` is discarded;
it is the executable spec every phase is checked against. Resolve §2 first — it
decides whether P1 is "write a renderer" or "configure one."
```
