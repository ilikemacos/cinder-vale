extends Control
## Cinder Vale launcher — a Lunar-Client-style front end: hero background,
## left nav (Home / Settings / About), a news panel, video settings, and a big
## PLAY button that loads the world. Built in code to keep the scene trivial.

const AMBER := Color(0.96, 0.76, 0.36)
const DARK := Color(0.06, 0.06, 0.07)
const PANEL := Color(0.09, 0.09, 0.11, 0.92)

var _content: Control
var _pages := {}

var _title_font: FontFile
var _mono_font: FontFile

func _ready() -> void:
	set_anchors_preset(Control.PRESET_FULL_RECT)
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
	theme = UITheme.build()
	_title_font = UITheme.title_font()
	_mono_font = UITheme.mono_font()
	_bg()
	_top_bar()
	_nav()
	_content_area()
	_play_button()
	_scanlines()
	_show_page("home")
	# Boot fade-in.
	modulate = Color(1, 1, 1, 0)
	create_tween().tween_property(self, "modulate:a", 1.0, 0.5)

func _scanlines() -> void:
	var sl := Control.new()
	sl.set_anchors_preset(Control.PRESET_FULL_RECT)
	sl.mouse_filter = Control.MOUSE_FILTER_IGNORE
	sl.draw.connect(func():
		var h := sl.size.y
		for y in range(0, int(h), 3):
			sl.draw_line(Vector2(0, y), Vector2(sl.size.x, y), Color(0, 0, 0, 0.06), 1.0))
	sl.resized.connect(func(): sl.queue_redraw())
	add_child(sl)

func _bg() -> void:
	var tex := load("res://assets/ui/menu_bg.png")
	if tex:
		var r := TextureRect.new()
		r.texture = tex
		r.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		r.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		r.set_anchors_preset(Control.PRESET_FULL_RECT)
		add_child(r)
	var dim := ColorRect.new()
	dim.color = Color(0.03, 0.03, 0.05, 0.55)
	dim.set_anchors_preset(Control.PRESET_FULL_RECT)
	dim.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(dim)
	# Bottom gradient bar for the play row.
	var grad := ColorRect.new()
	grad.color = Color(0, 0, 0, 0.5)
	grad.anchor_top = 1.0
	grad.anchor_right = 1.0
	grad.anchor_bottom = 1.0
	grad.offset_top = -140
	grad.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(grad)

func _top_bar() -> void:
	var title := Label.new()
	title.text = "CINDER  VALE"
	title.add_theme_font_override("font", _title_font)
	title.add_theme_font_size_override("font_size", 58)
	title.add_theme_color_override("font_color", AMBER)
	title.position = Vector2(48, 28)
	add_child(title)

	var sub := Label.new()
	sub.text = "> hearthlink terminal · pacific northwest wasteland_"
	sub.add_theme_font_size_override("font_size", 15)
	sub.add_theme_color_override("font_color", Color(0.5, 0.85, 0.5))
	sub.position = Vector2(52, 92)
	add_child(sub)

	var ver := Label.new()
	ver.text = "v0.4  ·  build: wasteland"
	ver.add_theme_font_size_override("font_size", 15)
	ver.add_theme_color_override("font_color", Color(0.7, 0.7, 0.72))
	ver.anchor_left = 1.0
	ver.anchor_right = 1.0
	ver.offset_left = -260
	ver.offset_top = 40
	add_child(ver)

func _nav() -> void:
	var col := VBoxContainer.new()
	col.position = Vector2(48, 150)
	col.add_theme_constant_override("separation", 10)
	add_child(col)
	for item in [["home", "▸  HOME"], ["settings", "▸  SETTINGS"], ["about", "▸  ABOUT"]]:
		var b := _nav_button(item[1])
		b.pressed.connect(_show_page.bind(item[0]))
		col.add_child(b)
	var quit := _nav_button("▸  QUIT")
	quit.pressed.connect(func(): get_tree().quit())
	col.add_child(quit)

func _nav_button(text: String) -> Button:
	var b := Button.new()
	b.text = text
	b.alignment = HORIZONTAL_ALIGNMENT_LEFT
	b.custom_minimum_size = Vector2(230, 40)
	b.flat = true
	b.add_theme_font_size_override("font_size", 20)
	b.add_theme_color_override("font_color", Color(0.85, 0.85, 0.86))
	b.add_theme_color_override("font_hover_color", AMBER)
	return b

