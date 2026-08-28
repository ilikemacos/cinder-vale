package com.cindervale;

import com.cindervale.engine.core.Engine;
import com.cindervale.engine.core.EngineConfig;
import com.cindervale.game.CinderValeDemo;

/**
 * Cinder Vale entry point. All engine machinery lives in com.cindervale.engine.*;
 * the game lives in com.cindervale.game.*. Main just wires them together (doc §36).
 */
public class Main {
    public static void main(String[] args) {
        EngineConfig cfg = new EngineConfig();
        cfg.title = "Cinder Vale";
        cfg.width = intProp("cindervale.width", 1280);
        cfg.height = intProp("cindervale.height", 720);
        cfg.fullscreen = boolProp("cindervale.fullscreen", false);
        new Engine(cfg, new CinderValeDemo()).start();
    }

    private static int intProp(String k, int d) {
        try { return Integer.parseInt(System.getProperty(k, "")); }
        catch (NumberFormatException e) { return d; }
    }
    private static boolean boolProp(String k, boolean d) {
        String v = System.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v);
    }
}
