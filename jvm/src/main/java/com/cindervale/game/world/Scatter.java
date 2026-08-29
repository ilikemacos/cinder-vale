package com.cindervale.game.world;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.scene.Scene;

import java.util.Random;

/**
 * Populates the wasteland with real Poly Haven props + procedural dead trees
 * and a distant mountain silhouette. Deterministic (seeded) — same seed, same
 * layout. Densities chosen to look dense on the M1 without blowing the 8 GB
 * budget: hundreds of props, individual clones sharing cached meshes/textures
 * via the Assets cache (proper instancing comes with BatchNode in a later pass).
 */
public final class Scatter {

    public static void populate(Scene scene, Assets a) {
        Random rng = new Random(20260828L);

        // The catalogue — spawn weights sum informally to ~1.
        Object[][] catalogue = {
                {"Barrel_02",                 0.15, 1.5f, 1.8f},
                {"metal_trash_can",           0.10, 1.4f, 1.7f},
                {"cardboard_box_01",          0.12, 1.2f, 1.5f},
                {"boulder_01",                0.22, 1.6f, 3.2f},
                {"rusted_wheel_rim_01",       0.10, 1.3f, 1.7f},
                {"concrete_road_barrier_02", 0.05, 1.4f, 1.6f},
                {"can_rusted",                0.26, 1.0f, 1.4f},
        };

        // Density: ~500 props over 200x200 = 1 prop per ~80 m². Reasonable.
        int propCount = 520;
        Node scatterRoot = new Node("Scatter");
        for (int i = 0; i < propCount; i++) {
            float x = rng.nextFloat() * (WorldConfig.WORLD_HALF * 2f) - WorldConfig.WORLD_HALF;
            float z = rng.nextFloat() * (WorldConfig.WORLD_HALF * 2f) - WorldConfig.WORLD_HALF;
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.55f) continue;   // avoid cliffs

            Object[] pick = pickWeighted(catalogue, rng);
            String name = (String) pick[0];
            float scaleMin = (float) pick[2];
            float scaleMax = (float) pick[3];

            try {
                Spatial prop = a.loadModel(name);
                float y = Terrain.groundY(x, z);
                float sc = scaleMin + rng.nextFloat() * (scaleMax - scaleMin);
                prop.setLocalTranslation(x, y, z);
                prop.setLocalRotation(new Quaternion().fromAngles(0, rng.nextFloat() * FastMath.TWO_PI, 0));
                prop.setLocalScale(sc);
                scatterRoot.attachChild(prop);
                // Register a rough collision cylinder — radius derived from
                // the prop's typical footprint × scale. Not physical, but reads.
                float r = propRadius(name) * sc;
                float h = propHeight(name) * sc;
                scene.collision.add(x, z, r, h);
            } catch (Exception ignored) {}
        }
        scene.add(scatterRoot);

