package com.fbo;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleSystem {
    private final List<Effect> effects = new ArrayList<>();
    private final Random rand = new Random();

    public ParticleSystem() {}

    public void spawnDeathEffect(double cx, double cy, double difficultyMultiplier) {
        effects.add(new DeathEffect(cx, cy, rand, difficultyMultiplier));
    }

    public void update(double dt) {
        Iterator<Effect> it = effects.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.update(dt);
            if (!e.isAlive()) it.remove();
        }
    }

    public void render(GraphicsContext gc) {
        for (Effect e : effects) e.render(gc);
    }

    public void clear() {
        effects.clear();
    }

    private interface Effect {
        void update(double dt);
        void render(GraphicsContext gc);
        boolean isAlive();
    }

    private static final class Debris {
        double x, y;
        double vx, vy;
        double life;
        final double maxLife;
        final Color color;
        Debris(double x, double y, double vx, double vy, double life, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.life = life; this.maxLife = life; this.color = color;
        }
        void update(double dt) {
            vy += 900 * dt; // gravity (px/s^2)
            x += vx * dt;
            y += vy * dt;
            life -= dt;
        }
        void render(GraphicsContext gc) {
            double alpha = Math.max(0, life / maxLife);
            gc.setGlobalAlpha(alpha);
            gc.setFill(color);
            gc.fillOval(x - 3, y - 3, 6, 6);
            gc.setGlobalAlpha(1.0);
        }
        boolean alive() { return life > 0; }
    }

    private static final class DeathEffect implements Effect {
        private final double cx, cy;
        private final Random rand;
        private final double difficulty;
        private double time = 0.0;
        private boolean alive = true;

        private final double glowDuration = 0.45;
        private final double streakDuration = 0.75;
        private final double debrisDuration = 1.6;

        private final List<Debris> debris = new ArrayList<>();
        private final int streakCount = 16;

        DeathEffect(double cx, double cy, Random rand, double difficulty) {
            this.cx = cx; this.cy = cy; this.rand = rand; this.difficulty = difficulty;
            int baseCount = 18;
            int count = baseCount + (int)(difficulty * 6);
            for (int i = 0; i < count; i++) {
                double angle = rand.nextDouble() * Math.PI * 2.0;
                double speed = 120 + rand.nextDouble() * 280;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed - 100;
                double life = 0.7 + rand.nextDouble() * 1.3;
                Color color = Color.hsb(rand.nextDouble() * 60 + 10, 0.9, 0.9);
                debris.add(new Debris(cx, cy, vx, vy, life, color));
            }
        }

        @Override
        public void update(double dt) {
            time += dt;
            for (Debris d : debris) d.update(dt);
            debris.removeIf(d -> !d.alive());
            if (time > Math.max(glowDuration + streakDuration, debrisDuration)) alive = false;
        }

        @Override
        public void render(GraphicsContext gc) {
            // glow phase
            if (time <= glowDuration) {
                double t = Math.min(1.0, time / glowDuration);
                double eased = easeOutCubic(t);
                double radius = 36 + eased * 160 * (1.0 + difficulty * 0.25);
                double alpha = Math.max(0.0, 0.9 * (1.0 - t));
                gc.setGlobalAlpha(alpha * 0.9);
                gc.setFill(Color.color(0.95, 0.7, 0.3, 1.0));
                gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                gc.setGlobalAlpha(1.0);
            }

            // streak phase
            if (time > glowDuration && time <= glowDuration + streakDuration) {
                double local = (time - glowDuration) / streakDuration;
                double alpha = Math.max(0.0, 1.0 - local);
                gc.setGlobalAlpha(alpha);
                gc.setLineWidth(1.5 + (1.0 - local) * 2.0);
                for (int i = 0; i < streakCount; i++) {
                    double a = (2 * Math.PI * i) / streakCount + (rand.nextDouble() - 0.5) * 0.15;
                    double p = easeOutCubic(local);
                    double length = p * (100 + rand.nextDouble() * 80);
                    double ax = cx + Math.cos(a) * 12;
                    double ay = cy + Math.sin(a) * 12;
                    double bx = cx + Math.cos(a) * (length + 12);
                    double by = cy + Math.sin(a) * (length + 12);
                    gc.setStroke(Color.hsb(30 + rand.nextDouble() * 50, 0.9, 1.0));
                    gc.strokeLine(ax, ay, bx, by);
                }
                gc.setGlobalAlpha(1.0);
            }

            // debris
            for (Debris d : debris) d.render(gc);
        }

        @Override
        public boolean isAlive() {
            return alive || !debris.isEmpty();
        }

        private static double easeOutCubic(double t) {
            double p = t - 1;
            return 1 + p * p * p;
        }
    }
}
