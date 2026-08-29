extends Control
## Cinder Vale in-world HUD — HearthLink chrome, drawn (no debug widgets).
## Compass, thin health pip with amber ticks, ammo as "30 | 120" in the title
## face, a bracket reticle. Objective fades after 20 m walked; the control
## legend fades after 10 s. Everything else stays out of the way.

@export var player_path: NodePath
var _p: Node

var _amber := UITheme.AMBER
var _ink := Color(0.05, 0.05, 0.06, 0.72)
var _title: FontFile
var _mono: FontFile

var _walk_start: Vector3
var _walked := 0.0
var _elapsed := 0.0
var _obj_a := 1.0
var _legend_a := 1.0
var _flash := 0.0
var _hp := 1.0
var _fire_pulse := 0.0

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	mouse_filter = Control.MOUSE_FILTER_IGNORE
	_title = UITheme.title_font()
	_mono = UITheme.mono_font()
	_p = get_node_or_null(player_path)
	if _p:
		_walk_start = _p.global_position
		_hp = _p.health / _p.max_health
		_p.damaged.connect(func(): _flash = 1.0)
		_p.health_changed.connect(func(c, m): _hp = c / m)

func _process(delta: float) -> void:
	_elapsed += delta
	_flash = maxf(0.0, _flash - delta * 2.2)
	_fire_pulse = maxf(0.0, _fire_pulse - delta * 6.0)
	if _p and _p.get("_fire_cd") != null and _p._fire_cd > 0.09:
		_fire_pulse = 1.0
	if _p:
		_walked = _p.global_position.distance_to(_walk_start)
	_legend_a = lerpf(_legend_a, 0.0 if _elapsed > 10.0 else 1.0, clampf(delta * 3.0, 0, 1))
	_obj_a = lerpf(_obj_a, 0.0 if _walked > 20.0 else 1.0, clampf(delta * 3.0, 0, 1))
	queue_redraw()

func _draw() -> void:
	var s := size
	if _flash > 0.01:
		draw_rect(Rect2(Vector2.ZERO, s), Color(0.6, 0.05, 0.05, _flash * 0.4))
	_draw_compass(s)
	_draw_reticle(s)
	_draw_health(s)
	_draw_ammo(s)
	if _obj_a > 0.01:
		_draw_objective(s)
	if _legend_a > 0.01:
		_draw_legend(s)
	if _p and not _p.alive:
		_draw_dead(s)

# --- compass -----------------------------------------------------------------

func _draw_compass(s: Vector2) -> void:
	var cx := s.x * 0.5
	var top := 26.0
	var half := 230.0
	var span := deg_to_rad(70.0)  # degrees visible each side
	var heading := 0.0
	if _p:
		var fwd: Vector3 = -_p.global_transform.basis.z
		heading = atan2(fwd.x, fwd.z)
	# Backing strip.
	draw_rect(Rect2(cx - half, top - 4, half * 2, 26), Color(0.05, 0.05, 0.06, 0.5))
	draw_line(Vector2(cx - half, top + 18), Vector2(cx + half, top + 18), Color(_amber.r, _amber.g, _amber.b, 0.35), 1.0)
	# Cardinal + inter-cardinal ticks.
	var marks := {0.0: "N", PI * 0.5: "E", PI: "S", -PI * 0.5: "W",
		PI * 0.25: "·", PI * 0.75: "·", -PI * 0.25: "·", -PI * 0.75: "·"}
	for bearing in marks:
		var rel := wrapf(bearing - heading, -PI, PI)
		if absf(rel) > span:
			continue
		var x := cx + (rel / span) * half
		var lbl: String = marks[bearing]
		var big := lbl != "·"
		draw_line(Vector2(x, top), Vector2(x, top + (12 if big else 6)), Color(_amber.r, _amber.g, _amber.b, 0.8 if big else 0.4), 1.5 if big else 1.0)
		if big:
			var w := _title.get_string_size(lbl, HORIZONTAL_ALIGNMENT_LEFT, -1, 16).x
			draw_string(_title, Vector2(x - w * 0.5, top - 6), lbl, HORIZONTAL_ALIGNMENT_LEFT, -1, 16, _amber)
	# POI markers (amber) + nearest hostile (red) as ticks on the strip.
	if _p:
		for poi in WorldConfig.POIS:
			if not WorldState.is_found(poi["id"]):
				continue
			_compass_tick(cx, top, half, span, heading, Vector2(poi["pos"].x, poi["pos"].y), Color(_amber.r, _amber.g, _amber.b, 0.7), 3.0)
		var nearest = _nearest_hostile()
		if nearest != null:
			_compass_tick(cx, top, half, span, heading, Vector2(nearest.global_position.x, nearest.global_position.z), Color(0.85, 0.2, 0.15), 4.0)
	# Fixed centre heading marker.
	draw_colored_polygon(PackedVector2Array([Vector2(cx, top + 20), Vector2(cx - 5, top + 28), Vector2(cx + 5, top + 28)]), _amber)

