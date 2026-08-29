package com.cindervale.game.world;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

import com.cindervale.engine.assets.Assets;

/**
 * Procedural rusted-out sedan wreck — the Fallout 4 signature roadside prop.
 * Chassis, cabin, hood (open at an angle), bumpers, four flattened tyres. A
 * rusted brown/red palette so it reads as decades-old wreckage without needing
 * a scanned model.
 */
public final class WreckedCar {

    private static final ColorRGBA RUST      = new ColorRGBA(0.34f, 0.18f, 0.10f, 1f);
    private static final ColorRGBA RUST_DARK = new ColorRGBA(0.22f, 0.11f, 0.06f, 1f);
    private static final ColorRGBA TIRE      = new ColorRGBA(0.06f, 0.05f, 0.05f, 1f);
    private static final ColorRGBA GLASS     = new ColorRGBA(0.10f, 0.11f, 0.09f, 1f);

    public static Node build(Assets a) {
        Node root = new Node("WreckedCar");
        Material body = a.litColor(RUST);
        Material bodyDark = a.litColor(RUST_DARK);
        Material tire = a.litColor(TIRE);
        Material glass = a.litColor(GLASS);

        // Chassis (main body, low to ground).
        root.attachChild(box("chassis", 2.0f, 0.5f, 4.6f,
                new Vector3f(0, 0.55f, 0), body));
        // Cabin (upper) — narrower, further back.
        root.attachChild(box("cabin", 1.75f, 0.7f, 2.2f,
                new Vector3f(0, 1.15f, 0.2f), bodyDark));
        // Rear window band.
        root.attachChild(box("rearwin", 1.7f, 0.5f, 0.05f,
                new Vector3f(0, 1.15f, 1.30f), glass));
        // Side glass slivers (broken — small strips).
        root.attachChild(box("sglassL", 0.05f, 0.4f, 1.8f,
                new Vector3f(-0.88f, 1.20f, 0.2f), glass));
        root.attachChild(box("sglassR", 0.05f, 0.4f, 1.8f,
                new Vector3f(0.88f, 1.20f, 0.2f), glass));
        // Hood — tilted open like it was ripped off.
        Geometry hood = new Geometry("hood",
                new Box(0.9f, 0.05f, 0.9f));
        hood.setMaterial(body);
        hood.setLocalTranslation(0, 1.05f, -1.65f);
        hood.setLocalRotation(new Quaternion().fromAngles(
                (float) Math.toRadians(-35), 0, 0));
        root.attachChild(hood);
        // Bumpers — small blocks front + back.
        root.attachChild(box("bumperF", 2.0f, 0.25f, 0.15f,
                new Vector3f(0, 0.45f, -2.35f), bodyDark));
        root.attachChild(box("bumperR", 2.0f, 0.25f, 0.15f,
                new Vector3f(0, 0.45f, 2.35f), bodyDark));
        // Four tires (flattened cylinders — one deflated).
        addTire(root, tire, -0.90f, 0.28f, -1.50f, 0.30f);
        addTire(root, tire,  0.90f, 0.28f, -1.50f, 0.30f);
        addTire(root, tire, -0.90f, 0.28f,  1.50f, 0.30f);
        addTire(root, tire,  0.90f, 0.20f,  1.50f, 0.20f);  // flat rear-right
        // Steering column stub visible through blown-out windshield.
        root.attachChild(box("column", 0.06f, 0.35f, 0.06f,
                new Vector3f(0.35f, 1.25f, -0.55f), bodyDark));
        return root;
    }

    private static Geometry box(String name, float w, float h, float d,
                                 Vector3f pos, Material mat) {
        Geometry g = new Geometry(name, new Box(w * 0.5f, h * 0.5f, d * 0.5f));
        g.setLocalTranslation(pos);
        g.setMaterial(mat);
        return g;
    }

    private static void addTire(Node root, Material mat, float x, float y, float z, float radius) {
        Cylinder c = new Cylinder(6, 12, radius, 0.35f, true);
        Geometry g = new Geometry("tire", c);
        g.setMaterial(mat);
        g.setLocalTranslation(x, y, z);
        // Cylinder is Z-aligned; rotate so it's axle-aligned (X).
        g.setLocalRotation(new Quaternion().fromAngles(0, FastMath.HALF_PI, 0));
        root.attachChild(g);
    }

    private WreckedCar() {}
}
