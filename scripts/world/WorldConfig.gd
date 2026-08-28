extends Node
class_name WorldConfig
## Single source of truth for the Cinder Vale valley.
## Terrain height is a PURE function of world (x,z) so adjacent streamed
## tiles share edge vertices exactly — no seams. POI data drives blockouts,
## map markers and fast travel.
##
## Axis convention: -Z = north, +Z = south, +X = east, -X = west.

const TILE_SIZE := 400.0          # metres per tile edge
const TILES_PER_AXIS := 2         # 2x2 -> 800 m x 800 m region
const WORLD_HALF := TILE_SIZE * TILES_PER_AXIS * 0.5  # 400 m from centre
const QUAD := 4.0                 # terrain vertex spacing (m)
const AMPLITUDE := 4.0            # rolling hill height (m)
const BASE := 3.0                 # valley-floor elevation above waterline (m)
const RIVER_HALF_WIDTH := 22.0    # river channel half-width (m)

static var _noise: FastNoiseLite
static var _noise2: FastNoiseLite

# name, position (Vector2 = x,z), kind, faction, reveal_radius
const POIS := [
	{"id": "town",     "name": "Cinder Vale",     "pos": Vector2(0, 0),       "kind": "town",    "faction": "Vale Salvage"},
	{"id": "dam",      "name": "Cordon Dam",       "pos": Vector2(20, -330),   "kind": "dam",     "faction": "Red Cordon"},
	{"id": "quarry",   "name": "Ash Dogs Quarry",  "pos": Vector2(-30, 320),   "kind": "quarry",  "faction": "Ash Dogs"},
	{"id": "mill",     "name": "The Mill",         "pos": Vector2(325, 30),    "kind": "mill",    "faction": "Vale Salvage"},
	{"id": "farm",     "name": "West Ridge Farm",  "pos": Vector2(-320, -30),  "kind": "farm",    "faction": "Player"},
	{"id": "radio",    "name": "Radio Tower KVLE", "pos": Vector2(-210, 205),  "kind": "radio",   "faction": "none"},
	{"id": "convoy",   "name": "Wrecked Convoy",   "pos": Vector2(160, -170),  "kind": "convoy",  "faction": "none"},
	{"id": "clinic",   "name": "Roadside Clinic",  "pos": Vector2(-140, -250), "kind": "clinic",  "faction": "none"},
	{"id": "overpass", "name": "Overpass Camp",    "pos": Vector2(250, 210),   "kind": "camp",    "faction": "Ash Dogs"},
	{"id": "ford",     "name": "River Ford",       "pos": Vector2(35, 110),    "kind": "ford",    "faction": "none"},
]

const BUS_WRECK := Vector2(-45, 270)   # player spawn, on the highway

static func _ensure_noise() -> void:
	if _noise != null:
		return
	_noise = FastNoiseLite.new()
	_noise.noise_type = FastNoiseLite.TYPE_SIMPLEX_SMOOTH
	_noise.seed = 4471
	_noise.frequency = 0.0038
	_noise.fractal_octaves = 4
	_noise2 = FastNoiseLite.new()
	_noise2.noise_type = FastNoiseLite.TYPE_SIMPLEX
	_noise2.seed = 991
	_noise2.frequency = 0.02

static func height(x: float, z: float) -> float:
	_ensure_noise()
	# Rolling terrain rising toward the E/W ridges.
	var ridge := pow(absf(x) / WORLD_HALF, 2.0) * 26.0
	var h := BASE + _noise.get_noise_2d(x, z) * AMPLITUDE + ridge
	h += _noise2.get_noise_2d(x, z) * 1.2
	# River channel: a meandering N-S trench through the middle.
	var river_x := sin(z * 0.006) * 40.0 + 15.0
	var d := absf(x - river_x)
	if d < RIVER_HALF_WIDTH:
		var t := d / RIVER_HALF_WIDTH
		h -= (1.0 - t * t) * 6.5   # carve down toward the water
	# Flatten pads under POIs so blockouts sit level.
	for p in POIS:
		var pos: Vector2 = p["pos"]
		var pad := _pad_radius(p["kind"])
		var dd := Vector2(x, z).distance_to(pos)
		if dd < pad:
			var flat := _sample_flat(pos)
			var w := clampf((pad - dd) / (pad * 0.5), 0.0, 1.0)
			h = lerp(h, flat, w)
	return h

static func _sample_flat(pos: Vector2) -> float:
	# Raw rolling height at a POI centre, ignoring the flatten pass.
	_ensure_noise()
	var ridge := pow(absf(pos.x) / WORLD_HALF, 2.0) * 26.0
	return BASE + _noise.get_noise_2d(pos.x, pos.y) * AMPLITUDE + ridge

static func _pad_radius(kind: String) -> float:
	match kind:
		"town": return 70.0
		"dam", "quarry", "mill": return 45.0
		"farm", "camp": return 38.0
		_: return 24.0

static func normal(x: float, z: float) -> Vector3:
	var e := 1.5
	var hl := height(x - e, z)
	var hr := height(x + e, z)
	var hd := height(x, z - e)
	var hu := height(x, z + e)
	return Vector3(hl - hr, 2.0 * e, hd - hu).normalized()

static func spawn_transform() -> Transform3D:
	var p := BUS_WRECK
	var y := height(p.x, p.y) + 1.0
	return Transform3D(Basis(), Vector3(p.x, y, p.y))