func _content_area() -> void:
	_content = Control.new()
	_content.anchor_left = 0.0
	_content.offset_left = 320
	_content.offset_top = 150
	_content.anchor_right = 1.0
	_content.offset_right = -60
	_content.anchor_bottom = 1.0
	_content.offset_bottom = -170
	add_child(_content)
	_pages["home"] = _page_home()
	_pages["settings"] = _page_settings()
	_pages["about"] = _page_about()
	for p in _pages.values():
		p.set_anchors_preset(Control.PRESET_FULL_RECT)
		p.visible = false
		_content.add_child(p)

func _panel(title: String) -> Control:
	var bg := ColorRect.new()
	bg.color = PANEL
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	var box := VBoxContainer.new()
	box.set_anchors_preset(Control.PRESET_FULL_RECT)
	box.offset_left = 26
	box.offset_top = 22
	box.offset_right = -26
	box.offset_bottom = -22
	box.add_theme_constant_override("separation", 12)
	bg.add_child(box)
	# Terminal window header: a path line + the section title.
	var path := Label.new()
	path.text = "root@cindervale:~ $ cat " + title.to_lower().replace(" ", "_")
	path.add_theme_font_size_override("font_size", 13)
	path.add_theme_color_override("font_color", Color(0.5, 0.85, 0.5))
	box.add_child(path)
	var h := Label.new()
	h.text = title
	h.add_theme_font_override("font", _title_font)
	h.add_theme_font_size_override("font_size", 28)
	h.add_theme_color_override("font_color", AMBER)
	box.add_child(h)
	var rule := ColorRect.new()
	rule.color = Color(AMBER.r, AMBER.g, AMBER.b, 0.35)
	rule.custom_minimum_size = Vector2(0, 2)
	box.add_child(rule)
	# The ColorRect is the returned page root; the box is its child[0].
	bg.set_meta("box", box)
	return bg

func _page_home() -> Control:
	var page := _panel("DISPATCHES FROM CINDER VALE")
	var box: VBoxContainer = page.get_meta("box")
	for line in [
		"• The mill turbine is dead. Vale Salvage needs the exciter coil.",
		"• Ash Dogs raiders sighted at the quarry and the overpass camp.",
		"• Feral irradiated dogs nesting around the roadside clinic — approach armed.",
		"• Red Cordon holds the dam. They are not friendly to scavvers.",
		"",
		"Press PLAY to wake in the bus wreck on the highway.",
		"WASD move · LMB fire · R reload · Shift sprint · C crouch · V camera · Tab map",
	]:
		var l := Label.new()
		l.text = line
		l.add_theme_font_size_override("font_size", 17)
		l.add_theme_color_override("font_color", Color(0.86, 0.86, 0.87))
		box.add_child(l)
	return page

func _page_settings() -> Control:
	var page := _panel("VIDEO SETTINGS")
	var box: VBoxContainer = page.get_meta("box")

	box.add_child(_row("Resolution", _make_res_option()))
	box.add_child(_row("Quality (render scale)", _make_quality_option()))
	box.add_child(_row("World streaming", _make_streaming_option()))
	box.add_child(_row("Renderer", _make_renderer_option()))
	var full := CheckButton.new()
	full.text = "Fullscreen"
	full.add_theme_color_override("font_color", Color(0.86, 0.86, 0.87))
	full.toggled.connect(func(on):
		DisplayServer.window_set_mode(
			DisplayServer.WINDOW_MODE_FULLSCREEN if on else DisplayServer.WINDOW_MODE_WINDOWED))
	box.add_child(full)

	var note := Label.new()
	note.text = "Tuned for Apple M1 / 8 GB. Lower render scale if fps dips below 30."
	note.add_theme_font_size_override("font_size", 14)
	note.add_theme_color_override("font_color", Color(0.65, 0.65, 0.67))
	box.add_child(note)
	return page

func _row(label: String, control: Control) -> HBoxContainer:
	var h := HBoxContainer.new()
	h.add_theme_constant_override("separation", 18)
	var l := Label.new()
	l.text = label
	l.custom_minimum_size = Vector2(220, 0)
	l.add_theme_font_size_override("font_size", 17)
	l.add_theme_color_override("font_color", Color(0.86, 0.86, 0.87))
	h.add_child(l)
	h.add_child(control)
	return h

