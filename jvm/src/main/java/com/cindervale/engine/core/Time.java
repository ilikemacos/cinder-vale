package com.cindervale.engine.core;

/**
 * Engine time state — updated by GameLoop each frame. Kept as a small POJO so
 * game systems can read dt/uptime without touching JME internals.
 */
public final class Time {
    public float dt = 0f;         // seconds since last frame
    public float uptime = 0f;     // seconds since engine start
    public long frame = 0L;       // frame counter
    public float fixedStep = 1f / 60f;   // physics/AI fixed step, doc §5

    void tick(float delta) {
        dt = delta;
        uptime += delta;
        frame++;
    }
}
