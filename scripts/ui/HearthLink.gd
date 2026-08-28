extends CanvasLayer
## HearthLink bracer — a wrist device, not a pause panel. Tab powers it on:
## amber grid + river sketch on a framed screen with scanlines and a status
## header, click a marker to fast travel. Amber-on-dark, mono type.

@export var player_path: NodePath
var _player: Node3D
var _open := false

@onready var _panel: Control = $Panel
@onready var _map: Control = $Panel/Map
@onready var _fade: ColorRect = $Fade

const AMBER := Color(0.96, 0.76, 0.36)
const AMBER_D := Color(0.5, 0.4, 0.2)
const SCREEN := Color(0.04, 0.05, 0.045, 0.98)
var _title: FontFile
var _mono: FontFile

func _ready() -> void:
	_title = UITheme.title_font()
	_mono = UITheme.mono_font()
	_panel.visible = false
	_panel.pivot_offset = _panel.size * 0.5
	_fade.color = Color(0, 0, 0, 0)
	WorldState.discovered.connect(func(_id): _map.queue_redraw())
	_map.draw.connect(_draw_map)
	_map.gui_input.connect(_on_map_input)

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and event.keycode == KEY_TAB:
		_toggle()

func _toggle() -> void:
	_open = not _open
	get_tree().paused = _open
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if _open else Input.MOUSE_MODE_CAPTURED
	if _open:
		_panel.visible = true
		_panel.pivot_offset = _panel.size * 0.5
		_panel.scale = Vector2(0.94, 0.94)
		_panel.modulate = Color(1, 1, 1, 0)
		var tw := create_tween().set_ignore_time_scale(true)
		tw.set_parallel(true)
		tw.tween_property(_panel, "scale", Vector2.ONE, 0.18).set_trans(Tween.TRANS_BACK).set_ease(Tween.EASE_OUT)
		tw.tween_property(_panel, "modulate:a", 1.0, 0.15)
		_map.queue_redraw()
	else:
		_panel.visible = false

func _screen_rect() -> Rect2:
	var m := _map.size
	return Rect2(Vector2(34, 66), Vector2(m.x - 68, m.y - 112))

func _world_to_map(w: Vector2) -> Vector2:
	var sr := _screen_rect()
	var h := WorldConfig.WORLD_HALF
	var u := (w.x + h) / (2.0 * h)
	var v := (w.y + h) / (2.0 * h)
	return sr.position + Vector2(u, v) * sr.size

