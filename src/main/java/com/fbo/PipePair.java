package com.fbo;

import com.fbo.config.GameConfig;
import com.fbo.graphics.AssetManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class PipePair {
    public double x;
    private double gapCenterY;
    private int gapSize;
    private final AssetManager assets;
    private double topCapAngle = 0.0;
    private double bottomCapAngle = 0.0;

    public PipePair(double x, double gapCenterY, AssetManager assets) {
        this.x = x;
        this.gapCenterY = gapCenterY;
        this.gapSize = GameConfig.INITIAL_PIPE_GAP;
        this.assets = assets;
    }

    public void reset(double x, double gapCenterY, int gapSize) {
        this.x = x;
        this.gapCenterY = gapCenterY;
        this.gapSize = gapSize;

        double seed = x * 0.13 + gapCenterY * 0.37;
        double frac = Math.abs(Math.sin(seed));
        double tilt = (frac * 28.0) - 14.0;
        topCapAngle = tilt;
        bottomCapAngle = -tilt * 0.6;
    }

    public boolean collidesWith(Bird bird) {
        double birdX = bird.getX();
        double birdY = bird.getY();
        double birdSize = bird.getSize();

        if (birdX + birdSize < x || birdX > x + GameConfig.PIPE_WIDTH) {
            return false;
        }

        double topPipeBottom = gapCenterY - gapSize / 2.0;
        double bottomPipeTop = gapCenterY + gapSize / 2.0;

        return birdY < topPipeBottom || birdY + birdSize > bottomPipeTop;
    }

    public void render(GraphicsContext gc, double screenHeight) {
        double topPipeBottom = gapCenterY - gapSize / 2.0;
        double topPipeHeight = topPipeBottom;
        double bottomPipeTop = gapCenterY + gapSize / 2.0;
        double bottomPipeHeight = screenHeight - bottomPipeTop;

        Image pipeImg = assets.pipeTexture;

        if (pipeImg != null) {

            gc.drawImage(pipeImg, x, 0, GameConfig.PIPE_WIDTH, topPipeHeight);

            gc.drawImage(pipeImg, x, bottomPipeTop, GameConfig.PIPE_WIDTH, bottomPipeHeight);

            Image capTop = assets.pipeCapTop;
            Image capBottom = assets.pipeCapBottom;

            if (capTop != null) {
                double capScale = (GameConfig.PIPE_WIDTH) / capTop.getWidth();
                double capW = capTop.getWidth() * capScale;
                double capH = capTop.getHeight() * capScale;

                double cx = x + GameConfig.PIPE_WIDTH / 2.0; // center x
                double cy = topPipeBottom;

                gc.save();
                gc.translate(cx, cy);
                gc.rotate(topCapAngle);
                gc.scale(1, -1);
                gc.drawImage(capTop, -capW / 2.0, -capH / 2.0 + (capH * 0.25), capW, capH);
                gc.restore();
            }

            if (capBottom != null) {
                double capScale = (GameConfig.PIPE_WIDTH) / capBottom.getWidth();
                double capW = capBottom.getWidth() * capScale;
                double capH = capBottom.getHeight() * capScale;

                double cx = x + GameConfig.PIPE_WIDTH / 2.0;
                double cy = bottomPipeTop;

                gc.save();
                gc.translate(cx, cy);
                gc.rotate(bottomCapAngle);
                gc.drawImage(capBottom, -capW / 2.0, -capH / 2.0 - (capH * 0.25), capW, capH);
                gc.restore();
            }
        } else {
            gc.setFill(Color.GREEN);
            gc.fillRect(x, 0, GameConfig.PIPE_WIDTH, topPipeHeight);
            gc.fillRect(x, bottomPipeTop, GameConfig.PIPE_WIDTH, bottomPipeHeight);

            gc.setFill(Color.DARKGREEN);
            gc.fillRect(x - 5, topPipeHeight - 30, GameConfig.PIPE_WIDTH + 10, 30);
            gc.fillRect(x - 5, bottomPipeTop, GameConfig.PIPE_WIDTH + 10, 30);
        }
    }
}
