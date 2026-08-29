package com.cindervale.game;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.core.Engine;
import com.cindervale.engine.core.Game;
import com.cindervale.engine.scene.Scene;
import com.cindervale.game.items.Rifle;
import com.cindervale.game.world.Road;
import com.cindervale.game.world.Scatter;
import com.cindervale.game.world.Terrain;
import com.cindervale.game.world.WorldConfig;
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

    @Override
    public void init(Engine engine, Scene scene) {
        Assets a = scene.assets;

        // Sky: overcast HDRI (falls back to the rural asphalt one if missing).
        boolean sky = scene.setSkyEquirect(
                "assets/env/cinder-vale-env-art/hdris/overcast_soil_puresky_2k.hdr");
        if (!sky) scene.setSkyEquirect(
                "assets/env/cinder-vale-env-art/hdris/rural_asphalt_road_2k.hdr");

        scene.addDefaultLighting();
        // FO4 golden-hour cast: warmer sun with a subtle amber tint, and a
        // faintly amber ambient — dry, dusty, "the day after the bombs".
        if (scene.sun() != null) {
            scene.sun().setColor(new ColorRGBA(1.10f, 0.96f, 0.80f, 1f));
        }
        if (scene.ambient() != null) {
            scene.ambient().setColor(new ColorRGBA(0.75f, 0.70f, 0.60f, 1f));
        }

        // Distance fog — warm amber haze pulling everything toward that FO4
        // "post-bomb morning" look. Density kept low so the terrain reads at
        // close range; only the far mountains + horizon fade into the haze.
        scene.enableFog(new ColorRGBA(0.82f, 0.72f, 0.55f, 1f), 0.9f, 320f);

        // Tell the engine how to look up ground height for terrain-clamp.
        scene.groundHeight = (x, z) -> Terrain.groundY(x, z);

        // Terrain — 200×200 m, ~2 m quads = 100×100 grid = 10k quads / 20k tris.
        Geometry terrain = Terrain.build(a, WorldConfig.WORLD_HALF, 2f);
        scene.add(terrain);

        // Cracked-asphalt highway crossing the valley N-S through the centre.
        scene.add(Road.build(a, WorldConfig.WORLD_HALF, 7f, 3f));

        // Wasteland dressing: real Poly Haven props + dead trees + mountains.
        Scatter.populate(scene, a);

        // Camera spawn near the "bus wreck" spot, eye height above ground.
        Vector3f spawn = new Vector3f(WorldConfig.SPAWN.x,
                Terrain.groundY(WorldConfig.SPAWN.x, WorldConfig.SPAWN.y) + 1.7f,
                WorldConfig.SPAWN.y);
        scene.cam.setLocation(spawn);
        scene.cam.lookAt(new Vector3f(0f, spawn.y - 0.3f, 0f), Vector3f.UNIT_Y);

        // Rifle: dropped at spawn so the player literally starts *at* the gun,
        // slightly right of the camera at hip height. Scaled up 3x so it reads
        // clearly even when you first look down. Viewmodel-in-camera can come
        // later; this is a solid, visible starting weapon.
        Node rifle = Rifle.build(a);
        rifle.setLocalScale(3.0f);
        rifle.setLocalTranslation(spawn.x + 0.8f,
                Terrain.groundY(spawn.x + 0.8f, spawn.z) + 0.9f,
                spawn.z + 0.2f);
        rifle.setLocalRotation(new Quaternion().fromAngles(
                (float) Math.toRadians(0),
                (float) Math.toRadians(-25),
                (float) Math.toRadians(-8)));
        scene.add(rifle);
    }

    @Override
    public void update(float dt) {
        // Static wasteland for now — enemies + gunplay reuse the ported systems next.
    }
}
