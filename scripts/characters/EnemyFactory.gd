extends RefCounted
class_name EnemyFactory
## Builds a raider from an assembled Mixamo character scene + Enemy.gd +
## a capsule body. Enemies sit on collision layer 4 so the player's hitscan
## (and only that) can register hits; they collide with terrain (layer 1).

const BRUTE := "res://assets/characters/RaiderBrute.tscn"
const SWAT := "res://assets/characters/RaiderSwat.tscn"

static func build(ranged: bool) -> CharacterBody3D:
	var body := CharacterBody3D.new()
	body.collision_layer = 4          # "enemy" hit layer
	body.collision_mask = 1           # collide with terrain only
	var script := load("res://scripts/characters/Enemy.gd")
	body.set_script(script)

	var col := CollisionShape3D.new()
	var cap := CapsuleShape3D.new()
	cap.radius = 0.4
	cap.height = 1.8
	col.shape = cap
	col.position = Vector3(0, 0.9, 0)
	body.add_child(col)

	var mesh_scene: PackedScene = load(SWAT if ranged else BRUTE)
	var mesh := mesh_scene.instantiate()
	body.add_child(mesh)

	body.set("is_ranged", ranged)
	body.configure(ranged)
	return body
