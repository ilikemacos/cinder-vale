package com.cindervale.engine.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.List;

/**
 * Esc pause menu — amber/mono HearthLink chrome, rendered with JME's built-in
 * BitmapText so no extra UI dep is needed. Attach once via
 * stateManager.attach(new PauseMenu()); Escape toggles.
 *
 * When open: game update is skipped by CinderValeDemo (checks isPaused()),
 * mouse is released, and clicks route to menu items.
 */
public final class PauseMenu extends BaseAppState {

    private static final String INPUT_ESC = "pause_menu_esc";
    private static final ColorRGBA AMBER = new ColorRGBA(0.96f, 0.76f, 0.36f, 1f);
    private static final ColorRGBA INK = new ColorRGBA(0.05f, 0.05f, 0.06f, 0.82f);
    private static final ColorRGBA TEXT = new ColorRGBA(0.90f, 0.90f, 0.92f, 1f);

    private SimpleApplication app;
    private Node gui;
    private Node overlay;                      // detach to close
    private final List<MenuItem> items = new ArrayList<>();
    private int hover = 0;
    private boolean open = false;
    private RawInputListener mouseListener;

    private static final class MenuItem {
        final String label;
        final Runnable action;
        BitmapText txt;
        float y;
        MenuItem(String l, Runnable a) { label = l; action = a; }
    }

    @Override protected void initialize(Application a) {
        this.app = (SimpleApplication) a;
        this.gui = app.getGuiNode();

        items.add(new MenuItem("RESUME", this::close));
        items.add(new MenuItem("SETTINGS", () -> System.out.println("[PauseMenu] settings TBD")));
        items.add(new MenuItem("QUIT TO LAUNCHER", () -> System.exit(2)));
        items.add(new MenuItem("QUIT GAME", () -> System.exit(0)));

        app.getInputManager().addMapping(INPUT_ESC, new KeyTrigger(com.jme3.input.KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(escListener, INPUT_ESC);
    }
    @Override protected void cleanup(Application a) {
        app.getInputManager().deleteMapping(INPUT_ESC);
        app.getInputManager().removeListener(escListener);
    }
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    public boolean isOpen() { return open; }

    private final ActionListener escListener = (name, pressed, tpf) -> {
        if (pressed && INPUT_ESC.equals(name)) {
            if (open) close(); else open();
        }
    };

    private void open() {
        if (open) return;
        open = true;
        buildOverlay();
        // Release the mouse for menu interaction; disable flycam.
        app.getInputManager().setCursorVisible(true);
        if (app.getFlyByCamera() != null) app.getFlyByCamera().setEnabled(false);

        mouseListener = new SimpleMouseListener();
        app.getInputManager().addRawInputListener(mouseListener);
    }

    private void close() {
        if (!open) return;
        open = false;
        if (overlay != null) { gui.detachChild(overlay); overlay = null; }
        app.getInputManager().setCursorVisible(false);
        if (app.getFlyByCamera() != null) app.getFlyByCamera().setEnabled(true);
        if (mouseListener != null) {
            app.getInputManager().removeRawInputListener(mouseListener);
            mouseListener = null;
        }
    }

    private void buildOverlay() {
        overlay = new Node("PauseOverlay");
        int W = app.getCamera().getWidth();
        int H = app.getCamera().getHeight();

        // Full-screen dim.
        overlay.attachChild(quad(0, 0, W, H, new ColorRGBA(0, 0, 0, 0.55f)));

        // Amber bracket frame around the menu box.
        int boxW = 480, boxH = 380;
        int bx = (W - boxW) / 2, by = (H - boxH) / 2;
        overlay.attachChild(quad(bx, by, boxW, boxH, INK));
        // Cut-corner accent bars.
        overlay.attachChild(quad(bx, by + boxH - 2, boxW, 2, AMBER));
        overlay.attachChild(quad(bx, by, boxW, 2, AMBER));
        overlay.attachChild(quad(bx, by, 2, boxH, AMBER));
        overlay.attachChild(quad(bx + boxW - 2, by, 2, boxH, AMBER));

        // Title.
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        BitmapText title = new BitmapText(font);
        title.setText("PAUSED");
        title.setSize(38);
        title.setColor(AMBER);
        title.setLocalTranslation(bx + 30, by + boxH - 30, 0);
        overlay.attachChild(title);

        BitmapText sub = new BitmapText(font);
        sub.setText("HEARTHLINK  //  main menu");
        sub.setSize(14);
        sub.setColor(new ColorRGBA(0.55f, 0.44f, 0.22f, 1f));
        sub.setLocalTranslation(bx + 30, by + boxH - 76, 0);
        overlay.attachChild(sub);

        // Items.
        float itemY = by + boxH - 130;
        for (MenuItem it : items) {
            BitmapText t = new BitmapText(font);
            t.setText("▸  " + it.label);
            t.setSize(22);
            t.setColor(TEXT);
            t.setLocalTranslation(bx + 40, itemY, 0);
            overlay.attachChild(t);
            it.txt = t;
            it.y = itemY;
            itemY -= 46;
        }
        gui.attachChild(overlay);
        highlight(0);
    }

    private void highlight(int idx) {
        hover = Math.max(0, Math.min(items.size() - 1, idx));
        for (int i = 0; i < items.size(); i++) {
            var it = items.get(i);
            it.txt.setColor(i == hover ? AMBER : TEXT);
        }
    }

    private Geometry quad(int x, int y, int w, int h, ColorRGBA c) {
        Quad q = new Quad(w, h);
        Geometry g = new Geometry("q", q);
        Material m = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", c);
        m.getAdditionalRenderState().setBlendMode(
                com.jme3.material.RenderState.BlendMode.Alpha);
        g.setMaterial(m);
        g.setQueueBucket(RenderQueue.Bucket.Gui);
        g.setLocalTranslation(x, y, 0);
        return g;
    }

    /** Cursor-drives-hover, click-fires-action. */
    private final class SimpleMouseListener implements RawInputListener {
        @Override public void onMouseMotionEvent(MouseMotionEvent e) {
            int H = app.getCamera().getHeight();
            int mx = e.getX(), my = e.getY();  // JME: y from bottom
            int best = -1;
            for (int i = 0; i < items.size(); i++) {
                var it = items.get(i);
                float itemLeft = it.txt.getLocalTranslation().x;
                float itemTop = it.y;
                float itemBot = it.y - 28;
                if (mx >= itemLeft && mx <= itemLeft + 420
                        && my <= itemTop && my >= itemBot) {
                    best = i;
                    break;
                }
            }
            if (best >= 0) highlight(best);
        }
        @Override public void onMouseButtonEvent(MouseButtonEvent e) {
            if (e.isPressed() && e.getButtonIndex() == MouseInput.BUTTON_LEFT
                    && hover >= 0 && hover < items.size()) {
                items.get(hover).action.run();
            }
        }
        @Override public void beginInput() {} @Override public void endInput() {}
        @Override public void onJoyAxisEvent(JoyAxisEvent e) {} @Override public void onJoyButtonEvent(JoyButtonEvent e) {}
        @Override public void onKeyEvent(KeyInputEvent e) {}
        @Override public void onTouchEvent(TouchEvent e) {}
    }
}
