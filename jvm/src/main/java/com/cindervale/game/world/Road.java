package com.cindervale.game.world;

import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import com.cindervale.engine.assets.Assets;

/**
 * Cracked-asphalt road strip that hugs the terrain. Deforms per-vertex to sit
 * ~5 cm above ground and follows a slightly winding centreline through the
 * valley (matches the Godot version's highway shape). Single mesh, one PBR
 * asphalt material — cheap.
 */
public final class Road {
    public static Geometry build(Assets a, float halfExtent, float width, float step) {
        int segs = (int) (halfExtent * 2f / step);
        int vpr = 2;  // left + right per segment
        int vCount = (segs + 1) * vpr;
        int iCount = segs * 6;

        Vector3f[] verts = new Vector3f[vCount];
        Vector2f[] uvs = new Vector2f[vCount];
        Vector3f[] norms = new Vector3f[vCount];
        int[] tris = new int[iCount];

        for (int i = 0; i <= segs; i++) {
            float z = -halfExtent + i * step;
            float cx = com.jme3.math.FastMath.sin(z * 0.03f) * 4f;  // gentle winding
            float y = WorldConfig.height(cx, z) + 0.05f;
            verts[i * 2]     = new Vector3f(cx - width * 0.5f, y, z);
            verts[i * 2 + 1] = new Vector3f(cx + width * 0.5f, y, z);
            uvs[i * 2]       = new Vector2f(0f, z * 0.15f);
            uvs[i * 2 + 1]   = new Vector2f(1f, z * 0.15f);
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

        Geometry g = new Geometry("Road", m);
        Material mat = a.pbr("asphalt_02");
        g.setMaterial(mat);
        return g;
    }

    private Road() {}
}
