extends RefCounted
class_name UITheme
## Shared UI theme for Cinder Vale — dark glass panels, amber accents, rounded
## controls with clear hover/press states. Built in code so it applies live to
## the launcher and in-game menus without a .tres asset.

const AMBER := Color(0.96, 0.76, 0.36)
const AMBER_DIM := Color(0.62, 0.49, 0.24)
const INK := Color(0.055, 0.058, 0.07)
const TEXT := Color(0.90, 0.90, 0.92)
const TEXT_DIM := Color(0.62, 0.63, 0.66)
const GREEN := Color(0.55, 0.85, 0.5)      # terminal phosphor

static var _title: FontFile
static var _mono: FontFile

static func title_font() -> FontFile:
	if _title == null:
		_title = load("res://assets/ui/fonts/Oswald.ttf")
	return _title

static func mono_font() -> FontFile:
	if _mono == null:
		_mono = load("res://assets/ui/fonts/ShareTechMono.ttf")
	return _mono

static func _sb(bg: Color, radius := 8, border := 0, border_col := Color(0,0,0,0)) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = bg
	s.set_corner_radius_all(radius)
	s.content_margin_left = 16
	s.content_margin_right = 16
	s.content_margin_top = 9
	s.content_margin_bottom = 9
	if border > 0:
		s.set_border_width_all(border)
		s.border_color = border_col
	return s

static func build() -> Theme:
	var t := Theme.new()
	t.default_font = mono_font()
	t.default_font_size = 17

	# --- Button (default: subtle glass) ---
	t.set_stylebox("normal", "Button", _sb(Color(0.14, 0.15, 0.18, 0.9)))
	t.set_stylebox("hover", "Button", _sb(Color(0.20, 0.21, 0.25, 0.95), 8, 1, AMBER_DIM))
	t.set_stylebox("pressed", "Button", _sb(Color(0.10, 0.10, 0.12, 0.98)))
	t.set_stylebox("focus", "Button", _sb(Color(0, 0, 0, 0), 8, 1, AMBER_DIM))
	t.set_color("font_color", "Button", TEXT)
	t.set_color("font_hover_color", "Button", AMBER)
	t.set_color("font_pressed_color", "Button", AMBER)
	t.set_font_size("font_size", "Button", 17)

	# --- OptionButton ---
	for st in ["normal", "hover", "pressed", "focus"]:
		t.set_stylebox(st, "OptionButton", _sb(Color(0.12, 0.13, 0.16, 0.95), 6, 1, Color(0.3, 0.3, 0.34)))
	t.set_color("font_color", "OptionButton", TEXT)
	t.set_color("font_hover_color", "OptionButton", AMBER)

	# --- PopupMenu (dropdown list) ---
	t.set_stylebox("panel", "PopupMenu", _sb(Color(0.10, 0.11, 0.14, 0.98), 6, 1, Color(0.3, 0.3, 0.34)))
	t.set_stylebox("hover", "PopupMenu", _sb(AMBER, 4))
	t.set_color("font_color", "PopupMenu", TEXT)
	t.set_color("font_hover_color", "PopupMenu", INK)

	# --- CheckButton ---
	t.set_color("font_color", "CheckButton", TEXT)
	t.set_color("font_hover_color", "CheckButton", AMBER)

	# --- Panels ---
	t.set_stylebox("panel", "PanelContainer", _sb(Color(0.08, 0.085, 0.11, 0.90), 12))

	return t
