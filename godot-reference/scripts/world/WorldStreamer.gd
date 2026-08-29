extends Node3D
class_name WorldStreamer
## Streams the 2x2 valley. Keeps the player's tile + edge-adjacent tiles
## loaded; unloads the rest (the diagonal). Each tile owns its terrain,
## the POI blockouts whose centre lies inside it, and the road segments in it.

@export var player_path: NodePath
var _player: Node3D
var _tiles := {}          # Vector2i -> Node3D
var _current := Vector2i(-999, -999)

func _ready() -> void:
	_player = get_node(player_path)
	_refresh(_tile_of(_player.global_position))

func _process(_delta: float) -> void:
	if _player == null:
		return
	var t := _tile_of(_player.global_position)
	if t != _current:
		_refresh(t)

func _tile_of(pos: Vector3) -> Vector2i:
	var half := WorldConfig.TILES_PER_AXIS / 2.0
	var tx := int(floor(pos.x / WorldConfig.TILE_SIZE + half))
	var tz := int(floor(pos.z / WorldConfig.TILE_SIZE + half))
	tx = clampi(tx, 0, WorldConfig.TILES_PER_AXIS - 1)
	tz = clampi(tz, 0, WorldConfig.TILES_PER_AXIS - 1)
	return Vector2i(tx, tz)

func _wanted(center: Vector2i) -> Array:
	# High memory budget: keep the whole 2x2 region resident (no unloads).
	if WorldState.ram_budget_gb >= 6:
		var all := []
		for x in WorldConfig.TILES_PER_AXIS:
			for z in WorldConfig.TILES_PER_AXIS:
				all.append(Vector2i(x, z))
		return all
	# Active + the 4 edge neighbours that exist. Diagonals dropped.
	var out := [center]
	for d in [Vector2i(1,0), Vector2i(-1,0), Vector2i(0,1), Vector2i(0,-1)]:
		var t: Vector2i = center + d
		if t.x >= 0 and t.x < WorldConfig.TILES_PER_AXIS and t.y >= 0 and t.y < WorldConfig.TILES_PER_AXIS:
			out.append(t)
	return out

func _refresh(center: Vector2i) -> void:
	_current = center
	var want := _wanted(center)
	# Unload tiles no longer wanted.
	for key in _tiles.keys():
		if not want.has(key):
			_tiles[key].queue_free()
			_tiles.erase(key)
	# Load newly wanted tiles.
	for t in want:
		if not _tiles.has(t):
			_tiles[t] = _make_tile(t)

func _make_tile(t: Vector2i) -> Node3D:
	var root := Node3D.new()
	root.name = "Tile_%d_%d" % [t.x, t.y]
	add_child(root)

	var terrain := TerrainTile.new()
	terrain.setup(t.x, t.y)
	root.add_child(terrain)

	# POIs whose centre falls in this tile.
	for p in WorldConfig.POIS:
		if _tile_of(Vector3(p["pos"].x, 0, p["pos"].y)) == t:
			var poi := POIBlockout.new()
			poi.setup(p)
			root.add_child(poi)

	# Roads whose midpoint falls in this tile.
	for seg in _road_segments():
		var a: Vector2 = seg[0]
		var b: Vector2 = seg[1]
		var w: float = seg[2]
		var mid: Vector2 = (a + b) * 0.5
		if _tile_of(Vector3(mid.x, 0, mid.y)) == t:
			_add_road(root, a, b, w)
			_add_powerline(root, a, b)

	return root

func _pole_mat() -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = Color(0.12, 0.10, 0.08)
	m.roughness = 0.95
	return m

func _add_powerline(parent: Node3D, a: Vector2, b: Vector2) -> void:
	var dir := (b - a).normalized()
	var perp := Vector2(-dir.y, dir.x)
	var length := a.distance_to(b)
	var spacing := 34.0
	var n := maxi(2, int(length / spacing))
	var mat := _pole_mat()
	var tops := []
	for i in range(n + 1):
		var p2 := a.lerp(b, float(i) / n) + perp * 7.0
		var gy := WorldConfig.height(p2.x, p2.y)
		var top := Vector3(p2.x, gy + 9.0, p2.y)
		tops.append(top)
		_pole(parent, Vector3(p2.x, gy, p2.y), mat)
	# String a sagging wire between consecutive pole tops.
	for i in range(tops.size() - 1):
		_wire(parent, tops[i], tops[i + 1], mat)