func _compass_tick(cx: float, top: float, half: float, span: float, heading: float, target: Vector2, col: Color, r: float) -> void:
	var d := target - Vector2(_p.global_position.x, _p.global_position.z)
	if d.length() < 1.0:
		return
	var bearing := atan2(d.x, d.y)
	var rel := wrapf(bearing - heading, -PI, PI)
	if absf(rel) > span:
		return
	var x := cx + (rel / span) * half
	draw_circle(Vector2(x, top + 18), r, col)

func _nearest_hostile():
	var best = null
	var bd := 45.0
	for e in get_tree().get_nodes_in_group("enemy"):
		var dd: float = e.global_position.distance_to(_p.global_position)
		if dd < bd:
			bd = dd
			best = e
	return best

# --- reticle -----------------------------------------------------------------

func _draw_reticle(s: Vector2) -> void:
	var c := s * 0.5
	var gap := 7.0 + _fire_pulse * 5.0
	var len := 7.0
	var col := Color(_amber.r, _amber.g, _amber.b, 0.9)
	for dir in [Vector2.UP, Vector2.DOWN, Vector2.LEFT, Vector2.RIGHT]:
		draw_line(c + dir * gap, c + dir * (gap + len), col, 1.5)
	draw_circle(c, 1.2, col)

# --- health / ammo -----------------------------------------------------------

func _draw_health(s: Vector2) -> void:
	var x := 40.0
	var y := s.y - 48.0
	var w := 240.0
	var h := 7.0
	draw_rect(Rect2(x, y, w, h), _ink)
	var hpcol := _amber if _hp > 0.35 else Color(0.85, 0.3, 0.18)
	draw_rect(Rect2(x, y, w * clampf(_hp, 0, 1), h), hpcol)
	# Amber ticks every 25%.
	for i in range(1, 4):
		var tx := x + w * (i / 4.0)
		draw_line(Vector2(tx, y - 2), Vector2(tx, y + h + 2), Color(0.02, 0.02, 0.03, 0.9), 1.0)
	draw_rect(Rect2(x, y, w, h), Color(_amber.r, _amber.g, _amber.b, 0.5), false, 1.0)
	draw_string(_mono, Vector2(x, y - 8), "VITALS", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color(_amber.r, _amber.g, _amber.b, 0.7))

func _draw_ammo(s: Vector2) -> void:
	if _p == null:
		return
	var txt := "%d | %d" % [_p.ammo, _p.reserve]
	if _p.reloading:
		txt = "-- | %d" % _p.reserve
	var fs := 34
	var w := _title.get_string_size(txt, HORIZONTAL_ALIGNMENT_LEFT, -1, fs).x
	var col := _amber if (_p.ammo > 0 or _p.reloading) else Color(0.85, 0.3, 0.18)
	draw_string(_title, Vector2(s.x - 40 - w, s.y - 34), txt, HORIZONTAL_ALIGNMENT_LEFT, -1, fs, col)
	draw_string(_mono, Vector2(s.x - 40 - w, s.y - 58), "MAG | RESERVE" if not _p.reloading else "RELOADING", HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color(_amber.r, _amber.g, _amber.b, 0.7))

# --- transient text ----------------------------------------------------------

func _draw_objective(s: Vector2) -> void:
	var a := _obj_a
	draw_string(_mono, Vector2(40, 40), "OBJECTIVE", HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color(_amber.r, _amber.g, _amber.b, 0.7 * a))
	draw_string(_mono, Vector2(40, 62), "Reach Cinder Vale.", HORIZONTAL_ALIGNMENT_LEFT, -1, 18, Color(TEXT_A(a)))

func _draw_legend(s: Vector2) -> void:
	var a := _legend_a
	var t := "WASD move   ·   LMB fire   ·   R reload   ·   Shift sprint   ·   C crouch   ·   V camera   ·   Tab HearthLink"
	draw_string(_mono, Vector2(40, s.y - 74), t, HORIZONTAL_ALIGNMENT_LEFT, -1, 13, Color(0.8, 0.8, 0.82, 0.6 * a))

func _draw_dead(s: Vector2) -> void:
	draw_rect(Rect2(Vector2.ZERO, s), Color(0, 0, 0, 0.5))
	var t := "YOU DIED"
	var w := _title.get_string_size(t, HORIZONTAL_ALIGNMENT_LEFT, -1, 64).x
	draw_string(_title, Vector2(s.x * 0.5 - w * 0.5, s.y * 0.5), t, HORIZONTAL_ALIGNMENT_LEFT, -1, 64, Color(0.8, 0.12, 0.1))

func TEXT_A(a: float) -> Color:
	return Color(0.9, 0.9, 0.92, a)
