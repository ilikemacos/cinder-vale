package com.cindervale.game.items;

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
 * Procedural low-poly assault rifle, built as a viewmodel — receiver + barrel
 * + magazine + grip + stock + iron sight. Attached to a Node that follows the
 * camera each frame, offset bottom-right so it reads as "held".
 *
 * Not the real weapon system yet — no ammo, no fire, no reload; that comes
 * next. This just gets a gun on-screen at spawn.
 */
public final class Rifle {

    /** Build the rifle root — call once, then attach to the viewmodel anchor. */
    public static Node build(Assets a) {
        Node root = new Node("Rifle");
        Material gunmetal = a.litColor(new ColorRGBA(0.09f, 0.09f, 0.10f, 1f));
        Material grip = a.litColor(new ColorRGBA(0.06f, 0.06f, 0.07f, 1f));
        Material mag = a.litColor(new ColorRGBA(0.13f, 0.13f, 0.14f, 1f));

        // Receiver — the body of the gun.
        root.attachChild(box("receiver", 0.06f, 0.09f, 0.46f,
                new Vector3f(0, 0, 0.02f), gunmetal));
        // Barrel + handguard extending forward (-Z).
        root.attachChild(box("barrel", 0.032f, 0.032f, 0.50f,
                new Vector3f(0, 0.02f, -0.42f), gunmetal));
        // Magazine (curved-ish approximation).
        root.attachChild(box("mag", 0.05f, 0.20f, 0.09f,
                new Vector3f(0, -0.14f, 0.06f), mag));
        // Pistol grip.
        root.attachChild(box("grip", 0.045f, 0.11f, 0.05f,
                new Vector3f(0, -0.09f, 0.16f), grip));
        // Stock.
        root.attachChild(box("stock", 0.05f, 0.10f, 0.22f,
                new Vector3f(0, 0f, 0.34f), grip));
        // Front sight / rail.
        root.attachChild(box("sight", 0.012f, 0.03f, 0.14f,
                new Vector3f(0, 0.09f, -0.05f), gunmetal));
        // A tiny cylinder as the muzzle so the barrel tip reads.
        Geometry muzzle = new Geometry("muzzle",
                new Cylinder(6, 8, 0.024f, 0.03f, true));
        muzzle.setLocalRotation(new Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        muzzle.setLocalTranslation(0, 0.02f, -0.68f);
        muzzle.setMaterial(gunmetal);
        root.attachChild(muzzle);

        return root;
    }

    private static Geometry box(String name, float hx, float hy, float hz,
                                 Vector3f pos, Material mat) {
        Geometry g = new Geometry(name, new Box(hx * 0.5f, hy * 0.5f, hz * 0.5f));
        g.setLocalTranslation(pos);
        g.setMaterial(mat);
        return g;
    }

    private Rifle() {}
}
