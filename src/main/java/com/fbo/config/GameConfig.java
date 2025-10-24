package com.fbo.config;

public final class GameConfig {
    private GameConfig() {}

    public static final int BIRD_SIZE = 40;
    public static final double GRAVITY = 850.0;
    public static final double FLAP_STRENGTH = -380.0;

    public static final double BASE_PIPE_SPEED = 160.0;
    public static final int PIPE_WIDTH = 72;
    public static final int INITIAL_PIPE_GAP = 300;
    public static final double PIPE_SPACING = 380.0;

    public static final int POOL_SIZE = 12;

    public static final int INTERSTITIAL_MS = 6000;
    public static final int INTERSTITIAL_TRIGGER_SCORE = 4;

    public static double getDifficultyFactor(double playTimeSeconds, int score, double userMultiplier) {
        double timeFactor = 1.0 + Math.log1p(playTimeSeconds) / 8.0;
        double scoreFactor = 1.0 + score * 0.02;
        return Math.max(0.6, userMultiplier * timeFactor * scoreFactor);
    }
}