func _pole(parent: Node3D, base: Vector3, mat: StandardMaterial3D) -> void:
	var post := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.3, 9.0, 0.3)
	post.mesh = bm
	post.material_override = mat
	post.position = base + Vector3(0, 4.5, 0)
	parent.add_child(post)
	var arm := MeshInstance3D.new()
	var am := BoxMesh.new()
	am.size = Vector3(3.0, 0.22, 0.22)
	arm.mesh = am
	arm.material_override = mat
	arm.position = base + Vector3(0, 8.3, 0)
	parent.add_child(arm)

func _wire(parent: Node3D, a: Vector3, b: Vector3, mat: StandardMaterial3D) -> void:
	# Three short segments with a slight catenary sag.
	var steps := 3
	var prev := a
	for i in range(1, steps + 1):
		var t := float(i) / steps
		var p := a.lerp(b, t)
		p.y -= sin(t * PI) * 1.4  # sag
		_wire_seg(parent, prev, p, mat)
		prev = p

func _wire_seg(parent: Node3D, a: Vector3, b: Vector3, mat: StandardMaterial3D) -> void:
	var seg := MeshInstance3D.new()
	var bm := BoxMesh.new()
	var dist := a.distance_to(b)
	bm.size = Vector3(0.06, 0.06, dist)
	seg.mesh = bm
	seg.material_override = mat
	seg.position = (a + b) * 0.5
	seg.look_at_from_position((a + b) * 0.5, b, Vector3.UP)
	parent.add_child(seg)

func _road_segments() -> Array:
	# bus -> town, then town spokes to the major POIs.
	var town := Vector2(0, 0)
	var segs := [[WorldConfig.BUS_WRECK, town, 8.0]]
	for id in ["dam", "quarry", "mill", "farm", "clinic", "convoy"]:
		var p := WorldState.poi_by_id(id)
		if p:
			segs.append([town, p["pos"], 6.0])
	return segs

func _add_road(parent: Node3D, a: Vector2, b: Vector2, width: float) -> void:
	# Cracked asphalt: a grid of strips with per-vertex colouring for a faded
	# centre line, worn patches, and dark cracks. Vertex colours are LINEAR.
	var steps := int(a.distance_to(b) / 6.0) + 1
	var cols := 8
	var mat := StandardMaterial3D.new()
	mat.vertex_color_use_as_albedo = true
	mat.roughness = 1.0
	var mm := MeshInstance3D.new()
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	var dir := (b - a).normalized()
	var perp := Vector2(-dir.y, dir.x) * width * 0.5
	for i in range(steps):
		var t0 := float(i) / steps
		var t1 := float(i + 1) / steps
		var c0 := a.lerp(b, t0)
		var c1 := a.lerp(b, t1)
		for j in range(cols):
			var u0 := lerpf(-1.0, 1.0, float(j) / cols)
			var u1 := lerpf(-1.0, 1.0, float(j + 1) / cols)
			var p00 := c0 + perp * u0
			var p10 := c0 + perp * u1
			var p01 := c1 + perp * u0
			var p11 := c1 + perp * u1
			_road_vert(st, p00, u0)
			_road_vert(st, p10, u1)
			_road_vert(st, p11, u1)
			_road_vert(st, p00, u0)
			_road_vert(st, p11, u1)
			_road_vert(st, p01, u0)
	st.generate_normals()
	mm.mesh = st.commit()
	mm.material_override = mat
	mm.gi_mode = GeometryInstance3D.GI_MODE_DISABLED
	parent.add_child(mm)

func _road_vert(st: SurfaceTool, p: Vector2, u: float) -> void:
	st.set_color(_road_color(p.x, p.y, u))
	st.add_vertex(Vector3(p.x, WorldConfig.height(p.x, p.y) + 0.14, p.y))

func _road_color(wx: float, wz: float, u: float) -> Color:
	var base := Color(0.05, 0.05, 0.055)                 # dark asphalt
	# Worn / sun-bleached patches.
	var wear := fposmod(sin(wx * 0.21 + wz * 0.17) * 3.3, 1.0)
	base = base.lerp(Color(0.11, 0.105, 0.10), clampf(wear * 0.7, 0.0, 0.6))
	# Cracks: narrow dark bands from a cheap hash.
	var h := fposmod(sin(wx * 1.7 + wz * 2.3) * 43758.5, 1.0)
	if h < 0.06 or absf(fposmod(wx * 0.5 + wz * 0.4, 2.0) - 1.0) < 0.04:
		base = base.darkened(0.5)
	# Faded double centre line.
	if absf(u) < 0.09:
		base = base.lerp(Color(0.16, 0.12, 0.02), 0.7)
	# Grimy edges.
	if absf(u) > 0.86:
		base = base.lerp(Color(0.08, 0.075, 0.06), 0.6)
	return base
