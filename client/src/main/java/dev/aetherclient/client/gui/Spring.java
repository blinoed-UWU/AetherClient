package dev.aetherclient.client.gui;

/**
 * Lightweight damped spring for fluid, tactile micro-animations in the ClickGUI.
 * Drives scale bounces, indicator slides, and context-menu entrances without
 * allocating per-frame garbage.
 */
public final class Spring {
    private float pos;
    private float vel;
    private float target;
    private final float stiffness;
    private final float damping;

    public Spring(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void update(float dt) {
        // Clamp dt: the explicit Euler integrator is only stable while
        // dt < 2 / stiffness; a lag spike would otherwise make pos/vel explode.
        if (dt > 1f / 30f) dt = 1f / 30f;
        if (dt < 0f) dt = 0f;
        float accel = (target - pos) * stiffness - vel * damping;
        vel += accel * dt;
        pos += vel * dt;
    }

    public void impulse(float v) {
        vel += v;
    }

    public void setTarget(float t) {
        this.target = t;
    }

    public float getPos() {
        return pos;
    }

    public void snap(float value) {
        pos = value;
        vel = 0f;
        target = value;
    }
}
