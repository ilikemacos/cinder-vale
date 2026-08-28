package com.cindervale.game;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.core.Engine;
import com.cindervale.engine.core.Game;
import com.cindervale.engine.scene.Scene;

/**
 * Phase-1 vertical slice: proves the engine/game separation + PBR asset pipeline
 * end-to-end on the M1. Loads the Poly Haven overcast HDRI as sky, tiles the
 * cracked-asphalt PBR material on the ground, and scatters real props (barrel,
 * trash can, boulder, wheel rim, road barrier, cardboard box, rusted can) around
 * a small courtyard. No JME imports live in this file's *behaviour* — only for
 * Vector3f/geometry math, which is engine-independent value data.
 */
public final class CinderValeDemo implements Game {

    @Override
    public void init(Engine engine, Scene scene) {
        Assets a = scene.assets;

        // Overcast wasteland sky.
        boolean sky = scene.setSkyEquirect(
                "assets/env/cinder-vale-env-art/hdris/overcast_soil_puresky_2k.hdr");
        if (!sky) {
            scene.setSkyEquirect(
                    "assets/env/cinder-vale-env-art/hdris/rural_asphalt_road_2k.hdr");
        }

        scene.addDefaultLighting();

        // Ground: 60x60 m PBR asphalt quad, UV-tiled so 2K doesn't smear.
        Geometry ground = groundQuad(a, "asphalt_02", 60f, 8f);
        scene.add(ground);

        // Prop layout — small courtyard scatter to prove the glTF loader.
        placeProp(scene, a, "Barrel_02", new Vector3f(2.0f, 0, 1.5f), 0.0f, 1.6f);
        placeProp(scene, a, "Barrel_02", new Vector3f(3.4f, 0, 1.2f), 0.9f, 1.6f);
        placeProp(scene, a, "metal_trash_can", new Vector3f(-2.5f, 0, 2.0f), 0.4f, 1.6f);
        placeProp(scene, a, "cardboard_box_01", new Vector3f(-2.0f, 0, -0.4f), 1.1f, 1.6f);
        placeProp(scene, a, "concrete_road_barrier_02", new Vector3f(0, 0, -4.5f), 0.05f, 1.6f);
        placeProp(scene, a, "boulder_01", new Vector3f(-6.0f, 0, -2.5f), 0.6f, 2.4f);
        placeProp(scene, a, "boulder_01", new Vector3f(5.5f, 0, -3.2f), 2.1f, 1.8f);
        placeProp(scene, a, "rusted_wheel_rim_01", new Vector3f(1.2f, 0, -1.8f), 1.3f, 1.6f);
        placeProp(scene, a, "can_rusted", new Vector3f(-0.6f, 0, 1.2f), 0.2f, 1.6f);

        // Camera: over-the-shoulder view of the courtyard.
        scene.cam.setLocation(new Vector3f(5.5f, 2.4f, 6.5f));
        scene.cam.lookAt(new Vector3f(0, 0.6f, 0), Vector3f.UNIT_Y);
    }

    @Override
    public void update(float dt) {
        // Empty — Phase 1 is a static PBR check. Player + gameplay come next.
    }

    private Geometry groundQuad(Assets a, String texSet, float sizeM, float tiles) {
        Quad q = new Quad(sizeM, sizeM);
        q.scaleTextureCoordinates(new Vector2f(tiles, tiles));
        Geometry g = new Geometry("Ground", q);
        // Lay flat and centre on the origin.
        g.setLocalRotation(new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0));
        g.setLocalTranslation(-sizeM * 0.5f, 0, sizeM * 0.5f);
        Material m = a.pbr(texSet);
        g.setMaterial(m);
        return g;
    }

    private void placeProp(Scene s, Assets a, String name, Vector3f pos,
                            float yaw, float scale) {
        try {
            Spatial prop = a.loadModel(name);
            prop.setLocalTranslation(pos);
            prop.setLocalRotation(new Quaternion().fromAngles(0, yaw, 0));
            prop.setLocalScale(scale);
            s.add(prop);
        } catch (Exception e) {
            System.err.println("[Demo] failed to load prop " + name + " — " + e.getMessage());
        }
    }
}