func _make_res_option() -> OptionButton:
	var o := OptionButton.new()
	var reslist := [Vector2i(1280, 720), Vector2i(1920, 1080), Vector2i(2560, 1440), Vector2i(3840, 2160)]
	var labels := ["720p (HD)", "1080p (Full HD)", "1440p (QHD)", "2160p (4K UHD)"]
	for i in reslist.size():
		o.add_item("%s   —   %d x %d" % [labels[i], reslist[i].x, reslist[i].y])
	o.selected = 1  # default 1080p
	o.item_selected.connect(func(i):
		DisplayServer.window_set_size(reslist[i])
		var scr := DisplayServer.screen_get_size()
		DisplayServer.window_set_position((scr - reslist[i]) / 2))
	return o

func _make_streaming_option() -> OptionButton:
	# Low streams one tile at a time (least RAM); High keeps the whole valley
	# resident (no streaming hitches, more RAM). Real trade-off, plainly named.
	var o := OptionButton.new()
	o.add_item("Low  (stream tiles)")
	o.add_item("High (whole valley)")
	o.selected = 0
	WorldState.ram_budget_gb = 4
	o.item_selected.connect(func(i): WorldState.ram_budget_gb = (8 if i == 1 else 4))
	return o

func _make_quality_option() -> OptionButton:
	var o := OptionButton.new()
	var scales := [0.7, 0.85, 1.0]
	o.add_item("Low")
	o.add_item("Medium")
	o.add_item("High")
	o.selected = 2
	o.item_selected.connect(func(i):
		get_viewport().scaling_3d_scale = scales[i])
	return o

func _make_renderer_option() -> OptionButton:
	# One backend is active at a time — there is no OpenGL+Vulkan hybrid. On
	# Apple Silicon "Forward+" runs on Metal. Switching relaunches the game.
	var o := OptionButton.new()
	o.add_item("Metal (Forward+) — recommended")
	o.add_item("OpenGL (Compatibility) — fallback")
	var cur := RenderingServer.get_rendering_device() != null
	o.selected = 0 if cur else 1
	o.item_selected.connect(func(i):
		var args := PackedStringArray()
		if i == 0:
			args = PackedStringArray(["--rendering-method", "forward_plus", "--rendering-driver", "metal"])
		else:
			args = PackedStringArray(["--rendering-method", "gl_compatibility", "--rendering-driver", "opengl3"])
		OS.create_instance(args)
		get_tree().quit())
	return o

func _page_about() -> Control:
	var page := _panel("ABOUT")
	var box: VBoxContainer = page.get_meta("box")
	for line in [
		"Cinder Vale — an original open-world RPG built in Godot 4 on Metal.",
		"Grounded, morally grey survival in a ruined mill valley.",
		"",
		"Original IP. Not affiliated with any existing franchise.",
		"Characters & animations: Mixamo. Everything else procedural.",
	]:
		var l := Label.new()
		l.text = line
		l.add_theme_font_size_override("font_size", 17)
		l.add_theme_color_override("font_color", Color(0.86, 0.86, 0.87))
		box.add_child(l)
	return page

func _show_page(id: String) -> void:
	for k in _pages:
		_pages[k].visible = (k == id)

func _play_button() -> void:
	var play := Button.new()
	play.text = "►   PLAY"
	play.custom_minimum_size = Vector2(320, 74)
	play.add_theme_font_size_override("font_size", 34)
	play.anchor_left = 0.5
	play.anchor_right = 0.5
	play.anchor_top = 1.0
	play.anchor_bottom = 1.0
	play.offset_left = -160
	play.offset_right = 160
	play.offset_top = -118
	play.offset_bottom = -44
	play.add_theme_font_override("font", _title_font)
	play.add_theme_color_override("font_color", Color(0.05, 0.05, 0.05))
	play.add_theme_color_override("font_hover_color", Color(0, 0, 0))
	var sb := StyleBoxFlat.new()
	sb.bg_color = AMBER
	sb.set_corner_radius_all(6)
	var sbh := StyleBoxFlat.new()
	sbh.bg_color = Color(1.0, 0.85, 0.5)
	sbh.set_corner_radius_all(6)
	play.add_theme_stylebox_override("normal", sb)
	play.add_theme_stylebox_override("hover", sbh)
	play.add_theme_stylebox_override("pressed", sb)
	play.pressed.connect(_on_play)
	add_child(play)

func _on_play() -> void:
	# Fade to black, then boot the world.
	var fade := ColorRect.new()
	fade.color = Color(0, 0, 0, 0)
	fade.set_anchors_preset(Control.PRESET_FULL_RECT)
	fade.mouse_filter = Control.MOUSE_FILTER_STOP
	add_child(fade)
	var tw := create_tween()
	tw.tween_property(fade, "color:a", 1.0, 0.45)
	tw.tween_callback(func(): get_tree().change_scene_to_file("res://scenes/world/World.tscn"))
