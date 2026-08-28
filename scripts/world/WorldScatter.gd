extends RefCounted
class_name WorldScatter
## Deterministic wasteland dressing for a terrain tile: dead trees, boulders,
## rubble and scattered debris via MultiMesh (one draw call each). Placement is
## seeded by tile coords so it's stable across streaming, and it avoids POI pads
## and the river channel. Swap the primitive meshes for Quaternius kits later.

static var _tree_mesh: ArrayMesh
static var _rock_mesh: ArrayMesh
static var _debris_mesh: ArrayMesh
static var _stump_mesh: ArrayMesh
static var _car_mesh: ArrayMesh

static func populate(parent: Node3D, ox: float, oz: float, tx: int, tz: int) -> void:
	var rng := RandomNumberGenerator.new()
	rng.seed = 90001 + tx * 733 + tz * 197
	_scatter(parent, ox, oz, rng, _dead_tree(), 60, 0.7, 1.6, Color(0.16, 0.13, 0.09))
	_scatter(parent, ox, oz, rng, _rock(), 80, 0.5, 2.4, Color(0.16, 0.15, 0.135))
	_scatter(parent, ox, oz, rng, _debris(), 130, 0.4, 1.4, Color(0.14, 0.125, 0.10))
	_scatter(parent, ox, oz, rng, _dry_shrub(), 240, 0.5, 1.5, Color(0.19, 0.165, 0.075))
	_scatter_cars(parent, ox, oz, rng, 8)

static func _scatter(parent: Node3D, ox: float, oz: float, rng: RandomNumberGenerator,
		mesh: Mesh, count: int, smin: float, smax: float, col: Color) -> void:
	var mm := MultiMesh.new()
	mm.transform_format = MultiMesh.TRANSFORM_3D
	mm.use_colors = true
	mm.mesh = mesh
	var xf := []
	var cols := []
	var size := WorldConfig.TILE_SIZE
	for i in count * 2:
		if xf.size() >= count:
			break
		var wx := ox + rng.randf() * size
		var wz := oz + rng.randf() * size
		if not _valid(wx, wz):
			continue
		var y := WorldConfig.height(wx, wz)
		var s := rng.randf_range(smin, smax)
		var b := Basis().rotated(Vector3.UP, rng.randf() * TAU).scaled(Vector3(s, s * rng.randf_range(0.8, 1.3), s))
		xf.append(Transform3D(b, Vector3(wx - ox, y, wz - oz)))
		var shade := rng.randf_range(0.8, 1.15)
		cols.append(Color(col.r * shade, col.g * shade, col.b * shade))
	mm.instance_count = xf.size()
	for i in xf.size():
		mm.set_instance_transform(i, xf[i])
		mm.set_instance_color(i, cols[i])
	var mmi := MultiMeshInstance3D.new()
	mmi.multimesh = mm
	var m := StandardMaterial3D.new()
	m.vertex_color_use_as_albedo = true
	m.roughness = 0.95
	mmi.material_override = m
	mmi.gi_mode = GeometryInstance3D.GI_MODE_DISABLED
	parent.add_child(mmi)

static func _scatter_cars(parent: Node3D, ox: float, oz: float, rng: RandomNumberGenerator, count: int) -> void:
	# Rusted wrecked cars — upright, yaw only, baked colours (rust/glass/tyres).
	var mm := MultiMesh.new()
	mm.transform_format = MultiMesh.TRANSFORM_3D
	mm.use_colors = true
	mm.mesh = _car()
	var xf := []
	var cols := []
	var size := WorldConfig.TILE_SIZE
	for i in count * 3:
		if xf.size() >= count:
			break
		var wx := ox + rng.randf() * size
		var wz := oz + rng.randf() * size
		if not _valid(wx, wz):
			continue
		# Avoid steep ground so hulks sit flat.
		if 1.0 - WorldConfig.normal(wx, wz).y > 0.14:
			continue
		var y := WorldConfig.height(wx, wz)
		var b := Basis().rotated(Vector3.UP, rng.randf() * TAU)
		xf.append(Transform3D(b, Vector3(wx - ox, y, wz - oz)))
		var s := rng.randf_range(0.85, 1.05)
		cols.append(Color(s, s, s))
	mm.instance_count = xf.size()
	for i in xf.size():
		mm.set_instance_transform(i, xf[i])
		mm.set_instance_color(i, cols[i])
	var mmi := MultiMeshInstance3D.new()
	mmi.multimesh = mm
	var m := StandardMaterial3D.new()
	m.vertex_color_use_as_albedo = true
	m.roughness = 0.85
	m.metallic = 0.3
	mmi.material_override = m
	parent.add_child(mmi)

