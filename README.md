# Cinder Vale

An original-IP scavver RPG set in a ruined Pacific Northwest mill valley,
~15 years after a limited nuclear exchange. Grounded, morally grey, and
resolutely not the Commonwealth.

**The game runs on jMonkeyEngine 3 / JDK 21**, rendering through Apple's
OpenGL-on-Metal on Apple Silicon. This repo currently contains two trees:

- `jvm/` — **the game.** Cinder Engine skeleton on JME + Poly Haven PBR + Swing
  launcher. This is what boots.
- `godot-reference/` — the earlier Godot 4.4 build. Kept as the design spec
  and asset source, not for shipping. See `godot-reference/README.md`.

## Status (Aug 28, 2026)

Honest state, per the review in [docs/REVIEW.md](docs/REVIEW.md):

**Working:**
- 200×200 m rolling heightmap terrain, PBR ground, procedural scatter (~520
  Poly Haven props), cracked-asphalt highway ribbon, distance fog.
- Physics: gravity + capsule clamp + prop collision (no walking through cars).
- Wrecked cars, brick ruins, rad barrels (green PointLight leak).
- Zombie + reptile-predator enemies (procedural mesh, PBR skins, chase-and-bite AI).
- LMB fires a raycast; enemies take damage and drop.
- External `Cinder Vale Launcher.app` packaged with `jpackage`, bundles a full
  JDK, spawns the game as a child process with the user's native screen resolution.

**Missing / regressed vs the Godot build:**
- In-world HUD (compass, VITALS pip, ammo readout, bracer HearthLink map). See
  Task #12.
- Named POIs (town, dam, mill, quarry, KVLE radio, ford, farm, camp, convoy,
  clinic). Currently one un-named cell.
- 800 m streamed world → collapsed to 200 m single mesh.
- Mixamo character rig / third-person view.
- Equipped rifle with mag / reserve / reload / muzzle flash.
- Grade currently reads "Fallout 4 golden hour" — being pulled back toward
  the actual pitch (damp overcast PNW) this pass.

## Run

Requires JDK 21+ on your `PATH`.

```bash
cd jvm
./gradlew runLauncher            # boots the Swing launcher
./gradlew run                    # boots straight into the game
```

To build the standalone `.app` (self-contained, bundles a JRE):

```bash
cd jvm
./gradlew jpackageApp
xcrun codesign --force --deep --sign - "build/dist/Cinder Vale Launcher.app"
open "build/dist/Cinder Vale Launcher.app"
```

## Controls

`WASD` walk · `Shift` sprint · `Space` jump · `LMB` fire · `Esc` pause

## Assets

Two art packs (CC0, bundled locally under `jvm/src/main/resources/assets/env/`
but `.gitignore`d because of size — download links in the pack READMEs):

- **cinder-vale-env-art** — Poly Haven HDRIs, glTF props, tileable PBR
  textures (asphalt, brick, ground, rubble, rusted metal, concrete).
- **cinder-vale-guns-cars-creatures** — Poly Haven + ambientCG PBR triplets
  used as skins for the procedural gun / wrecked cars / zombie hide / predator
  hide + keratin plates. Original silhouettes — not scanned Bethesda meshes.

Fonts: Google Fonts **Oswald** (titles) and **Share Tech Mono** (terminal /
HUD readouts). SIL Open Font License.

## Docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — engine architecture and phase plan.
- [docs/REVIEW.md](docs/REVIEW.md) — external review, Aug 28.

## Original IP

Not affiliated with any existing franchise. Vaults, Pip-Boys, Vault-Tec,
Fallout-branded factions and creature silhouettes are deliberately not used.
Cinder Vale is its own thing: Vale Salvage, Ash Dogs, Red Cordon, Radio
Tower KVLE, and the exciter-coil-in-the-mill-turbine spine.
