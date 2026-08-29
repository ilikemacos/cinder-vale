extends SceneTree
## Eye-level town view for look/feel tuning.
var _frames := 0
var _cam: Camera3D
var _world: Node

func _initialize() -> void:
	print("INIT")
	_world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(_world)
	var p := _world.get_node_or_null("Player")
	print("player node: ", p)
	if p:
		p.global_position = Vector3(140, 10, -40)
	_cam = Camera3D.new()
	_cam.fov = 72
	_cam.far = 800
	get_root().add_child(_cam)

func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 4:
		var eye := Vector3(52, 0, 82)
		eye.y = WorldConfig.height(eye.x, eye.z) + 1.7
		_cam.look_at_from_position(eye, Vector3(0, 8, 0), Vector3.UP)
		_cam.make_current()
	if _frames == 40:
		var img := get_root().get_viewport().get_texture().get_image()
		var err := img.save_png("user://waste1.png")
		print("SAVE err=", err, " -> ", ProjectSettings.globalize_path("user://waste1.png"))
		quit()
	return false
