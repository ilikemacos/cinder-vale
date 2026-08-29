package com.cindervale.game.enemies;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import com.cindervale.engine.assets.Assets;

/**
 * Zombie / feral mutant — shambling humanoid built from primitives (head,
 * torso, two arms, two legs) skinned with the Leather008 CC0 hide from the
 * creatures pack. Slow, close-range biter. Legs sway when it's chasing.
 *
 * Not a scanned model — those aren't in the pack. Reads clearly at 20-30m,
 * which is where combat happens.
 */
public final class Zombie extends Enemy {

    private final Node legL, legR;
    private final Node armL, armR;
    private float gait = 0f;

    public Zombie(Assets a) {
        super(new Node("Zombie"), 0.5f, 1.8f, 42f, 1.7f, 8f);

        // Hide material — Leather008 with a sickly greenish tint.
        Material hide = a.pbrTriplet("feral_mutant/Leather008_2K-JPG",
                "Leather008_2K-JPG");
        hide.setColor("BaseColor", new ColorRGBA(0.55f, 0.60f, 0.42f, 1f));
        // Darker hide for boots/pants.
        Material darker = a.pbrTriplet("feral_mutant/Leather026_2K-JPG",
                "Leather026_2K-JPG");

        // Torso (chest).
        node.attachChild(box("torso", 0.55f, 0.75f, 0.35f,
                new Vector3f(0, 1.30f, 0), hide, 0.6f));
        // Head — slumped forward.
        Geometry head = box("head", 0.35f, 0.38f, 0.32f,
                new Vector3f(0, 1.90f, 0.05f), hide, 0.4f);
        head.setLocalRotation(new Quaternion().fromAngles(0.35f, 0, 0));
        node.attachChild(head);
        // Hip block.
        node.attachChild(box("hip", 0.5f, 0.30f, 0.35f,
                new Vector3f(0, 0.85f, 0), darker, 0.5f));

        // Arms as pivots (shoulder → forearm).
        armL = arm(new Vector3f(-0.36f, 1.55f, 0), hide);
        armR = arm(new Vector3f( 0.36f, 1.55f, 0), hide);
        node.attachChild(armL);
        node.attachChild(armR);

        // Legs as pivots (hip → shin).
        legL = leg(new Vector3f(-0.15f, 0.70f, 0), darker);
        legR = leg(new Vector3f( 0.15f, 0.70f, 0), darker);
        node.attachChild(legL);
        node.attachChild(legR);
    }

    private static Geometry box(String n, float w, float h, float d,
                                 Vector3f pos, Material mat, float uvScale) {
        Box b = new Box(w * 0.5f, h * 0.5f, d * 0.5f);
        b.scaleTextureCoordinates(new Vector2f(uvScale, uvScale));
        Geometry g = new Geometry(n, b);
        g.setLocalTranslation(pos);
        g.setMaterial(mat);
        return g;
    }

    private static Node arm(Vector3f shoulder, Material mat) {
        Node pivot = new Node("arm");
        pivot.setLocalTranslation(shoulder);
        // Slight droop forward so it reads as a shambling reach.
        pivot.setLocalRotation(new Quaternion().fromAngles(1.15f, 0, 0));
        Geometry g = box("armMesh", 0.14f, 0.75f, 0.14f,
                new Vector3f(0, -0.40f, 0), mat, 0.4f);
        pivot.attachChild(g);
        return pivot;
    }

    private static Node leg(Vector3f hip, Material mat) {
        Node pivot = new Node("leg");
        pivot.setLocalTranslation(hip);
        Geometry g = box("legMesh", 0.20f, 0.85f, 0.22f,
                new Vector3f(0, -0.45f, 0), mat, 0.4f);
        pivot.attachChild(g);
        return pivot;
    }

    @Override
    protected void afterMove(float dt, boolean moving) {
        if (moving) {
            gait += dt * 5.5f;   // shambling cadence
            float swing = FastMath.sin(gait) * 0.55f;
            legL.setLocalRotation(new Quaternion().fromAngles(swing, 0, 0));
            legR.setLocalRotation(new Quaternion().fromAngles(-swing, 0, 0));
            // Arms sway in opposition, still drooping.
            armL.setLocalRotation(new Quaternion().fromAngles(1.15f - swing * 0.4f, 0, 0));
            armR.setLocalRotation(new Quaternion().fromAngles(1.15f + swing * 0.4f, 0, 0));
        }
    }
}
