package com.cindervale.game.enemies;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import com.cindervale.game.world.Terrain;

/**
 * Wasteland enemy base — Node holding the visual mesh, plus a small AI state
 * machine (idle → chase → attack → dead). Slides on terrain via a ground-clamp
 * every tick. All enemies live in one shared list (Spawner.all) that the game
 * loop ticks each frame; the shared list makes it cheap for the rifle raycast
 * to iterate and hit-test them.
 */
public abstract class Enemy {

    public final Node node;
    public final float capsuleRadius;   // horizontal collider radius (m)
    public final float attackRange;
    public final float attackDamage;
    public final float speed;
    public float health;
    public boolean alive = true;

    protected State state = State.IDLE;
    protected float cooldown = 0f;

    public enum State { IDLE, CHASE, ATTACK, DEAD }

    protected Enemy(Node node, float radius, float speed, float health,
                     float attackRange, float attackDamage) {
        this.node = node;
        this.capsuleRadius = radius;
        this.speed = speed;
        this.health = health;
        this.attackRange = attackRange;
        this.attackDamage = attackDamage;
    }

    /** Called each frame by Spawner. player = player position; onHit = "dealt N damage". */
    public void tick(float dt, Vector3f player, java.util.function.DoubleConsumer onHit) {
        if (!alive) return;

        Vector3f pos = node.getLocalTranslation();
        Vector3f to = new Vector3f(player.x - pos.x, 0f, player.z - pos.z);
        float dist = to.length();
        cooldown = Math.max(0f, cooldown - dt);

        if (dist < 30f) {
            if (dist <= attackRange) state = State.ATTACK;
            else state = State.CHASE;
        } else {
            state = State.IDLE;
        }

        switch (state) {
            case CHASE -> {
                to.normalizeLocal();
                Vector3f next = new Vector3f(
                        pos.x + to.x * speed * dt,
                        Terrain.groundY(pos.x + to.x * speed * dt, pos.z + to.z * speed * dt),
                        pos.z + to.z * speed * dt);
                node.setLocalTranslation(next);
                // Face the player.
                float yaw = FastMath.atan2(to.x, to.z);
                node.setLocalRotation(new Quaternion().fromAngles(0, yaw, 0));
                afterMove(dt, true);
            }
            case ATTACK -> {
                float yaw = FastMath.atan2(to.x, to.z);
                node.setLocalRotation(new Quaternion().fromAngles(0, yaw, 0));
                if (cooldown <= 0f) {
                    cooldown = 1.2f;
                    onHit.accept(attackDamage);
                }
                afterMove(dt, false);
            }
            default -> afterMove(dt, false);
        }
    }

    /** Subclasses override to animate legs, gait bob, tail sway, etc. */
    protected void afterMove(float dt, boolean moving) {}

    public void takeDamage(float amount) {
        if (!alive) return;
        health -= amount;
        if (health <= 0f) die();
    }

    protected void die() {
        alive = false;
        state = State.DEAD;
        // Fall over; leave the corpse for a beat.
        node.setLocalRotation(new Quaternion().fromAngles(0, 0, FastMath.HALF_PI));
    }

    /** World-space centre of the hit capsule (for raycast tests). */
    public Vector3f centre() {
        Vector3f p = node.getLocalTranslation();
        return new Vector3f(p.x, p.y + 0.9f, p.z);
    }
}
