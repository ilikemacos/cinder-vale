extends CharacterBody3D
class_name Enemy
## Raider AI: idle → chase → attack → death. Drives the shared Mixamo
## animation library on the instanced character mesh. Melee (Brute) closes and
## swings; ranged (Swat) holds at distance and fires a hitscan. Max ~6 in a
## fight per the M1 budget. Placeholder combat until the full gunplay milestone.

enum State { IDLE, CHASE, ATTACK, DEAD }

@export var max_health := 60.0
@export var move_speed := 3.6
@export var detect_range := 34.0
@export var attack_range := 2.4
@export var attack_damage := 12.0
@export var attack_interval := 1.4
@export var is_ranged := false

var health := 60.0
var _state := State.IDLE
var _player: Node3D
var _anim: AnimationPlayer
var _cooldown := 0.0
var _cur := ""
var _die_timer := 0.0

func configure(ranged: bool) -> void:
	is_ranged = ranged
	if ranged:
		move_speed = 3.0
		attack_range = 22.0
		attack_damage = 8.0
		attack_interval = 1.8
		max_health = 45.0
	else:
		move_speed = 4.2
		attack_range = 2.6
		attack_damage = 14.0
		max_health = 70.0

func _ready() -> void:
	add_to_group("enemy")
	health = max_health
	_player = get_tree().get_first_node_in_group("player")
	# The instanced character scene carries the AnimationPlayer.
	_anim = _find_anim(self)
	_play("Idle")

func _physics_process(delta: float) -> void:
	if not is_on_floor():
		velocity.y -= 24.0 * delta
	else:
		velocity.y = 0.0

	if _state == State.DEAD:
		velocity.x = 0
		velocity.z = 0
		move_and_slide()
		_die_timer -= delta
		if _die_timer <= 0.0:
			queue_free()
		return

	_cooldown = maxf(0.0, _cooldown - delta)
	if _player == null or not is_instance_valid(_player):
		return

	var to := _player.global_position - global_position
	to.y = 0
	var dist := to.length()

	if dist < detect_range:
		_face(to)
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
			_play("Running")
		State.ATTACK:
			velocity.x = 0
			velocity.z = 0
			_play("Firing_Rifle" if is_ranged else "Standing_Melee_Attack_Downward")
			if _cooldown <= 0.0:
				_do_attack(dist)
				_cooldown = attack_interval
		State.IDLE:
			velocity.x = move_toward(velocity.x, 0, move_speed)
			velocity.z = move_toward(velocity.z, 0, move_speed)
			_play("Rifle_Idle" if is_ranged else "Idle")

	move_and_slide()

func _do_attack(dist: float) -> void:
	# Landed hit if still in range (melee) or player roughly in front (ranged).
	if _player.has_method("take_damage") and dist <= attack_range + 0.5:
		_player.take_damage(attack_damage)

func take_damage(amount: float) -> void:
	if _state == State.DEAD:
		return
	health -= amount
	if health <= 0.0:
		_die()
	else:
		_play("Hit_Reaction", true)

func _die() -> void:
	_state = State.DEAD
	_die_timer = 4.0
	collision_layer = 0
	set_collision_mask_value(1, true)  # keep falling onto terrain only
	_play("Dying", true)

func _face(to: Vector3) -> void:
	if to.length() > 0.05:
		rotation.y = atan2(to.x, to.z)

func _play(clip: String, force := false) -> void:
	if _anim == null:
		return
	var key := "mixamo/" + clip
	if not force and _cur == key:
		return
	if _anim.has_animation(key):
		_anim.play(key, 0.15)
		_cur = key

func _find_anim(n: Node) -> AnimationPlayer:
	if n is AnimationPlayer:
		return n
	for c in n.get_children():
		var r := _find_anim(c)
		if r:
			return r
	return null
