package com.cindervale.engine.input;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Cheap static-obstacle collision — a grid of vertical cylinder colliders
 * (props are treated as pillars). Registered once at scene build; the
 * character controller queries getBlockedPosition() each move to slide along
 * obstacles instead of walking through them.
 *
 * Not physically accurate — but for a wasteland with barrels, trash cans and
 * boulders it reads correctly, is O(k) per move where k = props in the
 * current spatial-hash cell, and needs zero native dependencies.
 */
public final class CollisionWorld {

    private static final float CELL = 8f;

    public record Obstacle(float x, float z, float radius, float height) {}

    // Spatial hash: (cellX, cellZ) key -> list of obstacles.
    private final java.util.HashMap<Long, List<Obstacle>> cells = new java.util.HashMap<>();

    public void add(float x, float z, float radius, float height) {
        Obstacle o = new Obstacle(x, z, radius, height);
        int cx = (int) Math.floor(x / CELL);
        int cz = (int) Math.floor(z / CELL);
        // Insert into every cell the disc overlaps.
        int r = (int) Math.ceil((radius + 1f) / CELL);
        for (int dz = -r; dz <= r; dz++) {
            for (int dx = -r; dx <= r; dx++) {
                cells.computeIfAbsent(key(cx + dx, cz + dz), k -> new ArrayList<>()).add(o);
            }
        }
    }

    /** Given a desired new position, return a slid one that respects obstacles. */
    public Vector3f resolve(Vector3f from, Vector3f to, float capsuleRadius) {
        Vector3f out = to.clone();
        // Two passes so a wall can push us, then a second wall we now touch can too.
        for (int i = 0; i < 2; i++) {
            int cx = (int) Math.floor(out.x / CELL);
            int cz = (int) Math.floor(out.z / CELL);
            List<Obstacle> list = cells.get(key(cx, cz));
            if (list == null) continue;
            for (Obstacle o : list) {
                float dx = out.x - o.x;
                float dz = out.z - o.z;
                float d2 = dx * dx + dz * dz;
                float min = o.radius + capsuleRadius;
                if (d2 < min * min && d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    float push = min - d;
                    out.x += (dx / d) * push;
                    out.z += (dz / d) * push;
                }
            }
        }
        return out;
    }

    public int size() {
        int n = 0;
        for (List<Obstacle> l : cells.values()) n += l.size();
        return n;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}
