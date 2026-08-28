extends Node3D
class_name POIBlockout
## Procedural blockout for a point of interest: a cluster of primitive
## structures keyed by "kind", a floating name label, and an Area3D that
## reveals the map marker + records discovery for fast travel.
## Placeholder geometry — swap for Kenney/Quaternius kits later.

var data := {}

const MAT_CONCRETE := Color(0.56, 0.56, 0.54)
const MAT_RUST := Color(0.52, 0.31, 0.19)
const MAT_WOOD := Color(0.44, 0.33, 0.21)
const MAT_METAL := Color(0.40, 0.42, 0.45)

func setup(_data: Dictionary) -> void:
	data = _data

func _ready() -> void:
	var pos: Vector2 = data["pos"]
	var y := WorldConfig.height(pos.x, pos.y)
	global_position = Vector3(pos.x, y, pos.y)
	_build()
	_add_label()
	_add_reveal()
	_spawn_enemies()

func _spawn_enemies() -> void:
	var kind: String = data["kind"]
	var rng := RandomNumberGenerator.new()
	rng.seed = hash(data["id"])
	# Ash Dogs hold the quarry and the overpass camp — hostile raiders.
	var raider_counts := {"quarry": 3, "camp": 2, "convoy": 1}
	if raider_counts.has(kind):
		for i in int(raider_counts[kind]):
			var ranged: bool = rng.randf() < 0.5
			_place(EnemyFactory.build(ranged), rng, i, int(raider_counts[kind]))
	# Feral irradiated dogs at the clinic (and a small pack near the quarry).
	var dog_counts := {"clinic": 3, "quarry": 2, "ford": 2}
	if dog_counts.has(kind):
		for i in int(dog_counts[kind]):
			var dog := CharacterBody3D.new()
			dog.set_script(load("res://scripts/characters/Dog.gd"))
			_place(dog, rng, i + 7, int(dog_counts[kind]) + 7)

func _place(e: Node3D, rng: RandomNumberGenerator, i: int, n: int) -> void:
	var a := TAU * i / float(n)
	var r := rng.randf_range(8.0, 20.0)
	var lx := cos(a) * r
	var lz := sin(a) * r
	var wx := global_position.x + lx
	var wz := global_position.z + lz
	e.position = Vector3(lx, WorldConfig.height(wx, wz) - global_position.y + 1.0, lz)
	add_child(e)

func _box(size: Vector3, offset: Vector3, col: Color) -> void:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = 0.9
	mi.material_override = m
	mi.position = offset + Vector3(0, size.y * 0.5, 0)
	add_child(mi)
	# Cheap collision so blockouts are solid.
	var body := StaticBody3D.new()
	var col_shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = size
	col_shape.shape = box
	col_shape.position = mi.position
	body.add_child(col_shape)
	add_child(body)

func _cyl(radius: float, h: float, offset: Vector3, col: Color) -> void:
	var mi := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = radius
	cm.bottom_radius = radius
	cm.height = h
	mi.mesh = cm
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = 0.85
	mi.material_override = m
	mi.position = offset + Vector3(0, h * 0.5, 0)
	add_child(mi)

