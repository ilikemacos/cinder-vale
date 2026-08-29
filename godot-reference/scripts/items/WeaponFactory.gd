extends RefCounted
class_name WeaponFactory
## Procedural low-poly assault rifle built from boxes, plus a muzzle marker and
## a hideable muzzle-flash. Barrel points along -Z (local forward) so it aligns
## with the hand bone. Swap for a real weapon model later.

static func build_rifle() -> Node3D:
	var root := Node3D.new()
	root.name = "Rifle"
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.06, 0.06, 0.07)
	mat.metallic = 0.6
	mat.roughness = 0.5

	var st := SurfaceTool.new()
	st.begin(Mesh.PRIMITIVE_TRIANGLES)
	_box(st, Vector3(0, 0, 0.02), Vector3(0.06, 0.09, 0.46))       # receiver
	_box(st, Vector3(0, 0.02, -0.42), Vector3(0.032, 0.032, 0.5))  # barrel + handguard
	_box(st, Vector3(0, -0.14, 0.06), Vector3(0.05, 0.2, 0.09))    # magazine (curved-ish)
	_box(st, Vector3(0, -0.09, 0.16), Vector3(0.045, 0.11, 0.05))  # pistol grip
	_box(st, Vector3(0, 0.0, 0.34), Vector3(0.05, 0.10, 0.22))     # stock
	_box(st, Vector3(0, 0.09, -0.05), Vector3(0.012, 0.03, 0.14))  # front sight/rail
	st.generate_normals()
	var mi := MeshInstance3D.new()
	mi.mesh = st.commit()
	mi.material_override = mat
	root.add_child(mi)

	# Muzzle position (barrel tip) + flash.
	var muzzle := Node3D.new()
	muzzle.name = "Muzzle"
	muzzle.position = Vector3(0, 0.02, -0.68)
	root.add_child(muzzle)

	var flash := MeshInstance3D.new()
	flash.name = "Flash"
	var fm := SphereMesh.new()
	fm.radius = 0.09
	fm.height = 0.18
	flash.mesh = fm
	var fmat := StandardMaterial3D.new()
	fmat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	fmat.albedo_color = Color(1.0, 0.85, 0.4)
	fmat.emission_enabled = true
	fmat.emission = Color(1.0, 0.7, 0.25)
	fmat.emission_energy_multiplier = 4.0
	flash.material_override = fmat
	flash.visible = false
	muzzle.add_child(flash)

	var light := OmniLight3D.new()
	light.name = "FlashLight"
	light.light_color = Color(1.0, 0.8, 0.4)
	light.light_energy = 0.0
	light.omni_range = 6.0
	muzzle.add_child(light)
	return root

static func _box(st: SurfaceTool, c: Vector3, size: Vector3) -> void:
	var h := size * 0.5
	var v := [
		c + Vector3(-h.x, -h.y, -h.z), c + Vector3(h.x, -h.y, -h.z),
		c + Vector3(h.x, h.y, -h.z), c + Vector3(-h.x, h.y, -h.z),
		c + Vector3(-h.x, -h.y, h.z), c + Vector3(h.x, -h.y, h.z),
		c + Vector3(h.x, h.y, h.z), c + Vector3(-h.x, h.y, h.z),
	]
	for f in [[0,1,2,3],[5,4,7,6],[4,0,3,7],[1,5,6,2],[3,2,6,7],[4,5,1,0]]:
		st.add_vertex(v[f[0]]); st.add_vertex(v[f[1]]); st.add_vertex(v[f[2]])
		st.add_vertex(v[f[0]]); st.add_vertex(v[f[2]]); st.add_vertex(v[f[3]])
