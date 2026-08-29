extends SceneTree
## Loads the world, waits a few frames, saves a screenshot, quits.
## Godot --path . --script res://scripts/tools/shot.gd
var _frames := 0

func _initialize() -> void:
	var world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(world)

func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 30:
		var img := get_root().get_viewport().get_texture().get_image()
		img.save_png("user://shot.png")
		var path := ProjectSettings.globalize_path("user://shot.png")
		print("SHOT: " + path)
		quit()
	return false