func _build() -> void:
	match data["kind"]:
		"town":
			# Ring of low buildings + a central water tower.
			var rng := RandomNumberGenerator.new()
			rng.seed = 1234
			for i in 12:
				var a := TAU * i / 12.0
				var r := 32.0 + rng.randf_range(-6, 6)
				var p := Vector3(cos(a) * r, 0, sin(a) * r)
				var hgt := rng.randf_range(4, 8)
				_box(Vector3(rng.randf_range(6, 10), hgt, rng.randf_range(6, 10)), p, MAT_CONCRETE if i % 2 else MAT_RUST)
			_cyl(4, 14, Vector3(0, 0, 0), MAT_METAL)
			_box(Vector3(9, 9, 9), Vector3(0, 14, 0), MAT_RUST)  # tank on legs
		"dam":
			_box(Vector3(90, 22, 10), Vector3(0, 0, 0), MAT_CONCRETE)
			_box(Vector3(14, 8, 14), Vector3(-30, 22, 0), MAT_METAL)  # control house
			_box(Vector3(14, 8, 14), Vector3(30, 22, 0), MAT_METAL)
		"quarry":
			for i in 5:
				_box(Vector3(20, 4 + i * 2, 20), Vector3(0, -2.0 * i, 0), Color(0.4, 0.38, 0.34))  # terraced pit
			_box(Vector3(8, 10, 4), Vector3(24, 0, 10), MAT_RUST)  # crusher
		"mill":
			_box(Vector3(30, 16, 20), Vector3(0, 0, 0), MAT_RUST)
			_cyl(3, 24, Vector3(16, 0, -6), MAT_METAL)  # smokestack
			_box(Vector3(10, 6, 10), Vector3(-18, 0, 8), MAT_CONCRETE)  # turbine house
		"farm":
			_box(Vector3(14, 7, 20), Vector3(0, 0, 0), MAT_WOOD)  # barn
			_box(Vector3(8, 5, 8), Vector3(16, 0, 6), MAT_WOOD)   # house ruin
			for i in 4:
				_box(Vector3(6, 0.4, 6), Vector3(-14 + i * 5, 0, -14), Color(0.28, 0.24, 0.15))  # crop beds
		"radio":
			_cyl(1.2, 40, Vector3.ZERO, MAT_METAL)  # tower mast
			_box(Vector3(6, 4, 6), Vector3(6, 0, 0), MAT_CONCRETE)  # shack
		"convoy":
			for i in 4:
				_box(Vector3(4, 3, 8), Vector3(i * 6 - 9, 0, i * 1.5), MAT_RUST)  # trucks
		"clinic":
			_box(Vector3(16, 6, 12), Vector3.ZERO, Color(0.5, 0.5, 0.48))
			_box(Vector3(2, 4, 0.5), Vector3(0, 6, 0), Color(0.7, 0.15, 0.15))  # red cross post
			_box(Vector3(0.5, 4, 2), Vector3(0, 6, 0), Color(0.7, 0.15, 0.15))
		"camp":
			for i in 5:
				var a := TAU * i / 5.0
				_box(Vector3(4, 3, 4), Vector3(cos(a) * 8, 0, sin(a) * 8), MAT_WOOD)  # shanties
			_box(Vector3(40, 6, 8), Vector3(0, 10, 0), MAT_CONCRETE)  # overpass slab
			_cyl(2, 10, Vector3(-14, 0, 0), MAT_CONCRETE)  # pillar
			_cyl(2, 10, Vector3(14, 0, 0), MAT_CONCRETE)
		"ford":
			for i in 6:
				_box(Vector3(3, 1, 3), Vector3(i * 4 - 12, -0.5, sin(i) * 3), Color(0.5, 0.5, 0.5))  # stepping stones
		_:
			_box(Vector3(4, 4, 4), Vector3.ZERO, MAT_CONCRETE)

func _add_label() -> void:
	var l := Label3D.new()
	l.text = data["name"]
	l.font_size = 64
	l.pixel_size = 0.02
	l.billboard = BaseMaterial3D.BILLBOARD_ENABLED
	l.modulate = Color(0.95, 0.75, 0.35)  # HearthLink amber
	l.outline_modulate = Color(0, 0, 0, 0.8)
	l.no_depth_test = false
	l.position = Vector3(0, 18, 0)
	add_child(l)

func _add_reveal() -> void:
	var area := Area3D.new()
	area.collision_layer = 0
	area.collision_mask = 2   # player body reveal layer
	var cs := CollisionShape3D.new()
	var sph := SphereShape3D.new()
	sph.radius = 45.0
	cs.shape = sph
	area.add_child(cs)
	add_child(area)
	area.body_entered.connect(_on_reveal)

func _on_reveal(body: Node) -> void:
	if body.is_in_group("player"):
		WorldState.discover(data["id"])
