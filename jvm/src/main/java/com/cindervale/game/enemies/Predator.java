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
 * Reptile predator — four-limbed lizard-like hunter. Not a Deathclaw silhouette
 * (per the pack README's original-IP note). Faster and tougher than a zombie,
 * shorter attack range but harder-hitting. Legs animate in a diagonal trot when
 * chasing; tail sways.
 */
public final class Predator extends Enemy {

    private final Node[] legs = new Node[4];
    private final Node tail;
    private float gait = 0f;

    public Predator(Assets a) {
        super(new Node("Predator"), 0.7f, 4.8f, 90f, 2.4f, 15f);

        Material hide = a.pbrTriplet("reptile_predator/Leather014_2K-JPG",
                "Leather014_2K-JPG");
        hide.setColor("BaseColor", new ColorRGBA(0.35f, 0.30f, 0.22f, 1f));
        Material plates = a.pbrTriplet("reptile_predator/Rock058_2K-JPG",
                "Rock058_2K-JPG");

        // Body (torso). Long, low to the ground.
        node.attachChild(box("torso", 0.9f, 0.7f, 1.9f,
                new Vector3f(0, 1.1f, 0), hide, 0.6f));
        // Shoulders block (front, slightly raised).
        node.attachChild(box("shoulders", 1.05f, 0.55f, 0.8f,
                new Vector3f(0, 1.30f, -0.55f), hide, 0.55f));
        // Neck.
        node.attachChild(box("neck", 0.45f, 0.45f, 0.5f,
                new Vector3f(0, 1.35f, -1.05f), hide, 0.35f));
        // Head — long snout.
        node.attachChild(box("head", 0.4f, 0.42f, 0.7f,
                new Vector3f(0, 1.35f, -1.55f), plates, 0.45f));
        // Twin horns on the head.
        Geometry hornL = box("hornL", 0.08f, 0.4f, 0.08f,
                new Vector3f(-0.14f, 1.65f, -1.35f), plates, 0.2f);
        Geometry hornR = box("hornR", 0.08f, 0.4f, 0.08f,
                new Vector3f( 0.14f, 1.65f, -1.35f), plates, 0.2f);
        hornL.setLocalRotation(new Quaternion().fromAngles(-0.5f, 0, -0.15f));
        hornR.setLocalRotation(new Quaternion().fromAngles(-0.5f, 0,  0.15f));
        node.attachChild(hornL);
        node.attachChild(hornR);
        // Back plates.
        for (int i = 0; i < 4; i++) {
            Geometry plate = box("plate" + i, 0.35f, 0.22f, 0.28f,
                    new Vector3f(0, 1.55f, -0.4f + i * 0.42f), plates, 0.35f);
            node.attachChild(plate);
        }

        // Four legs (pivots at the hip, cylinder shaft downward).
        legs[0] = leg(new Vector3f(-0.42f, 1.0f, -0.6f), plates); // front-L
        legs[1] = leg(new Vector3f( 0.42f, 1.0f, -0.6f), plates); // front-R
        legs[2] = leg(new Vector3f(-0.42f, 1.0f,  0.6f), plates); // back-L
        legs[3] = leg(new Vector3f( 0.42f, 1.0f,  0.6f), plates); // back-R
        for (Node l : legs) node.attachChild(l);

        // Tail — 3 segments tapering.
        tail = new Node("tail");
        tail.setLocalTranslation(0, 1.15f, 0.95f);
        Node seg1 = new Node("seg1");
        Node seg2 = new Node("seg2");
        seg1.attachChild(box("t1", 0.25f, 0.25f, 0.7f, new Vector3f(0, 0, 0.35f), hide, 0.3f));
        seg2.setLocalTranslation(0, 0, 0.7f);
        seg2.attachChild(box("t2", 0.18f, 0.18f, 0.7f, new Vector3f(0, 0, 0.35f), hide, 0.3f));
        seg1.attachChild(seg2);
        tail.attachChild(seg1);
        node.attachChild(tail);
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

    private static Node leg(Vector3f hip, Material mat) {
        Node pivot = new Node("leg");
        pivot.setLocalTranslation(hip);
        Geometry g = box("shaft", 0.20f, 1.0f, 0.20f,
                new Vector3f(0, -0.5f, 0), mat, 0.4f);
        pivot.attachChild(g);
        return pivot;
    }

    @Override
    protected void afterMove(float dt, boolean moving) {
        if (moving) {
            gait += dt * 9.5f;
            // Diagonal-trot pattern.
            float phaseA = FastMath.sin(gait) * 0.7f;
            float phaseB = FastMath.sin(gait + FastMath.PI) * 0.7f;
            legs[0].setLocalRotation(new Quaternion().fromAngles(phaseA, 0, 0));
            legs[3].setLocalRotation(new Quaternion().fromAngles(phaseA, 0, 0));
            legs[1].setLocalRotation(new Quaternion().fromAngles(phaseB, 0, 0));
            legs[2].setLocalRotation(new Quaternion().fromAngles(phaseB, 0, 0));
            // Tail sways slightly.
            tail.setLocalRotation(new Quaternion().fromAngles(0,
                    FastMath.sin(gait * 0.5f) * 0.35f, 0));
        }
    }
}
