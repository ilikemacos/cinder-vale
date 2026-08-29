package com.cindervale.game.enemies;

import com.jme3.math.Vector3f;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.scene.Scene;
import com.cindervale.game.world.Terrain;
import com.cindervale.game.world.WorldConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wasteland enemy spawner + tick manager. Places a mix of zombies and
 * reptile predators at seeded random locations (away from spawn), ticks their
 * AI each frame, and exposes the enemy list to the rifle for hit tests.
 */
public final class Spawner {

    public final List<Enemy> all = new ArrayList<>();
    private float damageAccumulator = 0f;

    public void spawn(Scene scene, Assets a, int zombies, int predators) {
        Random rng = new Random(20260828L * 31);
        // One zombie right in front of spawn so the player *sees* combat exists.
        {
            Zombie z = new Zombie(a);
            float x = WorldConfig.SPAWN.x + 2f;
            float zpos = WorldConfig.SPAWN.y - 12f;   // 12m ahead
            z.node.setLocalTranslation(x, Terrain.groundY(x, zpos), zpos);
            scene.add(z.node);
            all.add(z);
        }
        for (int i = 0; i < zombies - 1; i++) {
            Vector3f p = pickSpot(rng, 25f);
            if (p == null) continue;
            Zombie z = new Zombie(a);
            z.node.setLocalTranslation(p.x, Terrain.groundY(p.x, p.z), p.z);
            scene.add(z.node);
            all.add(z);
        }
        for (int i = 0; i < predators; i++) {
            Vector3f p = pickSpot(rng, 45f);
            if (p == null) continue;
            Predator pr = new Predator(a);
            pr.node.setLocalTranslation(p.x, Terrain.groundY(p.x, p.z), p.z);
            scene.add(pr.node);
            all.add(pr);
        }
    }

    /** Sample a buildable spot at least minDist metres from spawn. */
    private static Vector3f pickSpot(Random rng, float minDist) {
        Vector3f spawn = new Vector3f(WorldConfig.SPAWN.x, 0, WorldConfig.SPAWN.y);
        for (int tries = 0; tries < 20; tries++) {
            float x = (rng.nextFloat() - 0.5f) * (WorldConfig.WORLD_HALF * 1.8f);
            float z = (rng.nextFloat() - 0.5f) * (WorldConfig.WORLD_HALF * 1.8f);
            if (!WorldConfig.isBuildable(x, z)) continue;
            if (WorldConfig.slope01(x, z) > 0.4f) continue;
            Vector3f p = new Vector3f(x, 0, z);
            if (p.distance(spawn) < minDist) continue;
            return p;
        }
        return null;
    }

    /** Tick all alive enemies. Player damage is accumulated then flushed via drainDamage(). */
    public void tick(float dt, Vector3f player) {
        for (Enemy e : all) {
            if (!e.alive) continue;
            e.tick(dt, player, dmg -> damageAccumulator += (float) dmg);
        }
    }

    public float drainDamage() {
        float d = damageAccumulator;
        damageAccumulator = 0f;
        return d;
    }

    /** Ray hit-test: nearest alive enemy under the ray, or null. */
    public Enemy raycast(Vector3f origin, Vector3f dir, float maxDist) {
        Enemy best = null;
        float bestT = maxDist;
        for (Enemy e : all) {
            if (!e.alive) continue;
            Vector3f c = e.centre();
            Vector3f oc = new Vector3f(origin.x - c.x, origin.y - c.y, origin.z - c.z);
            float a = dir.dot(dir);
            float b = 2f * oc.dot(dir);
            float cc = oc.dot(oc) - e.capsuleRadius * e.capsuleRadius * 2.5f; // capsule fudge
            float disc = b * b - 4f * a * cc;
            if (disc < 0) continue;
            float t = (-b - (float) Math.sqrt(disc)) / (2f * a);
            if (t > 0 && t < bestT) { best = e; bestT = t; }
        }
        return best;
    }
}