func _draw_map() -> void:
	var m := _map.size
	var full := Rect2(Vector2.ZERO, m)
	# Device body.
	_map.draw_rect(full, Color(0.08, 0.08, 0.09, 0.97))
	_bracer_frame(full)
	# Header.
	_map.draw_string(_title, Vector2(28, 44), "HEARTHLINK", HORIZONTAL_ALIGNMENT_LEFT, -1, 30, AMBER)
	_map.draw_string(_mono, Vector2(180, 42), "// CINDER VALE  ·  MAP", HORIZONTAL_ALIGNMENT_LEFT, -1, 15, AMBER_D)
	_map.draw_string(_mono, Vector2(m.x - 118, 42), "◉ ONLINE", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, Color(0.5, 0.85, 0.5))

	var sr := _screen_rect()
	_map.draw_rect(sr, SCREEN)
	# Amber grid.
	for i in range(1, 8):
		var t := i / 8.0
		_map.draw_line(Vector2(sr.position.x + t * sr.size.x, sr.position.y), Vector2(sr.position.x + t * sr.size.x, sr.end.y), Color(0.22, 0.17, 0.08), 1.0)
		_map.draw_line(Vector2(sr.position.x, sr.position.y + t * sr.size.y), Vector2(sr.end.x, sr.position.y + t * sr.size.y), Color(0.22, 0.17, 0.08), 1.0)
	# River sketch.
	var pts := PackedVector2Array()
	for z in range(-400, 401, 40):
		var rx := sin(z * 0.006) * 40.0 + 15.0
		pts.append(_world_to_map(Vector2(rx, z)))
	for i in range(pts.size() - 1):
		_map.draw_line(pts[i], pts[i + 1], Color(0.25, 0.42, 0.52, 0.8), 3.0)
	# POI markers.
	for p in WorldConfig.POIS:
		var mp := _world_to_map(p["pos"])
		var known: bool = WorldState.is_found(p["id"])
		if known:
			_map.draw_rect(Rect2(mp - Vector2(4, 4), Vector2(8, 8)), AMBER)
			_map.draw_string(_mono, mp + Vector2(10, 5), p["name"], HORIZONTAL_ALIGNMENT_LEFT, -1, 13, AMBER)
		else:
			_map.draw_arc(mp, 4.0, 0, TAU, 12, Color(0.35, 0.32, 0.26), 1.5)
	# Player blip + facing.
	if _player:
		var pm := _world_to_map(Vector2(_player.global_position.x, _player.global_position.z))
		var fwd := -_player.global_transform.basis.z
		var dir := Vector2(fwd.x, fwd.z).normalized()
		_map.draw_colored_polygon(PackedVector2Array([pm + dir * 9, pm + dir.rotated(2.4) * 6, pm + dir.rotated(-2.4) * 6]), Color(0.5, 0.85, 1.0))
	# Scanlines over the screen.
	for y in range(int(sr.position.y), int(sr.end.y), 3):
		_map.draw_line(Vector2(sr.position.x, y), Vector2(sr.end.x, y), Color(0, 0, 0, 0.10), 1.0)
	# Footer.
	_map.draw_string(_mono, Vector2(34, m.y - 20), "▸ CLICK A MARKER TO FAST TRAVEL", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, AMBER_D)
	_map.draw_string(_mono, Vector2(m.x - 120, m.y - 20), "[TAB] CLOSE", HORIZONTAL_ALIGNMENT_LEFT, -1, 14, AMBER_D)

func _bracer_frame(r: Rect2) -> void:
	var cut := 22.0
	var col := AMBER
	# Cut-corner outline.
	var p := PackedVector2Array([
		r.position + Vector2(cut, 0), Vector2(r.end.x - cut, r.position.y),
		Vector2(r.end.x, r.position.y + cut), Vector2(r.end.x, r.end.y - cut),
		Vector2(r.end.x - cut, r.end.y), Vector2(r.position.x + cut, r.end.y),
		Vector2(r.position.x, r.end.y - cut), Vector2(r.position.x, r.position.y + cut),
		r.position + Vector2(cut, 0),
	])
	_map.draw_polyline(p, col, 2.5)
	# Corner brackets + rivets.
	for corner in [r.position, Vector2(r.end.x, r.position.y), r.end, Vector2(r.position.x, r.end.y)]:
		_map.draw_circle(corner + (r.get_center() - corner).normalized() * 30.0, 2.5, AMBER_D)
	# Header divider.
	_map.draw_line(Vector2(r.position.x + cut, 56), Vector2(r.end.x - cut, 56), Color(AMBER.r, AMBER.g, AMBER.b, 0.4), 1.5)

func _on_map_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		for p in WorldConfig.POIS:
			if not WorldState.is_found(p["id"]):
				continue
			if event.position.distance_to(_world_to_map(p["pos"])) < 14.0:
				_fast_travel(p)
				return

func _fast_travel(p: Dictionary) -> void:
	_toggle()
	var tw := create_tween().set_ignore_time_scale(true)
	tw.tween_property(_fade, "color", Color(0, 0, 0, 1), 0.35)
	tw.tween_callback(func():
		var pos: Vector2 = p["pos"]
		var y := WorldConfig.height(pos.x, pos.y) + 1.0
		_player.global_position = Vector3(pos.x, y, pos.y)
	)
	tw.tween_interval(0.15)
	tw.tween_property(_fade, "color", Color(0, 0, 0, 0), 0.35)
