package com.cindervale.game.world;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import com.cindervale.engine.assets.Assets;

import java.util.Random;

/**
 * Ruined structures — brick wall fragments and moss-brick chunks poking out of
 * the ground like a Fallout roadside settlement. Reuses the Poly Haven
 * brick_wall_001 and brick_moss_001 PBR sets so they blend with the terrain
 * PBR without needing new textures.
 */
public final class Ruins {

    /** Build a ruined wall fragment — a couple of leaning/broken segments. */
    public static Node wallFragment(Assets a, boolean mossy, Random rng) {
        Node root = new Node(mossy ? "MossWall" : "BrickWall");
        var mat = a.pbr(mossy ? "brick_moss_001" : "brick_wall_001");

        int segments = 2 + rng.nextInt(3);       // 2-4 segments
        float x = 0;
        for (int i = 0; i < segments; i++) {
            float h = 1.6f + rng.nextFloat() * 1.4f;
            float w = 1.2f + rng.nextFloat() * 1.0f;
            float d = 0.35f;
            // Chunk out the top so it reads as broken.
            float top = h - rng.nextFloat() * 0.6f;
            Geometry g = new Geometry("seg", new Box(w * 0.5f, top * 0.5f, d * 0.5f));
            g.setLocalTranslation(x + w * 0.5f, top * 0.5f, 0);
            // Slight lean.
            g.setLocalRotation(new Quaternion().fromAngles(
                    0, 0, (rng.nextFloat() - 0.5f) * 0.15f));
            // Tile the brick UVs — bricks stay brick-sized.
            g.getMesh().scaleTextureCoordinates(new Vector2f(w * 0.6f, top * 0.6f));
            g.setMaterial(mat);
            root.attachChild(g);
            x += w + rng.nextFloat() * 0.4f;
        }
        return root;
    }

    /** Radioactive-yellow barrel with a subtle green emissive leak. */
    public static Node radBarrel(Assets a) {
        Node root = new Node("RadBarrel");
        // Base barrel (same scaled Barrel model, tinted greenish/yellow).
        try {
            var b = a.loadModel("Barrel_02");
            b.setLocalScale(1.5f);
            root.attachChild(b);
        } catch (Exception ignored) {}

        // Glow indicator: a small unshaded green pip on top, hint at the leak.
        Geometry pip = new Geometry("glow",
                new com.jme3.scene.shape.Sphere(6, 8, 0.08f));
        var m = new com.jme3.material.Material(a.jme(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", new com.jme3.math.ColorRGBA(0.6f, 1.0f, 0.4f, 1f));
        pip.setMaterial(m);
        pip.setLocalTranslation(0, 1.2f, 0);
        root.attachChild(pip);

        // Point light for the leak — one small green source; keep total count low.
        var light = new com.jme3.light.PointLight();
        light.setColor(new com.jme3.math.ColorRGBA(0.35f, 0.9f, 0.30f, 1f).mult(1.4f));
        light.setRadius(3.5f);
        light.setPosition(new Vector3f(0, 1.2f, 0));
        // Attach to spatial's local via LightControl-style pattern:
        root.addLight(light);
        return root;
    }

    private Ruins() {}
}
