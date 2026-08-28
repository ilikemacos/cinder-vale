package com.cindervale.engine.scene;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;

import com.cindervale.engine.assets.Assets;

/**
 * The game's view of the world (doc §6). Wraps JME's rootNode + camera + lights
 * so game code never imports JME node types directly. Add/remove entities here,
 * set the sky, place the sun — nothing else touches the renderer.
 */
public final class Scene {

    public final Node root;
    public final Camera cam;
    public final Assets assets;
    private final AssetManager jmeAssets;

    private DirectionalLight sun;
    private AmbientLight ambient;
    private Spatial sky;

    public Scene(Node root, Camera cam, AssetManager jmeAssets, Assets assets) {
        this.root = root;
        this.cam = cam;
        this.jmeAssets = jmeAssets;
        this.assets = assets;
    }

    /** Add sun + ambient with sensible wasteland defaults. */
    public void addDefaultLighting() {
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4f, -0.85f, -0.35f).normalizeLocal());
        sun.setColor(ColorRGBA.White.mult(1.15f));
        root.addLight(sun);
        ambient = new AmbientLight(ColorRGBA.White.mult(0.55f));
        root.addLight(ambient);
    }

    /** Load an equirectangular HDR/JPG as skybox. Returns true if it took. */
    public boolean setSkyEquirect(String path) {
        try {
            sky = SkyFactory.createSky(jmeAssets, path,
                    SkyFactory.EnvMapType.EquirectMap);
            root.attachChild(sky);
            return true;
        } catch (Exception e) {
            System.err.println("[Scene] sky load failed: " + path + " — " + e.getMessage());
            return false;
        }
    }

    public void add(Spatial s)    { root.attachChild(s); }
    public void remove(Spatial s) { root.detachChild(s); }

    public DirectionalLight sun()  { return sun; }
    public AmbientLight ambient()  { return ambient; }
}
