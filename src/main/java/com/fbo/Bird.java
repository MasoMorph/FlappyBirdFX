package com.fbo;

import com.fbo.graphics.AssetManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * Bird with dynamic rotation/bobbing for flaps and a death fade for the sprite/head.
 *
 * Behavior:
 * - Flap applies an upward velocity and a short flap impulse timer that drives an upward rotation & bob.
 * - While falling, bird rotates downward proportional to downward velocity, clamped to a max angle.
 * - On death, startDeathFade() begins a smooth fade that reduces sprite alpha over deathFadeDuration.
 */
public class Bird {
    private double x = 0;
    private double y = 300;
    private double vy = 0;
    private final double size;
    private final Image sprite;
    private final AssetManager assets;

    // Animation / flap state
    private double stateTime = 0.0;
    private double flapTimer = 0.0;
    private final double flapImpulseDuration = 0.18; // seconds of "flap feeling"

    // Death fade
    private boolean deathFading = false;
    private double deathFadeTime = 0.0;
    private final double deathFadeDuration = 1.2; // seconds

    public Bird(double size, AssetManager assets) {
        this.size = size;
        this.assets = assets;
        this.sprite = (assets != null) ? assets.bird : null;
    }

    public void update(double dt) {
        stateTime += dt;

        // Gravity and vertical motion
        vy += 900 * dt;
        y += vy * dt;

        // flap impulse timer decreases
        if (flapTimer > 0) flapTimer = Math.max(0.0, flapTimer - dt);

        // death fade progression
        if (deathFading) {
            deathFadeTime += dt;
            if (deathFadeTime > deathFadeDuration) deathFadeTime = deathFadeDuration;
        }
    }

    /**
     * Render with rotation around the sprite center and a small bob when flapping/idle.
     */
    public void render(GraphicsContext gc) {
        double cx = x + size / 2.0;
        double cy = y + size / 2.0;

        // compute rotation:
        // - when flapping (flapTimer active), tilt slightly upward (negative angle)
        // - otherwise rotate down proportionally to vy
        double upAngle = -25.0; // degrees when freshly flapped
        double maxDown = 65.0; // degrees when falling hard
        double angle;
        if (flapTimer > 0) {
            double t = flapTimer / flapImpulseDuration;
            angle = upAngle * t;
        } else {
            // map vy (positive downward) to angle
            angle = (vy / 600.0) * maxDown;
            if (angle < -30) angle = -30;
            if (angle > maxDown) angle = maxDown;
        }

        // bob offset while flapping / small idle bob
        double bob = 0.0;
        if (flapTimer > 0) {
            double t = 1.0 - (flapTimer / flapImpulseDuration);
            bob = -6.0 * (1.0 - t); // small upward bob when flapping
        } else {
            bob = Math.sin(stateTime * 6.0) * 2.0; // gentle idle bob
        }

        // death fade alpha
        double alpha = 1.0;
        if (deathFading) alpha = Math.max(0.0, 1.0 - deathFadeTime / deathFadeDuration);

        gc.save();
        gc.translate(cx, cy + bob);
        gc.rotate(angle);
        gc.setGlobalAlpha(alpha);

        if (sprite != null) {
            // draw centered
            gc.drawImage(sprite, -size / 2.0, -size / 2.0, size, size);
        } else {
            gc.setFill(Color.YELLOW);
            gc.fillOval(-size / 2.0, -size / 2.0, size, size);
        }

        gc.setGlobalAlpha(1.0);
        gc.restore();
    }

    public void flap() {
        vy = -320;
        flapTimer = flapImpulseDuration;
    }

    public void reset() {
        y = 300;
        vy = 0;
        deathFading = false;
        deathFadeTime = 0;
        flapTimer = 0;
        stateTime = 0;
    }

    public void setX(double x) { this.x = x; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getSize() { return size; }
    public double getCenterX() { return x + size / 2.0; }
    public double getCenterY() { return y + size / 2.0; }

    public void startDeathFade() {
        deathFading = true;
        deathFadeTime = 0.0;
    }
}
