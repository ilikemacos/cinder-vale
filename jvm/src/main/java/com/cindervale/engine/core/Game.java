package com.cindervale.engine.core;

import com.cindervale.engine.scene.Scene;

/**
 * Game hook (doc §36 engine/game separation). The engine calls init/update/quit;
 * the game layer never touches JME directly — it goes through Scene + the other
 * engine services. Swap the underlying renderer without touching game code.
 */
public interface Game {
    /** Called once after the engine + scene are ready. */
    void init(Engine engine, Scene scene);

    /** Called every frame with delta seconds. Physics/AI use engine.time.fixedStep. */
    void update(float dt);

    /** Called on shutdown for game-side cleanup. */
    default void quit() {}
}
