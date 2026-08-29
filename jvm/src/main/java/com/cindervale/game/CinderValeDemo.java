package com.cindervale.game;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.core.Engine;
import com.cindervale.engine.core.Game;
import com.cindervale.engine.scene.Scene;
import com.cindervale.engine.input.FpsCamera;
import com.cindervale.game.enemies.Enemy;
import com.cindervale.game.enemies.Spawner;
import com.cindervale.game.items.Rifle;
import com.cindervale.game.world.Landmarks;
import com.cindervale.game.world.Road;
import com.cindervale.game.world.Scatter;
import com.cindervale.game.world.Terrain;
import com.cindervale.game.world.WorldConfig;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Quaternion;
import com.jme3.scene.Node;

/**
 * Cinder Vale wasteland — the first proper Fallout-shaped scene on the JVM
 * engine: 200×200 m rolling terrain with Poly Haven dry-ground PBR, hundreds
 * of scattered scanned props (barrels, boulders, cans, boxes, wheel rims,
 * trash cans), procedural dead trees, and a distant mountain silhouette
 * backdrop, all under the overcast HDRI sky.
 *
 * Deterministic (seeded RNG) so the layout is stable across restarts.
 */
public final class CinderValeDemo implements Game {

    private Spawner spawner;
    private Engine engine;
    private Scene scene;
    private float playerHp = 100f;

    @Override
    public void init(Engine engine, Scene scene) {
        this.engine = engine;
        this.scene = scene;
        Assets a = scene.assets;

        // Sky: overcast HDRI (falls back to the rural asphalt one if missing).
        boolean sky = scene.setSkyEquirect(
                "assets/env/cinder-vale-env-art/hdris/overcast_soil_puresky_2k.hdr");
        if (!sky) scene.setSkyEquirect(
                "assets/env/cinder-vale-env-art/hdris/rural_asphalt_road_2k.hdr");

        scene.addDefaultLighting();
        // Cinder Vale pitch: damp overcast Pacific Northwest, ~15y post-exchange.
        // NOT the Commonwealth. Cool near-neutral sun (weak sky diffuser through
        // cloud), cool grey-green ambient (wet moss + mineral haze), silver-grey
        // depth fog. Kills the amber golden-hour drift the earlier grade had.
        if (scene.sun() != null) {
            scene.sun().setColor(new ColorRGBA(0.85f, 0.88f, 0.92f, 1f));
        }
        if (scene.ambient() != null) {
            scene.ambient().setColor(new ColorRGBA(0.50f, 0.56f, 0.58f, 1f));
        }
        scene.enableFog(new ColorRGBA(0.70f, 0.74f, 0.74f, 1f), 1.3f, 260f);

        // Tell the engine how to look up ground height for terrain-clamp.
        scene.groundHeight = (x, z) -> Terrain.groundY(x, z);

        // Terrain — 200×200 m, ~2 m quads = 100×100 grid = 10k quads / 20k tris.
        Geometry terrain = Terrain.build(a, WorldConfig.WORLD_HALF, 2f);
        scene.add(terrain);

        // Cracked-asphalt highway crossing the valley N-S through the centre.
        scene.add(Road.build(a, WorldConfig.WORLD_HALF, 7f, 3f));

        // Named landmarks — a real river in the trench, the mill smokestack
        // east of the highway, a water tower where the town will grow.
        scene.add(Landmarks.riverWater(a));
        scene.add(Landmarks.mill(a, scene));
        scene.add(Landmarks.waterTower(a, scene, -14f, 8f));   // town centre

        // Wasteland dressing: real Poly Haven props + dead trees + mountains.
        Scatter.populate(scene, a);

        // Camera spawn near the "bus wreck" spot, eye height above ground.
        Vector3f spawn = new Vector3f(WorldConfig.SPAWN.x,
                Terrain.groundY(WorldConfig.SPAWN.x, WorldConfig.SPAWN.y) + 1.7f,
                WorldConfig.SPAWN.y);
        scene.cam.setLocation(spawn);
        scene.cam.lookAt(new Vector3f(0f, spawn.y - 0.3f, 0f), Vector3f.UNIT_Y);

        // Rifle: dropped as a real-scale pickup sitting on the road just ahead
        // of spawn. The player's *equipped* rifle is abstract (LMB fires from
        // the camera) until we get a proper viewmodel viewport in a later pass.
        Node rifle = Rifle.build(a);
        rifle.setLocalScale(1.0f);
        float rx = spawn.x - 3f;
        float rz = spawn.z - 6f;
        rifle.setLocalTranslation(rx, Terrain.groundY(rx, rz) + 0.1f, rz);
        rifle.setLocalRotation(new Quaternion().fromAngles(
                0, (float) Math.toRadians(45), 0));
        scene.add(rifle);

        // Spawn hostiles: 6 zombies + 3 predators seeded around the wasteland.
        spawner = new Spawner();
        spawner.spawn(scene, a, 6, 3);
        System.out.println("[Game] spawned " + spawner.all.size() + " hostiles");

        // Left-click = fire raycast at nearest enemy under the crosshair.
        engine.registerFire(() -> {
            Vector3f origin = scene.cam.getLocation();
            Vector3f dir = scene.cam.getDirection();
            Enemy hit = spawner.raycast(origin, dir, 90f);
            if (hit != null) {
                hit.takeDamage(28f);
                System.out.println("[Fire] hit " + hit.node.getName()
                        + "  hp=" + hit.health + (hit.alive ? "" : "  DEAD"));
            }
        });
    }

    @Override
    public void update(float dt) {
        if (spawner != null) {
            spawner.tick(dt, scene.cam.getLocation());
            float dmg = spawner.drainDamage();
            if (dmg > 0f) {
                playerHp = Math.max(0f, playerHp - dmg);
                System.out.println("[Damage] player hit for " + dmg
                        + "  hp=" + playerHp);
            }
        }
    }
}
