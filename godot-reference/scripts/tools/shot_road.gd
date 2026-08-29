extends SceneTree
var _frames := 0
var _cam: Camera3D
var _world: Node
func _initialize() -> void:
	_world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(_world)
	_cam = Camera3D.new()
	_cam.fov = 68
	_cam.far = 900
	get_root().add_child(_cam)
func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 2:
		var p := _world.get_node_or_null("Player")
		if p: p.global_position = Vector3(-22, 8, 135)  # on the bus->town road
	if _frames == 8:
		var eye := Vector3(-24, 0, 150)
		eye.y = WorldConfig.height(eye.x, eye.z) + 2.2
		_cam.look_at_from_position(eye, Vector3(0, 4, 0), Vector3.UP)  # look toward town
		_cam.make_current()
	if _frames == 46:
		get_root().get_viewport().get_texture().get_image().save_png("user://road.png")
		print("SAVE -> ", ProjectSettings.globalize_path("user://road.png"))
		quit()
	return false
