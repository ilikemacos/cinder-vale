package com.cindervale.engine.core;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

import com.cindervale.engine.assets.Assets;
import com.cindervale.engine.input.FpsCamera;
import com.cindervale.engine.scene.Scene;
import com.cindervale.engine.ui.PauseMenu;

/**
 * Cinder Engine entry point (doc §4). Owns the SimpleApplication (JME) instance,
 * builds the Scene + Assets facades, hands control to the Game, and runs the
 * update loop. Nothing in game/ imports com.jme3.* — it goes through here.
 */
public final class Engine {

    private final EngineConfig cfg;
    private final Game game;
    public final Time time = new Time();

    private Scene scene;
    private Assets assets;
    private JmeApp app;
    private PauseMenu pause;
    private FpsCamera camera;

    public Engine(EngineConfig cfg, Game game) {
        this.cfg = cfg;
        this.game = game;
    }

    /** Blocks until the window closes. */
    public void start() {
        app = new JmeApp();
        AppSettings s = new AppSettings(true);
        s.setTitle(cfg.title);
        s.setResolution(cfg.width, cfg.height);
        s.setVSync(cfg.vsync);
        s.setFullscreen(cfg.fullscreen);
        s.setSamples(cfg.samples);
        if (cfg.fpsCap > 0) s.setFrameRate(cfg.fpsCap);
        app.setSettings(s);
        app.setShowSettings(false);
        app.start();
    }

    public Scene scene() { return scene; }
    public Assets assets() { return assets; }
    public boolean isPaused() { return pause != null && pause.isOpen(); }

    /** Register a "fire" callback bound to left mouse — one shot per press. */
    public void registerFire(Runnable onFire) {
        if (app == null) return;
        var im = app.getInputManager();
        final String name = "cv_fire";
        im.addMapping(name, new com.jme3.input.controls.MouseButtonTrigger(
                com.jme3.input.MouseInput.BUTTON_LEFT));
        im.addListener((com.jme3.input.controls.ActionListener) (n, pressed, tpf) -> {
            if (pressed && !isPaused()) onFire.run();
        }, name);
    }

    /** Internal — JME app that hands lifecycle to the outer Engine + Game. */
    private final class JmeApp extends SimpleApplication {
        private float autoshotAt = -1f;         // seconds
        private String autoshotPath = null;

        @Override public void simpleInitApp() {
            assets = new Assets(getAssetManager());
            scene = new Scene(rootNode, cam, getAssetManager(), assets, viewPort);
            pause = new PauseMenu();
            stateManager.attach(pause);
            game.init(Engine.this, scene);
            // Camera controller attaches AFTER game.init, so game.init can position
            // the camera and FpsCamera seeds its yaw/pitch from that direction.
            camera = new FpsCamera();
            stateManager.attach(camera);
            // Bind the world so the camera clamps to terrain + slides on props.
            camera.bindWorld(scene.collision, scene.groundHeight);
            System.out.println("[Engine] up — " + renderer.getClass().getSimpleName()
                    + " · " + cfg.width + "x" + cfg.height);

            // Auto-shot hook: -Dcindervale.autoshot=2.0 -Dcindervale.shotpath=/tmp/x.png
            // ScreenshotAppState(dir, baseName) — appends numeric suffix + .png.
            String sec = System.getProperty("cindervale.autoshot");
            if (sec != null) {
                try {
                    autoshotAt = Float.parseFloat(sec);
                    String path = System.getProperty("cindervale.shotpath",
                            System.getProperty("user.home") + "/cinder-shot.png");
                    java.io.File p = new java.io.File(path);
                    String dir = (p.getParent() == null ? "." : p.getParent()) + "/";
                    String base = p.getName().replaceFirst("\\.png$", "");
                    var shot = new com.jme3.app.state.ScreenshotAppState(dir, base);
                    stateManager.attach(shot);
                    autoshotPath = dir + base + "0.png";
                    System.out.println("[Engine] autoshot armed t=" + autoshotAt
                            + "s -> " + autoshotPath);
                } catch (NumberFormatException ignored) {}
            }
        }
        @Override public void simpleUpdate(float dt) {
            time.tick(dt);
            // Test hook: -Dcindervale.autopause=3 opens the pause menu after 3s
            // so we can reproduce Esc-crash bugs without a real keypress.
            String ap = System.getProperty("cindervale.autopause");
            if (ap != null && time.uptime >= Float.parseFloat(ap) && pause != null && !pause.isOpen()) {
                System.setProperty("cindervale.autopause", "-1");
                try { pause.toggle(); }
                catch (Throwable t) { t.printStackTrace(System.err); throw t; }
            }
            boolean paused = isPaused();
            if (camera != null) camera.setInputEnabled(!paused);
            if (!paused) {
                if (camera != null) camera.tick(dt);
                game.update(dt);
            }
            if (autoshotAt > 0 && time.uptime >= autoshotAt) {
                autoshotAt = -1f;
                var shot = stateManager.getState(com.jme3.app.state.ScreenshotAppState.class);
                if (shot != null) shot.takeScreenshot();
                System.out.println("[Engine] shot taken -> " + autoshotPath);
            }
        }
        @Override public void destroy() {
            game.quit();
            super.destroy();
        }
    }
}
