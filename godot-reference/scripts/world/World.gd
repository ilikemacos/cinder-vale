extends Node3D
## World root. Spawns the player at the bus wreck on the highway and shows
## the opening objective. No vault, no bunker — the scavver is already here.

@onready var _player: CharacterBody3D = $Player

func _ready() -> void:
	_player.global_transform = WorldConfig.spawn_transform()
	# Face roughly toward town (origin) from the bus wreck.
	var to_town: Vector2 = Vector2(0, 0) - WorldConfig.BUS_WRECK
	_player.rotation.y = atan2(to_town.x, to_town.y)
	WorldState.discover("town")  # town is visible on the map from the start
	_build_backdrop()
	_intro_fade()

func _intro_fade() -> void:
	var cl := CanvasLayer.new()
	cl.layer = 128
	add_child(cl)
	var f := ColorRect.new()
	f.color = Color(0, 0, 0, 1)
	f.set_anchors_preset(Control.PRESET_FULL_RECT)
	f.anchor_right = 1.0
	f.anchor_bottom = 1.0
	f.mouse_filter = Control.MOUSE_FILTER_IGNORE
	cl.add_child(f)
	var tw := create_tween()
	tw.tween_interval(0.2)
	tw.tween_property(f, "color:a", 0.0, 0.7)
	tw.tween_callback(cl.queue_free)

func _build_backdrop() -> void:
	# Two static rings of hazy mountain silhouettes on the horizon, well beyond
	# the streamed region — pure backdrop, no collision, dark so fog blends them.
	add_child(_mountain_ring(760.0, 90.0, 150.0, Color(0.34, 0.33, 0.31), 44, 771))
	add_child(_mountain_ring(1050.0, 150.0, 240.0, Color(0.4, 0.42, 0.46), 40, 553))

func _mountain_ring(radius: float, hmin: float, hmax: float, col: Color, count: int, seed: int) -> MeshInstance3D:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed
	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	for i in count:
		var a0 := TAU * i / count
		var a1 := TAU * (i + 1) / count
		var spread := (a1 - a0) * rng.randf_range(0.6, 1.1)
		var mid := (a0 + a1) * 0.5
		var h := rng.randf_range(hmin, hmax)
		var base_l := Vector3(cos(mid - spread) * radius, -20, sin(mid - spread) * radius)
		var base_r := Vector3(cos(mid + spread) * radius, -20, sin(mid + spread) * radius)
		var peak := Vector3(cos(mid) * radius, h, sin(mid) * radius)
		st.add_vertex(base_l); st.add_vertex(peak); st.add_vertex(base_r)
	st.generate_normals()
	var mi := MeshInstance3D.new()
	mi.mesh = st.commit()
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = 1.0
	m.cull_mode = BaseMaterial3D.CULL_DISABLED
	mi.material_override = m
	mi.gi_mode = GeometryInstance3D.GI_MODE_DISABLED
	return mi
