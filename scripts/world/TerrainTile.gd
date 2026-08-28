extends Node3D
class_name TerrainTile
## Builds one 400 m terrain tile at grid coords (tx,tz) by sampling
## WorldConfig.height. Generates an ArrayMesh with normals + vertex colours
## (moss in the low/wet ground, rock/gravel on the ridges) and a trimesh
## collider. Purely procedural — no external textures required.

var tx := 0
var tz := 0

func setup(_tx: int, _tz: int) -> void:
	tx = _tx
	tz = _tz

func _ready() -> void:
	_build()

func _build() -> void:
	var origin_x := (tx - (WorldConfig.TILES_PER_AXIS / 2.0)) * WorldConfig.TILE_SIZE
	var origin_z := (tz - (WorldConfig.TILES_PER_AXIS / 2.0)) * WorldConfig.TILE_SIZE
	position = Vector3(origin_x, 0, origin_z)

	var q := WorldConfig.QUAD
	var n := int(WorldConfig.TILE_SIZE / q)  # quads per edge

	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)

	for iz in range(n):
		for ix in range(n):
			var x0 := origin_x + ix * q
			var x1 := x0 + q
			var z0 := origin_z + iz * q
			var z1 := z0 + q
			_emit_quad(st, x0, z0, x1, z1, origin_x, origin_z)

	st.generate_normals()
	var mesh := st.commit()

	var mi := MeshInstance3D.new()
	mi.mesh = mesh
	mi.material_override = _terrain_material()
	mi.gi_mode = GeometryInstance3D.GI_MODE_DISABLED
	add_child(mi)

	var body := StaticBody3D.new()
	body.collision_layer = 1
	add_child(body)
	var col := CollisionShape3D.new()
	var shape := mesh.create_trimesh_shape()
	col.shape = shape
	body.add_child(col)

	WorldScatter.populate(self, origin_x, origin_z, tx, tz)

func _emit_quad(st: SurfaceTool, x0: float, z0: float, x1: float, z1: float, ox: float, oz: float) -> void:
	var p00 := _v(x0, z0, ox, oz)
	var p10 := _v(x1, z0, ox, oz)
	var p11 := _v(x1, z1, ox, oz)
	var p01 := _v(x0, z1, ox, oz)
	_tri(st, p00, p10, p11, x0, z0)
	_tri(st, p00, p11, p01, x0, z0)

func _v(wx: float, wz: float, ox: float, oz: float) -> Vector3:
	# Local-space vertex (tile position already offsets to world).
	return Vector3(wx - ox, WorldConfig.height(wx, wz), wz - oz)

func _tri(st: SurfaceTool, a: Vector3, b: Vector3, c: Vector3, wx: float, wz: float) -> void:
	for p: Vector3 in [a, b, c]:
		var world_y := p.y
		var nrm := WorldConfig.normal(wx, wz)
		var slope := 1.0 - clampf(nrm.y, 0.0, 1.0)
		# Moss/dirt when low & flat, gravel-grey on slopes & ridges.
		var low := clampf((5.0 - world_y) / 9.0, 0.0, 1.0)
		# Damp near the river: darker, mossier the lower the ground.
		# Values are LINEAR (vertex colour) — keep them low for dark damp earth.
		# Sun-bleached dry wasteland palette (linear).
		var moss := Color(0.16, 0.145, 0.075)    # dead yellow grass
		var dirt := Color(0.21, 0.175, 0.115)    # tan sandy dirt
		var rock := Color(0.17, 0.155, 0.135)    # dry grey-tan rock
		var mud := Color(0.09, 0.08, 0.06)       # damp mud near river
		var ash := Color(0.20, 0.185, 0.15)      # pale dust patches
		var base := dirt.lerp(moss, low)
		base = base.lerp(mud, clampf((2.0 - world_y) / 4.0, 0.0, 0.6))  # wet mudline
		base = base.lerp(rock, clampf(slope * 2.4, 0.0, 1.0))
		# Scattered ash patches driven by cheap hash noise.
		var ashv := sin(wx * 0.13) * cos(wz * 0.11)
		base = base.lerp(ash, clampf(ashv * 0.5 + 0.15, 0.0, 0.5))
		st.set_color(base)
		st.add_vertex(p)

func _terrain_material() -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.vertex_color_use_as_albedo = true
	m.roughness = 0.95
	m.metallic = 0.0
	m.albedo_color = Color(1, 1, 1)
	return m
