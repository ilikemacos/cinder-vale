extends SceneTree
var _frames := 0
var _cam: Camera3D
var _world: Node
func _initialize() -> void:
	_world = load("res://scenes/world/World.tscn").instantiate()
	get_root().add_child(_world)
	_cam = Camera3D.new()
	_cam.fov = 62
	_cam.far = 1200
	get_root().add_child(_cam)
func _process(_d: float) -> bool:
	_frames += 1
	if _frames == 2:
		var p := _world.get_node_or_null("Player")
		if p: p.global_position = Vector3(60, 8, 95)
		for n in ["HUD", "CombatHUD", "HearthLink"]:
			var cl := _world.get_node_or_null(n)
			if cl: cl.visible = false
	if _frames == 10:
		var eye := Vector3(95, 0, 120)
		eye.y = WorldConfig.height(eye.x, eye.z) + 8.0
		_cam.look_at_from_position(eye, Vector3(-10, 10, -10), Vector3.UP)
		_cam.make_current()
	if _frames == 50:
		get_root().get_viewport().get_texture().get_image().save_png("res://assets/ui/menu_bg.png")
		print("SAVE hero")
		quit()
	return false
