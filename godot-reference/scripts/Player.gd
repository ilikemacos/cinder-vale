extends CharacterBody3D
## Cinder Vale player controller.
## First/third person toggle (V). WASD move, Space jump, Shift sprint, C crouch.
## Graybox milestone: proves locomotion + camera before Mixamo rig lands.

@export var walk_speed := 4.0
@export var sprint_speed := 7.0
@export var crouch_speed := 2.0
@export var jump_velocity := 8.0
@export var mouse_sensitivity := 0.0025

var _third_person := true
var _crouching := false
var _yaw := 0.0
var _pitch := 0.0

@onready var _pivot: Node3D = $CamPivot
@onready var _cam_first: Camera3D = $CamPivot/FirstPerson
@onready var _cam_third: Camera3D = $CamPivot/SpringArm3D/ThirdPerson
@onready var _body: Node3D = $Body
@onready var _anim: AnimationPlayer = $Body/PlayerCharacter/AnimationPlayer

var _cur_anim := ""
var _muzzle_flash: MeshInstance3D
var _flash_light: OmniLight3D
var _flash_timer := 0.0

# --- combat ---
@export var max_health := 100.0
@export var attack_damage := 25.0
@export var attack_range := 80.0
@export var fire_interval := 0.11        # full-auto cadence
@export var mag_size := 30
@export var reserve_ammo := 120
@export var reload_time := 1.9
var health := 100.0
var alive := true
var ammo := 30
var reserve := 120
var reloading := false
var _fire_cd := 0.0
var _reload_t := 0.0
var _recoil := 0.0
signal health_changed(cur: float, max: float)
signal damaged()
signal ammo_changed(mag: int, reserve: int, reloading: bool)

func _ready() -> void:
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	_apply_camera()
	_equip_rifle()
	_play("mixamo/Rifle_Idle")
	health = max_health
	ammo = mag_size
	reserve = reserve_ammo
	health_changed.emit(health, max_health)
	ammo_changed.emit(ammo, reserve, reloading)

func _equip_rifle() -> void:
	# Parent the rifle to the right-hand bone so the rifle animations hold it.
	var skel := _find_skeleton($Body/PlayerCharacter)
	if skel == null:
		return
	var attach := BoneAttachment3D.new()
	attach.bone_name = "mixamorig_RightHand"
	skel.add_child(attach)
	var rifle := WeaponFactory.build_rifle()
	# Tuned so the grip sits in the palm and the barrel points forward.
	rifle.transform = Transform3D(Basis.from_euler(Vector3(deg_to_rad(-90), deg_to_rad(0), deg_to_rad(90))), Vector3(0, 0.0, 0.0))
	attach.add_child(rifle)
	_muzzle_flash = rifle.get_node("Muzzle/Flash")
	_flash_light = rifle.get_node("Muzzle/FlashLight")

func _find_skeleton(n: Node) -> Skeleton3D:
	if n is Skeleton3D:
		return n
	for c in n.get_children():
		var r := _find_skeleton(c)
		if r:
			return r
	return null

func _play(clip: String, force := false) -> void:
	if not force and _cur_anim == clip:
		return
	if _anim.has_animation(clip):
		_anim.play(clip, 0.12)
		_cur_anim = clip

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		_yaw -= event.relative.x * mouse_sensitivity
		_pitch = clamp(_pitch - event.relative.y * mouse_sensitivity, -1.4, 1.4)
		rotation.y = _yaw
		_pivot.rotation.x = _pitch
	if event.is_action_pressed("toggle_camera"):
		_third_person = not _third_person
		_apply_camera()
	if event.is_action_pressed("reload"):
		_start_reload()
	if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
		Input.mouse_mode = Input.MOUSE_MODE_VISIBLE

func _apply_camera() -> void:
	_cam_third.current = _third_person
	_cam_first.current = not _third_person
	# Hide own body head-on in first person to avoid clipping.
	_body.visible = _third_person

