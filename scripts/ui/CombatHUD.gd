extends CanvasLayer
## Combat HUD: crosshair, health bar, hit flash, enemy-nearby alert, death text.
## Amber/dark HearthLink styling. Reads the player and listens to its signals.

@export var player_path: NodePath
var _player: Node
@onready var _bar: ProgressBar = $HP
@onready var _flash: ColorRect = $Flash
@onready var _cross: Label = $Crosshair
@onready var _alert: Label = $Alert
@onready var _dead: Label = $Dead
@onready var _ammo: Label = $Ammo

func _ready() -> void:
	_player = get_node(player_path)
	_flash.color = Color(0.6, 0.05, 0.05, 0.0)
	_dead.visible = false
	if _player:
		_player.health_changed.connect(_on_health)
		_player.damaged.connect(_on_damaged)
		_player.ammo_changed.connect(_on_ammo)
		_on_health(_player.health, _player.max_health)
		_on_ammo(_player.ammo, _player.reserve, _player.reloading)

func _on_ammo(mag: int, reserve: int, reloading: bool) -> void:
	if reloading:
		_ammo.text = "RELOADING…"
	else:
		_ammo.text = "%d / %d" % [mag, reserve]
	_ammo.modulate = Color(0.9, 0.35, 0.25) if (mag == 0 and not reloading) else Color(0.95, 0.8, 0.4)

func _on_health(cur: float, mx: float) -> void:
	_bar.max_value = mx
	_bar.value = cur
	if cur <= 0.0:
		_dead.visible = true

func _on_damaged() -> void:
	_flash.color = Color(0.6, 0.05, 0.05, 0.45)
	var tw := create_tween()
	tw.tween_property(_flash, "color:a", 0.0, 0.4)

func _process(_d: float) -> void:
	# Cheap "enemy nearby" alert.
	if _player == null:
		return
	var near := 0
	for e in get_tree().get_nodes_in_group("enemy"):
		if e.global_position.distance_to(_player.global_position) < 34.0:
			near += 1
	_alert.text = "! %d hostile%s nearby" % [near, "s" if near != 1 else ""] if near > 0 else ""
