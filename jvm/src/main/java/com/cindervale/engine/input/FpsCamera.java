package com.cindervale.engine.input;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * WASD + mouselook camera controller that moves in the XZ plane — so W walks
 * *forward on the ground*, not down toward wherever you're looking. Replaces
 * JME's default FlyByCamera behaviour, which is camera-relative and dives you
 * into the terrain the moment you angle the view down.
 *
 * Space rises / Ctrl (or C) descends (temporary — becomes jump/crouch once the
 * character controller lands). Shift sprints. Mouse pitch is clamped.
 */
public final class FpsCamera extends BaseAppState {

    // Bindings — the "cv_" prefix keeps them out of anyone else's namespace.
    private static final String F = "cv_fwd", B = "cv_back", L = "cv_left", R = "cv_right";
    private static final String UP = "cv_up", DOWN = "cv_down", SPRINT = "cv_sprint";
    private static final String YAW_L = "cv_yaw_l", YAW_R = "cv_yaw_r";
    private static final String PITCH_U = "cv_pitch_u", PITCH_D = "cv_pitch_d";

    public float walkSpeed = 6f;
    public float sprintMultiplier = 2.2f;
    public float mouseSensitivity = 1.6f;

    private SimpleApplication app;
    private Camera cam;
    private float yaw = 0f, pitch = 0f;
    private boolean enabledInput = true;

    // Key state — updated by ActionListener; polled by tick().
    private boolean kF, kB, kL, kR, kU, kD, kSprint;

    private final Vector3f tmpFwd = new Vector3f();
    private final Vector3f tmpRight = new Vector3f();

    @Override protected void initialize(Application a) {
        this.app = (SimpleApplication) a;
        this.cam = app.getCamera();
        // Kill the default flyCam — it fights us for WASD + look.
        if (app.getFlyByCamera() != null) app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(false);

        // Seed yaw/pitch from the starting camera direction so we don't snap.
        Vector3f dir = cam.getDirection();
        yaw = FastMath.atan2(-dir.x, -dir.z);
        pitch = FastMath.asin(dir.y);

        InputManager im = app.getInputManager();
        im.addMapping(F, new KeyTrigger(KeyInput.KEY_W));
        im.addMapping(B, new KeyTrigger(KeyInput.KEY_S));
        im.addMapping(L, new KeyTrigger(KeyInput.KEY_A));
        im.addMapping(R, new KeyTrigger(KeyInput.KEY_D));
        im.addMapping(UP, new KeyTrigger(KeyInput.KEY_SPACE));
        im.addMapping(DOWN, new KeyTrigger(KeyInput.KEY_LCONTROL),
                new KeyTrigger(KeyInput.KEY_C));
        im.addMapping(SPRINT, new KeyTrigger(KeyInput.KEY_LSHIFT));

        im.addMapping(YAW_L, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        im.addMapping(YAW_R, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        im.addMapping(PITCH_U, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        im.addMapping(PITCH_D, new MouseAxisTrigger(MouseInput.AXIS_Y, true));

        im.addListener(analog, YAW_L, YAW_R, PITCH_U, PITCH_D);
        im.addListener(action, F, B, L, R, UP, DOWN, SPRINT);
    }

    private final ActionListener action = (name, pressed, tpf) -> {
        switch (name) {
            case F -> kF = pressed;
            case B -> kB = pressed;
            case L -> kL = pressed;
            case R -> kR = pressed;
            case UP -> kU = pressed;
            case DOWN -> kD = pressed;
            case SPRINT -> kSprint = pressed;
        }
    };

    @Override protected void cleanup(Application a) {
        InputManager im = app.getInputManager();
        for (String n : new String[]{F, B, L, R, UP, DOWN, SPRINT,
                YAW_L, YAW_R, PITCH_U, PITCH_D}) {
            if (im.hasMapping(n)) im.deleteMapping(n);
        }
        im.removeListener(analog);
        im.removeListener(action);
    }
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    /** Called from Engine each frame; skipped while paused. */
    public void tick(float dt) {
        if (!enabledInput) return;
        applyLook();

        // XZ-plane movement basis derived from yaw only (Y stays 0).
        tmpFwd.set(-FastMath.sin(yaw), 0f, -FastMath.cos(yaw));
        tmpRight.set(FastMath.cos(yaw), 0f, -FastMath.sin(yaw));

        Vector3f v = new Vector3f();
        if (kF) v.addLocal(tmpFwd);
        if (kB) v.subtractLocal(tmpFwd);
        if (kR) v.addLocal(tmpRight);
        if (kL) v.subtractLocal(tmpRight);
        if (v.lengthSquared() > 0.0001f) v.normalizeLocal();

        if (kU) v.y += 1f;
        if (kD) v.y -= 1f;

        float speed = walkSpeed * (kSprint ? sprintMultiplier : 1f);
        v.multLocal(speed * dt);
        cam.setLocation(cam.getLocation().add(v));
    }

    /** Enable/disable input (e.g. when the pause menu is open). */
    public void setInputEnabled(boolean on) {
        enabledInput = on;
        app.getInputManager().setCursorVisible(!on);
    }

    private void applyLook() {
        // Direction from yaw/pitch (right-handed, y up).
        float cy = FastMath.cos(yaw), sy = FastMath.sin(yaw);
        float cp = FastMath.cos(pitch), sp = FastMath.sin(pitch);
        Vector3f dir = new Vector3f(-sy * cp, sp, -cy * cp);
        cam.lookAtDirection(dir, Vector3f.UNIT_Y);
    }

    private final AnalogListener analog = (name, value, tpf) -> {
        if (!enabledInput) return;
        switch (name) {
            case YAW_L -> yaw += value * mouseSensitivity;
            case YAW_R -> yaw -= value * mouseSensitivity;
            case PITCH_U -> pitch = clampPitch(pitch + value * mouseSensitivity);
            case PITCH_D -> pitch = clampPitch(pitch - value * mouseSensitivity);
        }
    };

    private static float clampPitch(float p) {
        float max = FastMath.HALF_PI - 0.05f;
        return Math.max(-max, Math.min(max, p));
    }
}
