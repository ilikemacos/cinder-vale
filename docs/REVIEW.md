# Cinder Vale — review notes

**28 Aug 2026.** From `main` at `1f9dc41` (Zombies + reptile predators + LMB fire), plus the Godot tree still sitting at repo root. I have not played the packaged `.app` this pass. This is a source-and-commit read.

Cinder Vale is meant to be an original-IP scavver RPG in a ruined Pacific Northwest mill valley, ~15 years after a limited exchange. That pitch is still the best idea in the project. The repo currently contains two games. Neither is that pitch yet.

---

## Verdict

You spent 28 Aug porting a graybox into jMonkeyEngine and chasing a Fallout 4 roadside. That part worked. Dry-ground PBR, an asphalt ribbon, wrecked sedans, brick fragments, rad barrels with a green leak, hundreds of Poly Haven props. For the first time the dirt looks like dirt.

It also stopped being Cinder Vale. The map shrank from 800 m to 200 m. The mill, dam, quarry, KVLE, Ash Dogs, HearthLink, Mixamo, compass, VITALS, ammo, third person, and the mill-valley pitch did not come with you. What came with you is a Commonwealth screenshot: golden-hour sun, amber fog, rusted cars on a highway, glowing barrels.

Godot still has the better *game*. JME has the better *dirt*. Right now you have neither, twice.

---

## What shipped (JVM / jMonkey)

Commits `c44ebd5` → `1f9dc41`. Engine is JME 3 on JDK 21, OpenGL 4.1-via-Metal, Swing launcher via `jpackage`.

### Environment

**Good**

- `dry_ground_rocks` PBR on a 200×200 m heightmesh. The ground finally has grit.
- Road ribbon in `asphalt_02`. Reads as a highway, not a vertex-color smear.
- 14 procedural wrecked sedans (open hood, one flat tire) along the corridor. Correct silhouette.
- 18 leaning brick / moss-brick fragments. Brick UVs stay brick-sized.
- 3 rad barrels with a tight green `PointLight`. Cheap and readable.
- Seeded scatter (~520 Poly Haven props, 160 dead trees). Layout is stable across restarts.
- Cylinder collision so you stop walking through barrels.
- Fog at 320 m, density 0.9. Better than the Godot 160 m melt.

**Not good**

- The commit is titled "Push wasteland toward Fallout 4 look." That is the identity leak, written down. Sun is `1.10 / 0.96 / 0.80`, ambient is amber, fog is amber. That is the Commonwealth at 10 a.m., not a PNW mill 15 years after a limited exchange.
- No water. The river is a 1.4 m dry carve. The pitch needs a mill race, not a ditch.
- No POIs. No town, dam, mill stack, radio mast, quarry hole. Nothing you can name from 200 m.
- No power lines. Those were the strongest Godot read and they did not get ported.
- 2.7 M tris / 381 objects for 200 m, ~51 fps at 720p on M1. Heavy for a cell this small, and there is no streaming. The launcher still offers "World streaming: Low / High" on a mesh that is not streamed.

Do this before another FO4 prop: kill the key light, go overcast gray-green, put a real river in, and give every remaining landmark one silhouette (dam wall, mill stack, radio mast, town tower).

### Combat

- Box-zombies in Leather008/026, shambling gait. Box-lizard with horns; comments promise it is not a Deathclaw. Designing around Fallout IP anxiety instead of Cinder Vale.
- 6 zombies + 3 predators. One zombie planted 12 m in front of spawn so you cannot miss that combat exists.
- LMB raycasts a sphere with a 2.5× radius fudge. 28 damage. Hit confirmation is `System.out.println`.
- Player HP is a float that only lives in the console. Death does not exist in the UI.
- The rifle is a world prop on the road. The javadoc still calls it a viewmodel. It is not equipped. No mag, no reload, no muzzle flash, no reticle.

Ash Dogs, irradiated dogs, Mixamo brute/swat, mag HUD, recoil: all still in `scripts/`, unused.

### UI

- **In-world HUD:** gone. No compass, no VITALS, no `30 | 120`, no HearthLink bracer. That was the 27 Aug Godot pass (`7223ecd`) and it did not travel.
- **Pause:** JME `Default.fnt` in a dim rectangle. RESUME works. SETTINGS prints to stdout. QUIT TO LAUNCHER is `System.exit(2)`.
- **Launcher:** Swing, documented as Lunar Client. Amber PLAY, HOME / SETTINGS / ABOUT, dispatches copy. Native-resolution auto-detect (clamped to 2560×1440) is the one launcher idea worth keeping. Oswald + Share Tech Mono are loaded from `../godot-reference/assets/ui/fonts/`, which does not exist. Fonts still live at `assets/ui/fonts/`. Packaged `Cinder Vale Launcher.app` is ~322 MB with a full JDK.

You walked away from the Lunar look in Godot and then rebuilt it in Swing.

---

## What is still in Godot (and unused)

`project.godot`, `scripts/`, `scenes/`, Mixamo-baked `assets/characters/`. Last Godot commit: `7223ecd`.

That build had the actual game layer:

- 800×800 m streamed 2×2 tiles, 10 named POIs, HearthLink discovery + fast travel
- Mixamo gas-mask player, first/third person, full-auto rifle with mag / reserve / recoil
- Ash Dogs raiders + procedural irradiated dogs
- Drawn HUD: compass with POI and hostile ticks, VITALS pips, Oswald ammo, bracket reticle, objective fade after 20 m, legend fade after 10 s
- HearthLink as a cut-corner bracer (bug: `_player` never assigned from `player_path`, so the blip is missing and click-to-travel will nil)
- Terminal-styled launcher (amber vs phosphor-green still fighting)

Environment in Godot was the Mojave problem: tan dunes, cactus-looking shrubs, water as a tinted plane, cardboard mountains, fog at 160 m. UI got ahead of the world. Then the world got rebuilt in JME as a smaller FO4 cell, and the UI was left behind.

---

## Repo hygiene

- **README.md is a lie.** It still describes Godot 4.4 / Metal Forward+ / 800 m / 10 POIs / Mixamo. The thing that boots is JME.
- **ARCHITECTURE.md** still treats "custom Cinder Engine vs JME" as an open Phase-2 call. `jvm/` already *is* the game.
- Planned layout (`godot-reference/`, `launcher-jvm/`, `launcher-swift/`) is not the layout on disk. Godot files sit at repo root next to `jvm/`.
- Env art is gitignored. A clone of `github.com/ilikemacos/cinder-vale` cannot run the JME scene.
- `docs/PERFORMANCE.md` is promised and missing. `docs/` is mostly import logs plus this architecture doc.
- Streaming setting is vestigial.

Pick one tree. If JVM is real, move Godot under `godot-reference/`, rewrite the README, vendor or LFS the env pack, and stop promising a custom GL engine.

---

## Priority if the JVM path is the game

1. Stop adding FO4 roadside props.
2. Port the HearthLink HUD (compass, VITALS, ammo, bracer map) before another zombie. NanoVG is already named in the architecture doc. Use it. Kill stdout combat.
3. Put the mill and a wet river back. Overcast, not golden hour.
4. Equip the rifle or stop calling it a viewmodel. Mag, reload, a reticle.
5. Fix the launcher font path. Drop Lunar from the comments.
6. Update README so it describes the binary that actually launches.
7. One repo story: JME *or* Godot, not both as peers at root.

The 15-year limited exchange, living memory, Vale Salvage / Ash Dogs / Red Cordon, Radio Tower KVLE, mill turbine / exciter coil: that is still the game. The dirt is finally good enough to put it on.
