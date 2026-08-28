extends SceneTree
## Elevated overview of the valley for verification.
var _frames := 0
var _cam: Camera3D

func _initialize() -> void:
	var world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(world)
	_cam = Camera3D.new()
	_cam.fov = 70
	_cam.far = 1200
	get_root().add_child(_cam)

func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 3:
		_cam.look_at_from_position(Vector3(-140, 120, 320), Vector3(60, 0, 40), Vector3.UP)
		_cam.make_current()
	if _frames == 40:
		var img := get_root().get_viewport().get_texture().get_image()
		img.save_png("user://overview.png")
		print("SHOT: " + ProjectSettings.globalize_path("user://overview.png"))
		quit()
	return false
