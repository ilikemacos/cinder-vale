package com.cindervale.engine.assets;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * Asset access (doc §14). Wraps JME's AssetManager with:
 *  - path helpers for the Poly Haven pack layout,
 *  - a Spatial + Material cache (loadModel returns fresh clones so instances
 *    can have independent transforms while sharing meshes/textures),
 *  - factory for PBR-lit materials from a diff/normal/rough triple.
 *
 * Game code never touches JME AssetManager — it goes through here.
 */
public final class Assets {

    private static final String ART_ROOT = "assets/env/cinder-vale-env-art/";

    private final AssetManager jme;
    private final Map<String, Spatial> modelCache = new HashMap<>();
    private final Map<String, Material> matCache = new HashMap<>();

    public Assets(AssetManager jme) {
        this.jme = jme;
        this.jme.registerLocator("src/main/resources", com.jme3.asset.plugins.FileLocator.class);
        this.jme.registerLocator("/", com.jme3.asset.plugins.ClasspathLocator.class);
    }

    /** name = folder under models/ (e.g. "Barrel_02"). Returns a fresh clone. */
    public Spatial loadModel(String name) {
        String path = ART_ROOT + "models/" + name + "/" + name + "_1k.gltf";
        Spatial proto = modelCache.get(path);
        if (proto == null) {
            proto = jme.loadModel(path);
            modelCache.put(path, proto);
        }
        return proto.clone();
    }

    /** PBR lighting material built from a Poly Haven texture set (diff/nor_gl/rough). */
    public Material pbr(String texSet) {
        Material cached = matCache.get(texSet);
        if (cached != null) return cached;
        String base = ART_ROOT + "textures/" + texSet + "/" + texSet;
        Material m = new Material(jme, "Common/MatDefs/Light/PBRLighting.j3md");
        m.setTexture("BaseColorMap", tex(base + "_diff_2k.jpg", true));
        m.setTexture("NormalMap", tex(base + "_nor_gl_2k.jpg", false));
        m.setTexture("RoughnessMap", tex(base + "_rough_2k.jpg", false));
        m.setFloat("Metallic", 0.0f);
        m.setColor("BaseColor", ColorRGBA.White);
        matCache.put(texSet, m);
        return m;
    }

    /** Uniform-coloured lit material for graybox blockouts (no textures). */
    public Material litColor(ColorRGBA color) {
        Material m = new Material(jme, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color);
        m.setColor("Ambient", color);
        return m;
    }

    private Texture tex(String path, boolean srgb) {
        Texture t = jme.loadTexture(path);
        t.setWrap(Texture.WrapMode.Repeat);
        return t;
    }

    /** Apply a material to every Geometry under a spatial (kits often nest meshes). */
    public static void applyMaterial(Spatial s, Material m) {
        if (s instanceof Geometry g) { g.setMaterial(m); return; }
        if (s instanceof Node n) for (Spatial c : n.getChildren()) applyMaterial(c, m);
    }

    public AssetManager jme() { return jme; }
}