static func _valid(wx: float, wz: float) -> bool:
	# Skip river channel and POI pads (keep the built areas clear).
	var river_x := sin(wz * 0.006) * 40.0 + 15.0
	if absf(wx - river_x) < WorldConfig.RIVER_HALF_WIDTH + 6.0:
		return false
	for p in WorldConfig.POIS:
		if Vector2(wx, wz).distance_to(p["pos"]) < 55.0:
			return false
	return true

# --- cached primitive meshes -------------------------------------------------

static func _dead_tree() -> ArrayMesh:
	if _tree_mesh: return _tree_mesh
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	_add_box(st, Vector3(0, 3.0, 0), Vector3(0.35, 6.0, 0.35))     # trunk
	_add_box(st, Vector3(0.9, 4.8, 0.2), Vector3(0.18, 2.4, 0.18)) # branch
	_add_box(st, Vector3(-0.7, 5.4, -0.3), Vector3(0.15, 2.0, 0.15))
	_add_box(st, Vector3(0.2, 6.2, 0.7), Vector3(0.14, 1.6, 0.14))
	st.generate_normals()
	_tree_mesh = st.commit()
	return _tree_mesh

static func _rock() -> ArrayMesh:
	if _rock_mesh: return _rock_mesh
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	_add_box(st, Vector3(0, 0.4, 0), Vector3(1.6, 0.9, 1.3))
	_add_box(st, Vector3(0.4, 0.9, 0.3), Vector3(0.9, 0.7, 0.8))
	st.generate_normals()
	_rock_mesh = st.commit()
	return _rock_mesh

static func _debris() -> ArrayMesh:
	if _debris_mesh: return _debris_mesh
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	_add_box(st, Vector3(0, 0.15, 0), Vector3(0.8, 0.3, 0.5))
	st.generate_normals()
	_debris_mesh = st.commit()
	return _debris_mesh

static func _dry_shrub() -> ArrayMesh:
	if _stump_mesh: return _stump_mesh
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	# Sparse angular dead bush.
	for i in 5:
		var a := TAU * i / 5.0
		_add_box(st, Vector3(cos(a) * 0.3, 0.5, sin(a) * 0.3), Vector3(0.08, 1.0, 0.08))
	st.generate_normals()
	_stump_mesh = st.commit()
	return _stump_mesh

static func _car() -> ArrayMesh:
	if _car_mesh: return _car_mesh
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	var rust := Color(0.20, 0.08, 0.045)
	var rust2 := Color(0.14, 0.06, 0.035)
	var glass := Color(0.02, 0.025, 0.03)
	var tyre := Color(0.015, 0.015, 0.015)
	_cbox(st, Vector3(0, 0.55, 0), Vector3(1.9, 0.7, 4.3), rust)      # chassis/body
	_cbox(st, Vector3(0, 1.05, 0.1), Vector3(1.7, 0.65, 2.0), rust2) # cabin
	_cbox(st, Vector3(0, 1.15, 0.1), Vector3(1.72, 0.4, 1.4), glass) # windows band
	_cbox(st, Vector3(0, 0.6, -2.05), Vector3(1.8, 0.5, 0.3), rust2) # hood front
	# Tyres (flattened, some missing on a wreck).
	_cbox(st, Vector3(0.85, 0.3, 1.4), Vector3(0.35, 0.6, 0.6), tyre)
	_cbox(st, Vector3(-0.85, 0.3, 1.4), Vector3(0.35, 0.6, 0.6), tyre)
	_cbox(st, Vector3(0.85, 0.28, -1.4), Vector3(0.35, 0.55, 0.6), tyre)
	st.generate_normals()
	_car_mesh = st.commit()
	return _car_mesh

static func _cbox(st: SurfaceTool, center: Vector3, size: Vector3, col: Color) -> void:
	st.set_color(col)
	_add_box(st, center, size)

static func _add_box(st: SurfaceTool, center: Vector3, size: Vector3) -> void:
	var h := size * 0.5
	var c := center
	var v := [
		c + Vector3(-h.x, -h.y, -h.z), c + Vector3(h.x, -h.y, -h.z),
		c + Vector3(h.x, h.y, -h.z), c + Vector3(-h.x, h.y, -h.z),
		c + Vector3(-h.x, -h.y, h.z), c + Vector3(h.x, -h.y, h.z),
		c + Vector3(h.x, h.y, h.z), c + Vector3(-h.x, h.y, h.z),
	]
	var faces := [
		[0,1,2,3], [5,4,7,6], [4,0,3,7], [1,5,6,2], [3,2,6,7], [4,5,1,0],
	]
	for f in faces:
		st.add_vertex(v[f[0]]); st.add_vertex(v[f[1]]); st.add_vertex(v[f[2]])
		st.add_vertex(v[f[0]]); st.add_vertex(v[f[2]]); st.add_vertex(v[f[3]])