        // Wrecked cars — the iconic Fallout roadside prop. Scattered along the
        // highway corridor for a "convoy caught in the blast" feel.
        Node cars = new Node("WreckedCars");
        for (int i = 0; i < 14; i++) {
            // Bias placement near the middle-N/S road corridor.
            float x = (rng.nextFloat() - 0.5f) * 40f + FastMath.sin(i * 0.7f) * 8f;
            float z = rng.nextFloat() * (WorldConfig.WORLD_HALF * 1.8f) - WorldConfig.WORLD_HALF * 0.9f;
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.4f) continue;
            Node car = WreckedCar.build(a);
            car.setLocalTranslation(x, Terrain.groundY(x, z), z);
            car.setLocalRotation(new Quaternion().fromAngles(0,
                    rng.nextFloat() * FastMath.TWO_PI, 0));
            cars.attachChild(car);
            scene.collision.add(x, z, 2.0f, 1.4f);
        }
        scene.add(cars);

        // Ruined brick walls — chunks of collapsed buildings for silhouette.
        Node ruins = new Node("Ruins");
        for (int i = 0; i < 18; i++) {
            float x = rng.nextFloat() * (WorldConfig.WORLD_HALF * 1.8f) - WorldConfig.WORLD_HALF * 0.9f;
            float z = rng.nextFloat() * (WorldConfig.WORLD_HALF * 1.8f) - WorldConfig.WORLD_HALF * 0.9f;
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.5f) continue;
            Node wall = Ruins.wallFragment(a, rng.nextBoolean(), rng);
            wall.setLocalTranslation(x, Terrain.groundY(x, z), z);
            wall.setLocalRotation(new Quaternion().fromAngles(0,
                    rng.nextFloat() * FastMath.TWO_PI, 0));
            ruins.attachChild(wall);
            scene.collision.add(x, z, 1.4f, 2.0f);
        }
        scene.add(ruins);

        // A few radioactive barrels with green glow — atmosphere at low
        // density (max 3, each carries a point light — respect the light budget).
        Node radBarrels = new Node("RadBarrels");
        for (int i = 0; i < 3; i++) {
            float x = (rng.nextFloat() - 0.5f) * (WorldConfig.WORLD_HALF * 1.6f);
            float z = (rng.nextFloat() - 0.5f) * (WorldConfig.WORLD_HALF * 1.6f);
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.4f) continue;
            Node b = Ruins.radBarrel(a);
            b.setLocalTranslation(x, Terrain.groundY(x, z), z);
            radBarrels.attachChild(b);
            scene.collision.add(x, z, 0.5f, 1.5f);
        }
        scene.add(radBarrels);

        // Dead trees — leafless silhouettes for that Fallout look.
        Node trees = new Node("DeadTrees");
        Material trunkMat = a.litColor(new ColorRGBA(0.16f, 0.12f, 0.08f, 1f));
        for (int i = 0; i < 160; i++) {
            float x = rng.nextFloat() * (WorldConfig.WORLD_HALF * 2f) - WorldConfig.WORLD_HALF;
            float z = rng.nextFloat() * (WorldConfig.WORLD_HALF * 2f) - WorldConfig.WORLD_HALF;
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.5f) continue;
            trees.attachChild(deadTree(x, Terrain.groundY(x, z), z,
                    2.5f + rng.nextFloat() * 3.5f, rng, trunkMat));
        }
        scene.add(trees);

        // Distant mountain backdrop — a ring of silhouette wedges well beyond
        // the playfield, so haze/fog blends them into the sky.
        scene.add(mountainRing(a, WorldConfig.WORLD_HALF * 3.0f, 44, 40f, 90f,
                new ColorRGBA(0.28f, 0.29f, 0.31f, 1f), 771));
        scene.add(mountainRing(a, WorldConfig.WORLD_HALF * 4.5f, 40, 70f, 140f,
                new ColorRGBA(0.35f, 0.36f, 0.38f, 1f), 553));
    }

    /** Approximate footprint radius (metres) at unit scale. */
    private static float propRadius(String name) {
        return switch (name) {
            case "Barrel_02", "metal_trash_can", "can_rusted",
                 "rusted_wheel_rim_01"          -> 0.35f;
            case "cardboard_box_01"             -> 0.45f;
            case "boulder_01"                   -> 0.9f;
            case "concrete_road_barrier_02"     -> 0.7f;
            default                             -> 0.4f;
        };
    }

    /** Approximate stand-up height at unit scale (used later for eye/ceiling checks). */
    private static float propHeight(String name) {
        return switch (name) {
            case "concrete_road_barrier_02" -> 0.9f;
            case "boulder_01"                -> 1.3f;
            default                          -> 1.0f;
        };
    }

    private static Object[] pickWeighted(Object[][] cat, Random rng) {
        double sum = 0;
        for (Object[] row : cat) sum += (double) row[1];
        double r = rng.nextDouble() * sum;
        double acc = 0;
        for (Object[] row : cat) {
            acc += (double) row[1];
            if (r <= acc) return row;
        }
        return cat[cat.length - 1];
    }

    /** Chunky trunk + a few boxy branches — cheap, reads as dead tree. */
    private static Node deadTree(float x, float y, float z, float height,
                                  Random rng, Material mat) {
        Node t = new Node("DeadTree");
        t.setLocalTranslation(x, y, z);
        float trunkR = 0.12f + rng.nextFloat() * 0.06f;
        Geometry trunk = new Geometry("t", new Cylinder(6, 8, trunkR, height, true));
        trunk.setLocalRotation(new Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        trunk.setLocalTranslation(0, height * 0.5f, 0);
        trunk.setMaterial(mat);
        t.attachChild(trunk);
        int branches = 3 + rng.nextInt(3);
        for (int b = 0; b < branches; b++) {
            float bh = height * (0.55f + rng.nextFloat() * 0.35f);
            float bl = 0.6f + rng.nextFloat() * 1.2f;
            Geometry br = new Geometry("b", new Box(0.04f, 0.04f, bl * 0.5f));
            float ang = rng.nextFloat() * FastMath.TWO_PI;
            br.setLocalRotation(new Quaternion().fromAngles(
                    -0.4f - rng.nextFloat() * 0.6f, ang, 0));
            br.setLocalTranslation(
                    FastMath.cos(ang) * bl * 0.4f,
                    bh,
                    FastMath.sin(ang) * bl * 0.4f);
            br.setMaterial(mat);
            t.attachChild(br);
        }
        return t;
    }

    /** Static ridge silhouette on the horizon — many low overlapping wedges so
     *  it reads as rolling mountains, not a row of teeth. No collision. */
    private static Geometry mountainRing(Assets a, float radius, int count,
                                          float hMin, float hMax,
                                          ColorRGBA col, int seed) {
        Random rng = new Random(seed);
        int vCount = count * 3;
        Vector3f[] v = new Vector3f[vCount];
        int[] idx = new int[vCount];
        for (int i = 0; i < count; i++) {
            float a0 = FastMath.TWO_PI * i / count;
            float a1 = FastMath.TWO_PI * (i + 1) / count;
            float mid = (a0 + a1) * 0.5f;
            // Wider than the wedge's slot so neighbours overlap into rolling ridges.
            float spread = (a1 - a0) * (1.6f + rng.nextFloat() * 0.8f);
            float h = hMin + rng.nextFloat() * (hMax - hMin);
            // Peak offset in azimuth so the silhouette isn't perfectly regular.
            float peakOff = (rng.nextFloat() - 0.5f) * (a1 - a0) * 0.9f;
            Vector3f bl = new Vector3f(FastMath.cos(mid - spread) * radius, -20f,
                    FastMath.sin(mid - spread) * radius);
            Vector3f br = new Vector3f(FastMath.cos(mid + spread) * radius, -20f,
                    FastMath.sin(mid + spread) * radius);
            Vector3f pk = new Vector3f(FastMath.cos(mid + peakOff) * radius, h,
                    FastMath.sin(mid + peakOff) * radius);
            v[i*3]   = bl; v[i*3+1] = pk; v[i*3+2] = br;
            idx[i*3] = i*3; idx[i*3+1] = i*3+1; idx[i*3+2] = i*3+2;
        }
        com.jme3.scene.Mesh m = new com.jme3.scene.Mesh();
        m.setBuffer(com.jme3.scene.VertexBuffer.Type.Position, 3,
                com.jme3.util.BufferUtils.createFloatBuffer(v));
        m.setBuffer(com.jme3.scene.VertexBuffer.Type.Index, 3,
                com.jme3.util.BufferUtils.createIntBuffer(idx));
        m.updateBound();
        Geometry g = new Geometry("Mountains", m);
        Material mat = a.litColor(col);
        mat.getAdditionalRenderState().setFaceCullMode(
                com.jme3.material.RenderState.FaceCullMode.Off);
        g.setMaterial(mat);
        return g;
    }

    private Scatter() {}
}
