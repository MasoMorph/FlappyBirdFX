package com.fbo.graphics;

import com.fbo.util.ResourceUtils;
import javafx.scene.image.Image;
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
    public Image uiButton;
    public Image logo;
    public Image interstitialPlaceholder;
    public Image particle;

    public Font uiLarge;
    public Font uiMedium;
    public Font uiSmall;

    public Media interstitialVideo;

    private double difficultyMultiplier = 1.0;

    public void loadAll(){
        backgroundLayer1 = loadImage("/images/bg_layer1.png");
        backgroundLayer2 = loadImage("/images/bg_layer2.png");
        bird = loadImage("/images/steve.png");
        pipeTexture = loadImage("/images/dirt.png");
        pipeCapTop = loadImage("/images/pipe_cap_top.png");
        pipeCapBottom = loadImage("/images/pipe_cap_bottom.png");
        uiButton = loadImage("/images/ui_button.png");
        logo = loadImage("/images/logo.png");
        interstitialPlaceholder = loadImage("/images/interstitial_placeholder.png");
        particle = loadImage("/images/particle.png");

        loadFonts();
        loadInterstitialVideo("/videos/interstitial.mp4");
    }

    private void loadFonts(){
        try (InputStream is = getClass().getResourceAsStream("/fonts/MyFont.ttf")) {
            if (is != null) {
                Font f = Font.loadFont(is, 48);
                uiLarge = Font.font(f.getFamily(), 72);
                uiMedium = Font.font(f.getFamily(), 36);
                uiSmall = Font.font(f.getFamily(), 20);
            } else setDefaultFonts();
        } catch (Exception e){
            setDefaultFonts();
        }
    }

    private void setDefaultFonts(){
        uiLarge = Font.font("Verdana", 72);
        uiMedium = Font.font("Verdana", 36);
        uiSmall = Font.font("Verdana", 20);
    }

    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Asset missing: " + path);
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            System.err.println("Asset missing: " + path + " -> " + e.getMessage());
            return null;
        }
    }

    private void loadInterstitialVideo(String path) {
        try {
            Path tmp = ResourceUtils.copyResourceToTemp(path, ".mp4");
            if (tmp == null) {
                System.err.println("Video missing: " + path);
                interstitialVideo = null;
                return;
            }
            interstitialVideo = new Media(tmp.toUri().toString());
            System.out.println("Loaded interstitial video (temp): " + tmp);
        } catch (Exception e) {
            System.err.println("Failed to load video: " + path + " -> " + e.getMessage());
            interstitialVideo = null;
        }
    }

    public void setDifficultyMultiplier(double m){ difficultyMultiplier = m; }
    public double getDifficultyMultiplier(){ return difficultyMultiplier; }
}
