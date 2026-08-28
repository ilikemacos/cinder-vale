extends CanvasLayer
## HearthLink bracer — amber-on-dark map overlay. Tab toggles it.
## Draws the valley, discovered POI markers, and the player blip.
## Click a discovered marker to fast travel (short black fade, no load screen).

@export var player_path: NodePath
var _player: Node3D
var _open := false

@onready var _panel: Control = $Panel
@onready var _map: Control = $Panel/Map
@onready var _fade: ColorRect = $Fade

const AMBER := Color(0.95, 0.75, 0.35)
const DIM := Color(0.55, 0.42, 0.2)

func _ready() -> void:
	_player = get_node(player_path)
	_panel.visible = false
	_fade.color = Color(0, 0, 0, 0)
	WorldState.discovered.connect(func(_id): _map.queue_redraw())
	_map.draw.connect(_draw_map)
	_map.gui_input.connect(_on_map_input)

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and event.keycode == KEY_TAB:
		_toggle()

func _toggle() -> void:
	_open = not _open
	_panel.visible = _open
	get_tree().paused = _open
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if _open else Input.MOUSE_MODE_CAPTURED
	if _open:
		_map.queue_redraw()

func _map_rect() -> Rect2:
	return Rect2(Vector2.ZERO, _map.size)

func _world_to_map(w: Vector2) -> Vector2:
	var h := WorldConfig.WORLD_HALF
	var u := (w.x + h) / (2.0 * h)
	var v := (w.y + h) / (2.0 * h)
	return Vector2(u, v) * _map.size

func _draw_map() -> void:
	var r := _map_rect()
	_map.draw_rect(r, Color(0.08, 0.07, 0.05))
	# Grid.
	for i in range(1, 8):
		var t := i / 8.0
		_map.draw_line(Vector2(t * r.size.x, 0), Vector2(t * r.size.x, r.size.y), Color(0.2, 0.16, 0.08), 1.0)
		_map.draw_line(Vector2(0, t * r.size.y), Vector2(r.size.x, t * r.size.y), Color(0.2, 0.16, 0.08), 1.0)
	_map.draw_rect(r, AMBER, false, 2.0)
	# River sketch.
	var pts := PackedVector2Array()
	for z in range(-400, 401, 40):
		var rx := sin(z * 0.006) * 40.0 + 15.0
		pts.append(_world_to_map(Vector2(rx, z)))
	for i in range(pts.size() - 1):
		_map.draw_line(pts[i], pts[i + 1], Color(0.25, 0.4, 0.5), 3.0)
	# POI markers.
	for p in WorldConfig.POIS:
		var mp := _world_to_map(p["pos"])
		var known: bool = WorldState.is_found(p["id"])
		var col: Color = AMBER if known else Color(0.3, 0.28, 0.24)
		_map.draw_circle(mp, 6.0, col)
		if known:
			_map.draw_string(ThemeDB.fallback_font, mp + Vector2(9, 4), p["name"], HORIZONTAL_ALIGNMENT_LEFT, -1, 14, AMBER)
	# Player blip.
	if _player:
		var pm := _world_to_map(Vector2(_player.global_position.x, _player.global_position.z))
		_map.draw_circle(pm, 5.0, Color(0.4, 0.8, 1.0))
		_map.draw_arc(pm, 9.0, 0, TAU, 20, Color(0.4, 0.8, 1.0), 1.5)

func _on_map_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		for p in WorldConfig.POIS:
			if not WorldState.is_found(p["id"]):
				continue
			if event.position.distance_to(_world_to_map(p["pos"])) < 12.0:
				_fast_travel(p)
				return

func _fast_travel(p: Dictionary) -> void:
	_toggle()
	var tw := create_tween()
	tw.tween_property(_fade, "color", Color(0, 0, 0, 1), 0.35)
	tw.tween_callback(func():
		var pos: Vector2 = p["pos"]
		var y := WorldConfig.height(pos.x, pos.y) + 1.0
		_player.global_position = Vector3(pos.x, y, pos.y)
	)
	tw.tween_interval(0.15)
	tw.tween_property(_fade, "color", Color(0, 0, 0, 0), 0.35)
