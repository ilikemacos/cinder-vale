package com.cindervale.engine.core;

/**
 * Boot-time engine configuration (doc §4). Passed to Engine.start.
 * Mutable up to start(); frozen afterwards.
 */
public final class EngineConfig {
    public String title = "Cinder Vale";
    public int width = 1280;
    public int height = 720;
    public boolean vsync = true;
    public boolean fullscreen = false;
    public int samples = 0;    // MSAA; keep 0 on M1 by default
    public int fpsCap = 0;     // 0 = uncapped
}
