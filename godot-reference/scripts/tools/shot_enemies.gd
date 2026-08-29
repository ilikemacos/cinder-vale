extends SceneTree
var _frames := 0
var _cam: Camera3D
var _world: Node
func _initialize() -> void:
	_world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(_world)
	_cam = Camera3D.new()
	_cam.fov = 70
	_cam.far = 800
	get_root().add_child(_cam)
func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 1:
		var p := _world.get_node_or_null("Player")
		if p: p.global_position = Vector3(-30, 10, 285)
	if _frames == 6:
		var eye := Vector3(-30, 0, 288)
		eye.y = WorldConfig.height(eye.x, eye.z) + 1.7
		_cam.look_at_from_position(eye, Vector3(-30, 6, 320), Vector3.UP)
		_cam.make_current()
	if _frames == 90:
		print("enemies: ", get_nodes_in_group("enemy").size())
		get_root().get_viewport().get_texture().get_image().save_png("user://enemies.png")
		print("SAVE -> ", ProjectSettings.globalize_path("user://enemies.png"))
		quit()
	return false
