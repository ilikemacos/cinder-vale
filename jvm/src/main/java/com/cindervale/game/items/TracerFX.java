package com.cindervale.game.items;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.scene.Scene;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Bullet tracer + muzzle-flash pool. Hitscan weapons need a visual so the
 * player sees WHERE the shot went — otherwise the "gun" is invisible impact.
 * Each fire() spawns a thin bright streak from muzzle to hit point that fades
 * over ~120ms; on the same tick we pop a tiny muzzle-flash sphere at the
 * origin that dies in one frame. Both use Unshaded so they read bright
 * against the overcast grade.
 */
public final class TracerFX {

    private static final float TRACER_LIFE = 0.12f;
    private static final float FLASH_LIFE = 0.06f;

    private final Assets assets;
    private final Node root;
    private final List<Tracer> tracers = new ArrayList<>();
    private final List<Flash> flashes = new ArrayList<>();

    public TracerFX(Assets a, Scene scene) {
        this.assets = a;
        this.root = new Node("TracerFX");
        scene.add(root);
    }

    /** Spawn a tracer streak from `from` to `to`, and a muzzle flash at `from`. */
    public void fire(Vector3f from, Vector3f to) {
        tracers.add(new Tracer(from, to));
        flashes.add(new Flash(from));
    }

    /** Advance all tracers/flashes; free geometry when their life expires. */
    public void tick(float dt) {
        Iterator<Tracer> ti = tracers.iterator();
        while (ti.hasNext()) {
            Tracer t = ti.next();
            t.age += dt;
            if (t.age >= TRACER_LIFE) {
                root.detachChild(t.geom);
                ti.remove();
            } else {
                float a = 1f - (t.age / TRACER_LIFE);
                t.mat.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.35f, a));
            }
        }
        Iterator<Flash> fi = flashes.iterator();
        while (fi.hasNext()) {
            Flash f = fi.next();
            f.age += dt;
            if (f.age >= FLASH_LIFE) {
                root.detachChild(f.geom);
                fi.remove();
            } else {
                float a = 1f - (f.age / FLASH_LIFE);
                f.mat.setColor("Color", new ColorRGBA(1.0f, 0.95f, 0.55f, a));
            }
        }
    }

    // --- internals ----------------------------------------------------------

    private final class Tracer {
        final Geometry geom;
        final Material mat;
        float age;

        Tracer(Vector3f from, Vector3f to) {
            Vector3f dir = to.subtract(from);
            float len = Math.max(0.5f, dir.length());
            // Slim streak — 3cm thick, `len` long along local Z.
            Box b = new Box(0.03f, 0.03f, len * 0.5f);
            geom = new Geometry("Tracer", b);
            mat = new Material(assets.jme(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(1.0f, 0.85f, 0.35f, 1f));
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            mat.getAdditionalRenderState().setDepthWrite(false);
            geom.setMaterial(mat);
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
            // Sit at midpoint; local -Z points along the shot direction.
            Vector3f mid = from.add(to).multLocal(0.5f);
            geom.setLocalTranslation(mid);
            geom.lookAt(to, Vector3f.UNIT_Y);
            root.attachChild(geom);
        }
    }

    private final class Flash {
        final Geometry geom;
        final Material mat;
        float age;

        Flash(Vector3f at) {
            com.jme3.scene.shape.Sphere s =
                    new com.jme3.scene.shape.Sphere(6, 8, 0.12f);
            geom = new Geometry("MuzzleFlash", s);
            mat = new Material(assets.jme(), "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(1f, 0.95f, 0.55f, 1f));
            mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            mat.getAdditionalRenderState().setDepthWrite(false);
            geom.setMaterial(mat);
            geom.setQueueBucket(RenderQueue.Bucket.Transparent);
            geom.setLocalTranslation(at);
            root.attachChild(geom);
        }
    }
}
