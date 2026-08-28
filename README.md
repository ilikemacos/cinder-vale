# Cinder Vale

An original open-world survival RPG set in a ruined Pacific Northwest mill
valley, ~15 years after a limited nuclear exchange. Built in **Godot 4.4** and
targeting **Apple Silicon / Metal (Forward+)**.

Original IP — not affiliated with any existing franchise.

## Features so far

- **Streamed open valley** — an 800 m × 800 m region built from a pure
  height-function, streamed in 400 m tiles. Rolling dunes, a carved river,
  distant mountain backdrop, utility poles + wires, dead brush, wrecked cars.
- **10 points of interest** — town, dam, quarry, mill, farm, radio tower,
  wrecked convoy, clinic, overpass camp, river ford — each with a map marker.
- **HearthLink map** (Tab) with discovery + fast travel.
- **Third / first person** (V), Mixamo locomotion, WASD + sprint + crouch + jump.
- **Gunplay** — held assault rifle, full-auto (LMB), reload (R), recoil,
  muzzle flash, 30-round mag + reserve, ammo HUD.
- **Enemies** — Ash Dogs raiders (melee + ranged) and procedural irradiated
  dogs, with chase/attack AI. Player health, damage flash, death.
- **Launcher** — front-end menu with resolution (720p–4K), memory budget,
  render scale, renderer, and fullscreen settings.

## Controls

`WASD` move · `LMB` fire · `R` reload · `Shift` sprint · `C` crouch ·
`Space` jump · `V` camera · `Tab` map · `Esc` free mouse

## Setup

Characters and animations come from [Mixamo](https://www.mixamo.com/) and are
**not** committed (licensing + file size). To build the character scenes:

1. Download the Mixamo FBX set into `assets_src/mixamo_assets/`
   (`characters/` + `animations/` — see the in-repo `docs/`).
2. Open the project in Godot 4.4 once to import the FBX.
3. Run the assembly tool:
   ```
   /path/to/Godot --headless --path . --script res://scripts/tools/build_character.gd
   ```
   This bakes `assets/characters/*.tscn` with a shared animation library.

## Run

Open in Godot 4.4+ and press Play, or:

```
/path/to/Godot --path . --resolution 1280x720
```

The game boots to the launcher; press **PLAY** to enter the world.

## Performance

Tuned for M1 / 8 GB: one adjacent tile streamed, 2K textures, single
directional light, procedural low-poly props. See `docs/PERFORMANCE.md`.
