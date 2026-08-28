package com.cindervale.game.world;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

/**
 * Deterministic world description — ported from the Godot version's
 * WorldConfig.gd. Height is a pure function of world (x,z) so tiles seam
 * exactly and scatter is stable across restarts. The wasteland is centred on
 * the origin and extends WORLD_HALF in every direction.
 */
public final class WorldConfig {
    public static final float WORLD_HALF = 100f;   // 200 m x 200 m playfield
    public static final float BASE = 0.5f;         // valley-floor elevation above waterline
    public static final float AMPLITUDE = 4.0f;    // rolling hill height (m)
    public static final float RIVER_HALF_W = 12f;  // dry riverbed half-width

    /** Two-octave value noise, seeded and cheap. */
    public static float noise(float x, float z, int seed) {
        int xi = (int) FastMath.floor(x);
        int zi = (int) FastMath.floor(z);
        float xf = x - xi, zf = z - zi;
        float n00 = hash(xi, zi, seed),      n10 = hash(xi + 1, zi, seed);
        float n01 = hash(xi, zi + 1, seed),  n11 = hash(xi + 1, zi + 1, seed);
        float u = smooth(xf), v = smooth(zf);
        float nx0 = FastMath.interpolateLinear(u, n00, n10);
        float nx1 = FastMath.interpolateLinear(u, n01, n11);
        return FastMath.interpolateLinear(v, nx0, nx1) * 2f - 1f;
    }

    private static float smooth(float t) { return t * t * (3f - 2f * t); }

    private static float hash(int x, int z, int seed) {
        int h = x * 374761393 + z * 668265263 + seed * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);
        return (h & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }

    /** Terrain height at world (x,z). Rolling dunes rising toward the edges. */
    public static float height(float x, float z) {
        float n1 = noise(x * 0.06f, z * 0.06f, 4471);          // large rolls
        float n2 = noise(x * 0.22f, z * 0.22f, 991) * 0.35f;   // texture
        float ridge = FastMath.sqr(Math.abs(x) / WORLD_HALF) * 6f;  // rise to edges
        float h = BASE + (n1 + n2) * AMPLITUDE + ridge;
        // Dry riverbed carves ~1.4m below through the middle (winding N-S).
        float rx = FastMath.sin(z * 0.05f) * 8f;
        float d = Math.abs(x - rx);
        if (d < RIVER_HALF_W) {
            float t = d / RIVER_HALF_W;
            h -= (1f - t * t) * 1.4f;
        }
        return h;
    }

    /** Approximate surface normal via central differences. */
    public static float slope01(float x, float z) {
        float e = 1.2f;
        float dx = height(x + e, z) - height(x - e, z);
        float dz = height(x, z + e) - height(x, z - e);
        float g = (float) Math.sqrt(dx * dx + dz * dz) / (2f * e);
        return Math.min(1f, g);
    }

    /** True if (x,z) is on the buildable ground and clear of the riverbed. */
    public static boolean isBuildable(float x, float z) {
        float rx = FastMath.sin(z * 0.05f) * 8f;
        return Math.abs(x - rx) > RIVER_HALF_W + 2f;
    }

    /** Bus-wreck / player spawn (offset from centre, on the road). */
    public static final Vector2f SPAWN = new Vector2f(-4f, 40f);

    private WorldConfig() {}
}
