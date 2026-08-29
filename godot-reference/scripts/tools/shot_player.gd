extends SceneTree
var _frames := 0
var _cam: Camera3D
var _world: Node
var _p: Node3D
func _initialize() -> void:
	_world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(_world)
	_cam = Camera3D.new()
	_cam.fov = 45
	get_root().add_child(_cam)
func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 2:
		_p = _world.get_node_or_null("Player")
		if _p: _p.global_position = Vector3(70, 6, 70)
	if _frames == 30:
		# Frame the player's upper body from front-right.
		var c := _p.global_position + Vector3(0, 1.1, 0)
		_cam.look_at_from_position(c + Vector3(2.6, 0.3, -3.2), c, Vector3.UP)
		_cam.make_current()
	if _frames == 46:
		get_root().get_viewport().get_texture().get_image().save_png("user://player.png")
		print("SAVE -> ", ProjectSettings.globalize_path("user://player.png"))
		quit()
	return false
