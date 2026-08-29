extends SceneTree
## Assembles a rigged character from Mixamo FBX imports.
## Takes the character scene (mesh + Skeleton3D) and every animation FBX,
## pulls each clip into one shared AnimationLibrary named "mixamo",
## and saves a ready-to-instance PackedScene with an AnimationPlayer.
##
## Mixamo rigs share "mixamorig:*" bone names, so clips retarget by name
## with no bone remap needed. Run via:
##   Godot --headless --path . --script res://scripts/tools/build_character.gd

const CHAR_DIR := "res://assets_src/mixamo_assets/characters/"
const ANIM_DIR := "res://assets_src/mixamo_assets/animations/"
const OUT_DIR := "res://assets/characters/"

# character source file -> output scene name
const CHARACTERS := {
	"Gas_Mask.fbx": "PlayerCharacter",
	"Kachujin G Rosales.fbx": "Companion",
	"Brute.fbx": "RaiderBrute",
	"Swat.fbx": "RaiderSwat",
}

const ANIMS := [
	"Idle", "Walking", "Running", "Jump", "Crouch Idle", "Crouched Walking",
	"Rifle Idle", "Rifle Walk", "Firing Rifle", "Aiming", "Reloading",
	"Hit Reaction", "Dying", "Picking Up", "Talking", "Sitting Idle",
	"Standing Melee Attack Downward", "Throwing",
]

func _initialize() -> void:
	_run()
	quit()

func _run() -> void:
	var lib := _build_anim_library()
	if lib == null:
		push_error("No animations collected — aborting.")
		return
	print("AnimationLibrary built with %d clips." % lib.get_animation_list().size())
	var da := DirAccess.open("res://")
	da.make_dir_recursive(OUT_DIR.trim_prefix("res://"))
	for src in CHARACTERS:
		_build_one(src, CHARACTERS[src], lib)
	print("Character assembly complete.")

func _build_anim_library() -> AnimationLibrary:
	var lib := AnimationLibrary.new()
	for name_v in ANIMS:
		var name: String = name_v
		var path: String = ANIM_DIR + name + ".fbx"
		if not ResourceLoader.exists(path):
			push_warning("Missing anim: " + path)
			continue
		var packed: PackedScene = load(path)
		if packed == null:
			push_warning("Failed to load: " + path)
			continue
		var scene := packed.instantiate()
		var ap := _find_anim_player(scene)
		if ap == null:
			push_warning("No AnimationPlayer in " + path)
			scene.free()
			continue
		# Mixamo single-take FBX exposes the clip; grab the first non-RESET.
		for clip_name in ap.get_animation_list():
			if clip_name == "RESET":
				continue
			var anim: Animation = ap.get_animation(clip_name).duplicate()
			var looped: bool = name in ["Idle", "Walking", "Running", "Rifle Idle",
				"Rifle Walk", "Crouch Idle", "Crouched Walking", "Aiming",
				"Talking", "Sitting Idle"]
			anim.loop_mode = Animation.LOOP_LINEAR if looped else Animation.LOOP_NONE
			var key: String = name.replace(" ", "_")
			lib.add_animation(key, anim)
			break
		scene.free()
	return lib

func _build_one(src_file: String, out_name: String, lib: AnimationLibrary) -> void:
	var path := CHAR_DIR + src_file
	if not ResourceLoader.exists(path):
		push_warning("Missing character: " + path)
		return
	var packed: PackedScene = load(path)
	var root := packed.instantiate()
	var skel := _find_skeleton(root)
	if skel == null:
		push_warning("No Skeleton3D in " + src_file)
		root.free()
		return
	# Remove any bundled AnimationPlayer; we attach the shared library.
	var old := _find_anim_player(root)
	if old:
		old.get_parent().remove_child(old)
		old.free()
	var ap := AnimationPlayer.new()
	ap.name = "AnimationPlayer"
	ap.add_animation_library("mixamo", lib)
	root.add_child(ap)
	ap.owner = root
	root.name = out_name
	var packed_out := PackedScene.new()
	packed_out.pack(root)
	var out_path := OUT_DIR + out_name + ".tscn"
	var err := ResourceSaver.save(packed_out, out_path)
	print("  %s -> %s (%s)" % [src_file, out_path, error_string(err)])
	root.free()

func _find_skeleton(n: Node) -> Skeleton3D:
	if n is Skeleton3D:
		return n
	for c in n.get_children():
		var r := _find_skeleton(c)
		if r:
			return r
	return null

func _find_anim_player(n: Node) -> AnimationPlayer:
	if n is AnimationPlayer:
		return n
	for c in n.get_children():
		var r := _find_anim_player(c)
		if r:
			return r
	return null
