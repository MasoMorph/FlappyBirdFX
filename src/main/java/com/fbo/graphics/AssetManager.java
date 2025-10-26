package com.fbo.graphics;

import com.fbo.util.ResourceUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.nio.file.Path;

public class AssetManager {
    private static final AssetManager INSTANCE = new AssetManager();
    private AssetManager(){}
    public static AssetManager get(){ return INSTANCE; }

    public Image backgroundLayer1;
    public Image backgroundLayer2;
    public Image bird;
    public Image pipeTexture;
    public Image pipeCapTop;
    public Image pipeCapBottom;
    public Image particle;
    public Image logo;
    public Image interstitialPlaceholder;

    // Simple button images
    public Image buttonNormal;
    public Image buttonPressed;

    public Font uiLarge;
    public Font uiMedium;
    public Font uiSmall;

    public Media interstitialVideo;

    private double difficultyMultiplier = 1.0;

    public void loadAll() {
        backgroundLayer1 = loadImage("/images/bg_layer1.png");
        backgroundLayer2 = loadImage("/images/bg_layer2.png");
        bird = loadImage("/images/steve.png");
        pipeTexture = loadImage("/images/dirt.png");
        pipeCapTop = loadImage("/images/pipe_cap_top.png");
        pipeCapBottom = loadImage("/images/pipe_cap_bottom.png");
        particle = loadImage("/images/xp.png");

        // Load simple button images
        buttonNormal = loadImage("/ui_pack/button1.png");
        buttonPressed = loadImage("/ui_pack/button2.png");

        logo = loadImage("/images/logo.png");
        interstitialPlaceholder = loadImage("/images/logo.png");

        loadFonts();
        loadInterstitialVideo("/videos/interstitial.mp4");
    }

    public ImageView getButton(String state) {
        Image buttonImage = "pressed".equals(state) ? buttonPressed : buttonNormal;
        if (buttonImage == null) {
            return null;
        }
        ImageView iv = new ImageView(buttonImage);
        return iv;
    }

    private void loadFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/MyFont.ttf")) {
            if (is != null) {
                Font f = Font.loadFont(is, 48);
                uiLarge = Font.font(f.getFamily(), 72);
                uiMedium = Font.font(f.getFamily(), 36);
                uiSmall = Font.font(f.getFamily(), 20);
            } else {
                setDefaultFonts();
            }
        } catch (Exception e) {
            setDefaultFonts();
        }
    }

    private void setDefaultFonts() {
        uiLarge = Font.font("Verdana", 72);
        uiMedium = Font.font("Verdana", 36);
        uiSmall = Font.font("Verdana", 20);
    }

    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            Image image = new Image(is);
            System.out.println("Loaded: " + path + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            return image;
        } catch (Exception e) {
            System.err.println("Failed to load image: " + path + " - " + e.getMessage());
            return null;
        }
    }

    private void loadInterstitialVideo(String path) {
        try {
            Path tmp = ResourceUtils.copyResourceToTemp(path, ".mp4");
            if (tmp != null) interstitialVideo = new Media(tmp.toUri().toString());
        } catch (Exception e) {
            System.err.println("Failed to load video: " + e.getMessage());
        }
    }

    public void setDifficultyMultiplier(double m){ difficultyMultiplier = m; }
    public double getDifficultyMultiplier(){ return difficultyMultiplier; }
}
