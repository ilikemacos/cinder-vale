package com.cindervale.game.world;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.util.BufferUtils;

import com.cindervale.engine.assets.Assets;

/**
 * Named landmarks that give the valley a shape you can navigate by. Per the
 * review — a mill stack, water in the carved trench, a silhouette per POI.
 * Everything here is intentionally low-poly and reads at silhouette scale
 * because that's how the player will see it: from a distance, through fog.
 */
public final class Landmarks {

    /** Standing water in the river channel. Ribbon that follows the same
     *  meander the WorldConfig height fn carves, so the river looks *cut*
     *  instead of laid on top. Water sits ~0.6m below the ambient ground. */
    public static Geometry riverWater(Assets a) {
        int segs = (int) (WorldConfig.WORLD_HALF * 2f / 4f);
        int vpr = 2;
        int vCount = (segs + 1) * vpr;
        int iCount = segs * 6;

        Vector3f[] verts = new Vector3f[vCount];
        Vector2f[] uvs = new Vector2f[vCount];
        Vector3f[] norms = new Vector3f[vCount];
        int[] tris = new int[iCount];

        for (int i = 0; i <= segs; i++) {
            float z = -WorldConfig.WORLD_HALF + i * 4f;
            // Match WorldConfig's river_x formula so water tracks the trench.
            float rx = FastMath.sin(z * 0.05f) * 8f;
            float w = WorldConfig.RIVER_HALF_W - 1.5f;   // sit inside the bank
            float y = WorldConfig.BASE - 0.3f;
            verts[i * 2]     = new Vector3f(rx - w, y, z);
            verts[i * 2 + 1] = new Vector3f(rx + w, y, z);
            uvs[i * 2]       = new Vector2f(0f, z * 0.2f);
            uvs[i * 2 + 1]   = new Vector2f(1f, z * 0.2f);
            norms[i * 2]     = Vector3f.UNIT_Y;
            norms[i * 2 + 1] = Vector3f.UNIT_Y;
        }
        int t = 0;
        for (int i = 0; i < segs; i++) {
            int a0 = i * 2, b = a0 + 1, c = a0 + 2, d = a0 + 3;
            tris[t++] = a0; tris[t++] = c; tris[t++] = b;
            tris[t++] = b;  tris[t++] = c; tris[t++] = d;
        }
        Mesh m = new Mesh();
        m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
        m.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        m.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(norms));
        m.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(tris));
        m.updateBound();

        Geometry g = new Geometry("River", m);
        Material mat = new Material(a.jme(), "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        // Cold, still, mineral — that damp PNW river look, not a Caribbean blue.
        mat.setColor("Diffuse", new ColorRGBA(0.09f, 0.14f, 0.13f, 0.85f));
        mat.setColor("Ambient", new ColorRGBA(0.08f, 0.11f, 0.11f, 1f));
        mat.setColor("Specular", new ColorRGBA(0.35f, 0.42f, 0.42f, 1f));
        mat.setFloat("Shininess", 96f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        g.setQueueBucket(RenderQueue.Bucket.Transparent);
        return g;
    }

    /** The Mill — a tall smokestack + adjoining hall you can see from anywhere.
     *  Reads as a silhouette. Placed east of centre so the highway leads toward
     *  it. Registers a collider so you can't walk through the stack. */
    public static Node mill(Assets a, com.cindervale.engine.scene.Scene scene) {
        Node root = new Node("Mill");
        float wx = 78f, wz = -32f;
        float wy = Terrain.groundY(wx, wz);
        root.setLocalTranslation(wx, wy, wz);

        Material brick = a.pbr("brick_wall_001");
        Material concrete = a.pbr("worn_concrete_floor");
        Material metal = a.litColor(new ColorRGBA(0.14f, 0.14f, 0.14f, 1f));

        // Hall — long low industrial building.
        Geometry hall = new Geometry("MillHall",
                boxWithUV(new Vector3f(18f, 8f, 12f), new Vector2f(6f, 3f)));
        hall.setLocalTranslation(0, 4f, 0);
        hall.setMaterial(brick);
        root.attachChild(hall);
        scene.collision.add(wx, wz, 12f, 8f);

        // Sawtooth roof — three ridge blocks (concrete for silhouette variety).
        for (int i = 0; i < 3; i++) {
            Geometry ridge = new Geometry("ridge" + i,
                    boxWithUV(new Vector3f(5f, 2.5f, 12f), new Vector2f(2f, 2f)));
            ridge.setLocalTranslation(-6f + i * 6f, 9.5f, 0);
            ridge.setMaterial(concrete);
            root.attachChild(ridge);
        }

        // The stack — the actual navigation landmark. Tall + narrow.
        Cylinder cyl = new Cylinder(2, 24, 2.6f, 34f, true);
        Geometry stack = new Geometry("MillStack", cyl);
        stack.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        stack.setLocalTranslation(9f, 17f + 8f, 6f);
        stack.setMaterial(brick);
        root.attachChild(stack);
        scene.collision.add(wx + 9f, wz + 6f, 3.2f, 34f);

        // A stub gantry — steel walkway rail off the stack, adds silhouette detail.
        Geometry gantry = new Geometry("gantry",
                boxWithUV(new Vector3f(0.3f, 0.3f, 6f), new Vector2f(1f, 1f)));
        gantry.setLocalTranslation(9f, 24f, 3f);
        gantry.setMaterial(metal);
        root.attachChild(gantry);

        return root;
    }

    /** A simple town-tower silhouette — water tank on legs. One more landmark. */
    public static Node waterTower(Assets a, com.cindervale.engine.scene.Scene scene, float wx, float wz) {
        Node root = new Node("WaterTower");
        float wy = Terrain.groundY(wx, wz);
        root.setLocalTranslation(wx, wy, wz);
        Material metal = a.litColor(new ColorRGBA(0.18f, 0.16f, 0.14f, 1f));

        // Four legs.
        for (int dx = -1; dx <= 1; dx += 2) for (int dz = -1; dz <= 1; dz += 2) {
            Geometry leg = new Geometry("leg",
                    new Box(0.15f, 6f, 0.15f));
            leg.setLocalTranslation(dx * 2.2f, 6f, dz * 2.2f);
            leg.setMaterial(metal);
            root.attachChild(leg);
        }
        // Tank.
        Cylinder tank = new Cylinder(2, 20, 3.2f, 4f, true);
        Geometry tg = new Geometry("tank", tank);
        tg.setLocalRotation(new com.jme3.math.Quaternion().fromAngles(FastMath.HALF_PI, 0, 0));
        tg.setLocalTranslation(0, 14f, 0);
        tg.setMaterial(metal);
        root.attachChild(tg);
        scene.collision.add(wx, wz, 3.5f, 16f);
        return root;
    }

    private static Box boxWithUV(Vector3f size, Vector2f tiles) {
        Box b = new Box(size.x * 0.5f, size.y * 0.5f, size.z * 0.5f);
        b.scaleTextureCoordinates(tiles);
        return b;
    }

    private Landmarks() {}
}
