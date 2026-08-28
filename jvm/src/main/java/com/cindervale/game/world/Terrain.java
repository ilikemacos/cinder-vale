package com.cindervale.game.world;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import com.cindervale.engine.assets.Assets;

/**
 * Builds one big rolling-hills terrain mesh from WorldConfig.height, tessellated
 * with a fixed quad size. Two-uv-set: base PBR (dry_ground_rocks) tiled tight
 * for close-in detail, and a coarser variation is done in shader-space later.
 * Single mesh — v1 skips streaming; the whole 200×200 valley fits in memory.
 */
public final class Terrain {

    /** Build a terrain Geometry covering [-halfExtent, halfExtent] on X and Z. */
    public static Geometry build(Assets a, float halfExtent, float quadSize) {
        int n = (int) (halfExtent * 2f / quadSize);          // vertices per edge - 1
        int vpr = n + 1;                                     // vertices per row
        int vCount = vpr * vpr;
        int iCount = n * n * 6;

        Vector3f[] verts = new Vector3f[vCount];
        Vector2f[] uvs = new Vector2f[vCount];
        Vector3f[] norms = new Vector3f[vCount];
        int[] tris = new int[iCount];

        float uvScale = 0.5f;  // tiles per metre — 2K stays crisp

        int vi = 0;
        for (int iz = 0; iz <= n; iz++) {
            for (int ix = 0; ix <= n; ix++) {
                float wx = -halfExtent + ix * quadSize;
                float wz = -halfExtent + iz * quadSize;
                float wy = WorldConfig.height(wx, wz);
                verts[vi] = new Vector3f(wx, wy, wz);
                uvs[vi] = new Vector2f(wx * uvScale, wz * uvScale);
                // Cheap normal via central differences.
                float e = quadSize;
                float hL = WorldConfig.height(wx - e, wz);
                float hR = WorldConfig.height(wx + e, wz);
                float hD = WorldConfig.height(wx, wz - e);
                float hU = WorldConfig.height(wx, wz + e);
                Vector3f nrm = new Vector3f(hL - hR, 2f * e, hD - hU);
                nrm.normalizeLocal();
                norms[vi] = nrm;
                vi++;
            }
        }

        int ti = 0;
        for (int iz = 0; iz < n; iz++) {
            for (int ix = 0; ix < n; ix++) {
                int a0 = iz * vpr + ix;
                int b = a0 + 1;
                int c = a0 + vpr;
                int d = c + 1;
                tris[ti++] = a0; tris[ti++] = c; tris[ti++] = b;
                tris[ti++] = b;  tris[ti++] = c; tris[ti++] = d;
            }
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(verts));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(norms));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(tris));
        mesh.updateBound();

        Geometry g = new Geometry("Terrain", mesh);
        Material m = a.pbr("dry_ground_rocks");
        g.setMaterial(m);
        return g;
    }

    /** Runtime lookup so scatter/props/spawn sit at ground height. */
    public static float groundY(float x, float z) {
        return WorldConfig.height(x, z);
    }

    private Terrain() {}
}
