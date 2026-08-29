extends CharacterBody3D
class_name Dog
## Irradiated feral dog. Procedural low-poly quadruped (no rigged mesh in the
## asset set) with code-driven leg gait. Fast, fragile, lunges and bites.
## Sickly green with a faint emissive glow. In the "enemy" group so the
## player's hitscan, HUD alert and combat all treat it like any raider.

enum State { IDLE, CHASE, ATTACK, DEAD }

@export var max_health := 28.0
@export var move_speed := 6.2
@export var detect_range := 30.0
@export var attack_range := 2.2
@export var attack_damage := 9.0
@export var attack_interval := 1.0

var health := 28.0
var _state := State.IDLE
var _player: Node3D
var _cooldown := 0.0
var _gait := 0.0
var _die_t := 0.0
var _legs: Array[Node3D] = []
var _model: Node3D

const HIDE := Color(0.16, 0.19, 0.10)      # sickly irradiated green-brown
const HIDE2 := Color(0.10, 0.13, 0.07)

func _ready() -> void:
	add_to_group("enemy")
	health = max_health
	_player = get_tree().get_first_node_in_group("player")
	_build()

func _build() -> void:
	collision_layer = 4     # enemy hit layer (matches raiders)
	collision_mask = 1      # collide with terrain
	var col := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(0.6, 0.7, 1.3)
	col.shape = box
	col.position = Vector3(0, 0.55, 0)
	add_child(col)

	_model = Node3D.new()
	add_child(_model)
	_part(Vector3(0, 0.62, 0), Vector3(0.5, 0.42, 1.1), HIDE)          # torso
	_part(Vector3(0, 0.72, -0.7), Vector3(0.36, 0.36, 0.4), HIDE)     # neck/head base
	_part(Vector3(0, 0.72, -0.95), Vector3(0.26, 0.26, 0.34), HIDE2)  # snout/head
	_part(Vector3(0, 0.62, -1.14), Vector3(0.12, 0.12, 0.12), HIDE2)  # nose
	_part(Vector3(-0.12, 0.92, -0.72), Vector3(0.1, 0.16, 0.06), HIDE2) # ear L
	_part(Vector3(0.12, 0.92, -0.72), Vector3(0.1, 0.16, 0.06), HIDE2)  # ear R
	# Tail as an angled part.
	var tail := _part(Vector3(0, 0.78, 0.66), Vector3(0.1, 0.1, 0.5), HIDE2)
	tail.rotation.x = deg_to_rad(-35)
	# Legs (animated pivots at the hips).
	_legs = []
	_legs.append(_leg(Vector3(-0.22, 0.5, -0.42)))  # front-left
	_legs.append(_leg(Vector3(0.22, 0.5, -0.42)))   # front-right
	_legs.append(_leg(Vector3(-0.22, 0.5, 0.42)))   # back-left
	_legs.append(_leg(Vector3(0.22, 0.5, 0.42)))    # back-right

func _part(pos: Vector3, size: Vector3, col: Color) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = size
	mi.mesh = bm
	mi.position = pos
	mi.material_override = _mat(col)
	_model.add_child(mi)
	return mi

func _leg(hip: Vector3) -> Node3D:
	var pivot := Node3D.new()
	pivot.position = hip
	_model.add_child(pivot)
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(0.12, 0.5, 0.12)
	mi.mesh = bm
	mi.position = Vector3(0, -0.25, 0)   # extends down from the hip pivot
	mi.material_override = _mat(HIDE2)
	pivot.add_child(mi)
	return pivot

func _mat(col: Color) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = 0.9
	m.emission_enabled = true
	m.emission = Color(0.10, 0.35, 0.06)   # radioactive green glow
	m.emission_energy_multiplier = 0.25
	return m

func _physics_process(delta: float) -> void:
	if not is_on_floor():
		velocity.y -= 24.0 * delta
	else:
		velocity.y = 0.0

	if _state == State.DEAD:
		velocity.x = 0
		velocity.z = 0
		move_and_slide()
		_die_t -= delta
		if _die_t <= 0.0:
			queue_free()
		return

	_cooldown = maxf(0.0, _cooldown - delta)
	if _player == null or not is_instance_valid(_player):
		return

	var to := _player.global_position - global_position
	to.y = 0
	var dist := to.length()
	var moving := false

	if dist < detect_range:
		if to.length() > 0.05:
			rotation.y = atan2(to.x, to.z)
		if dist <= attack_range:
			_state = State.ATTACK
		else:
			_state = State.CHASE
	else:
		_state = State.IDLE

	match _state:
		State.CHASE:
			var dir := to.normalized()
			velocity.x = dir.x * move_speed
			velocity.z = dir.z * move_speed
			moving = true
		State.ATTACK:
			velocity.x = 0
			velocity.z = 0
			if _cooldown <= 0.0:
				if _player.has_method("take_damage") and dist <= attack_range + 0.4:
					_player.take_damage(attack_damage)
				_cooldown = attack_interval
		State.IDLE:
			velocity.x = move_toward(velocity.x, 0, move_speed)
			velocity.z = move_toward(velocity.z, 0, move_speed)

	move_and_slide()
	_animate_gait(delta, moving)

func _animate_gait(delta: float, moving: bool) -> void:
	if _legs.is_empty():
		return
	var target_speed := 14.0 if moving else 0.0
	_gait += delta * target_speed
	var amp := 0.7 if moving else 0.0
	for i in _legs.size():
		var phase := _gait + (PI if i in [1, 2] else 0.0)  # diagonal trot
		_legs[i].rotation.x = sin(phase) * amp
	# Slight body bob when running.
	if _model:
		_model.position.y = absf(sin(_gait)) * 0.05 * (1.0 if moving else 0.0)

func take_damage(amount: float) -> void:
	if _state == State.DEAD:
		return
	health -= amount
	if health <= 0.0:
		_die()

func _die() -> void:
	_state = State.DEAD
	_die_t = 3.0
	collision_layer = 0
	if _model:
		_model.rotation.z = deg_to_rad(80)   # keel over
		_model.position.y = -0.2
