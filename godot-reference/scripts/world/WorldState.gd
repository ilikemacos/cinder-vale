extends Node
## Autoload. Global run state: which POIs the player has discovered,
## for the HearthLink map + fast travel. Emits on discovery so UI updates.

signal discovered(id: String)

var found := {}   # id -> true
var ram_budget_gb := 4   # advisory; >=6 keeps the whole region resident

func discover(id: String) -> void:
	if found.has(id):
		return
	found[id] = true
	discovered.emit(id)

func is_found(id: String) -> bool:
	return found.has(id)

func poi_by_id(id: String) -> Dictionary:
	for p in WorldConfig.POIS:
		if p["id"] == id:
			return p
	return {}