func _physics_process(delta: float) -> void:
	_fire_cd = maxf(0.0, _fire_cd - delta)
	_recoil = lerpf(_recoil, 0.0, clampf(delta * 9.0, 0.0, 1.0))
	_pivot.rotation.x = _pitch + _recoil
	if _flash_timer > 0.0:
		_flash_timer -= delta
		if _flash_timer <= 0.0:
			_show_flash(false)
	# Reload timer.
	if reloading:
		_reload_t -= delta
		if _reload_t <= 0.0:
			_finish_reload()
	# Full-auto fire while LMB held.
	if alive and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		_attack()
	if not is_on_floor():
		velocity.y -= get_gravity().y * delta if false else 24.0 * delta

	if not alive:
		velocity.x = 0
		velocity.z = 0
		move_and_slide()
		return

	if Input.is_action_pressed("crouch"):
		_crouching = true
	elif not Input.is_action_pressed("crouch"):
		_crouching = false

	if Input.is_action_just_pressed("jump") and is_on_floor():
		velocity.y = jump_velocity

	# Explicit WASD: W forward (-Z local), S back, A left, D right.
	var ix := Input.get_action_strength("move_right") - Input.get_action_strength("move_left")
	var iz := Input.get_action_strength("move_back") - Input.get_action_strength("move_forward")
	var direction := (transform.basis * Vector3(ix, 0, iz)).normalized()

	var speed := walk_speed
	if _crouching:
		speed = crouch_speed
	elif Input.is_action_pressed("sprint"):
		speed = sprint_speed

	if direction:
		velocity.x = direction.x * speed
		velocity.z = direction.z * speed
	else:
		velocity.x = move_toward(velocity.x, 0, speed)
		velocity.z = move_toward(velocity.z, 0, speed)

	move_and_slide()
	_update_anim(direction, speed)

func _active_camera() -> Camera3D:
	return _cam_third if _third_person else _cam_first

func _attack() -> void:
	if not alive or reloading or _fire_cd > 0.0:
		return
	if ammo <= 0:
		_start_reload()  # auto-reload on empty trigger pull
		return
	_fire_cd = fire_interval
	ammo -= 1
	ammo_changed.emit(ammo, reserve, reloading)
	_play("mixamo/Firing_Rifle", true)
	_show_flash(true)
	_flash_timer = 0.05
	_recoil += 0.012   # upward kick
	var cam := _active_camera()
	var from := cam.global_position
	var to := from - cam.global_transform.basis.z * attack_range
	var params := PhysicsRayQueryParameters3D.create(from, to, 1 | 4)  # terrain + enemy
	params.exclude = [self]
	var hit := get_world_3d().direct_space_state.intersect_ray(params)
	if hit and hit.collider and hit.collider.is_in_group("enemy"):
		hit.collider.take_damage(attack_damage)

func _start_reload() -> void:
	if reloading or ammo >= mag_size or reserve <= 0:
		return
	reloading = true
	_reload_t = reload_time
	_play("mixamo/Reloading", true)
	ammo_changed.emit(ammo, reserve, reloading)

func _finish_reload() -> void:
	reloading = false
	var need := mag_size - ammo
	var take := mini(need, reserve)
	ammo += take
	reserve -= take
	ammo_changed.emit(ammo, reserve, reloading)

func _show_flash(on: bool) -> void:
	if _muzzle_flash:
		_muzzle_flash.visible = on
	if _flash_light:
		_flash_light.light_energy = 4.0 if on else 0.0

func take_damage(amount: float) -> void:
	if not alive:
		return
	health = maxf(0.0, health - amount)
	health_changed.emit(health, max_health)
	damaged.emit()
	if health <= 0.0:
		_die()

func _die() -> void:
	alive = false
	_play("mixamo/Dying")
	Input.mouse_mode = Input.MOUSE_MODE_VISIBLE

func _update_anim(direction: Vector3, speed: float) -> void:
	if reloading:
		_play("mixamo/Reloading")
		return
	# Hold the firing pose through its cooldown before resuming locomotion.
	if _cur_anim == "mixamo/Firing_Rifle" and _fire_cd > 0.0:
		return
	var moving := direction.length() > 0.1 and Vector2(velocity.x, velocity.z).length() > 0.3
	if _crouching:
		_play("mixamo/Crouched_Walking" if moving else "mixamo/Crouch_Idle")
	elif not is_on_floor():
		_play("mixamo/Jump")
	elif moving:
		_play("mixamo/Running" if speed >= sprint_speed - 0.1 else "mixamo/Rifle_Walk")
	else:
		_play("mixamo/Rifle_Idle")
