package com.fbo.graphics;

import com.fbo.util.ResourceUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    private Image buttonsSheet;
    private Image iconsSheet;
    private Image hudSheet;
    private Image windowsSheet;

    private final Map<String, Rectangle2D> buttonViewports = new HashMap<>();
    private final Map<String, Rectangle2D> iconViewports = new HashMap<>();
    private final Map<String, Rectangle2D> hudViewports = new HashMap<>();
    private final Map<String, Rectangle2D> windowViewports = new HashMap<>();

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
        particle = loadImage("/images/particle.png");

        buttonsSheet = loadImage("/ui_pack/buttons.png");
        iconsSheet = loadImage("/ui_pack/icons.png");
        hudSheet = loadImage("/ui_pack/hud.png");
        windowsSheet = loadImage("/ui_pack/windows.png");

        setupButtonViewports();
        setupIconViewports();
        setupHudViewports();
        setupWindowViewports();

        loadFonts();
        loadInterstitialVideo("/videos/interstitial.mp4");
    }

    private void setupButtonViewports() {
        buttonViewports.put("resume_normal", new Rectangle2D(0, 0, 200, 50));
        buttonViewports.put("resume_hover", new Rectangle2D(0, 50, 200, 50));
        buttonViewports.put("resume_pressed", new Rectangle2D(0, 100, 200, 50));
        buttonViewports.put("restart_normal", new Rectangle2D(200, 0, 200, 50));
        buttonViewports.put("restart_hover", new Rectangle2D(200, 50, 200, 50));
        buttonViewports.put("restart_pressed", new Rectangle2D(200, 100, 200, 50));
    }

    private void setupIconViewports() {
        iconViewports.put("sword", new Rectangle2D(0, 0, 64, 64));
        iconViewports.put("shield", new Rectangle2D(64, 0, 64, 64));
        iconViewports.put("potion", new Rectangle2D(128, 0, 64, 64));
    }

    private void setupHudViewports() {
        hudViewports.put("health_bar", new Rectangle2D(0, 0, 200, 30));
        hudViewports.put("mana_bar", new Rectangle2D(0, 30, 200, 30));
        hudViewports.put("exp_meter", new Rectangle2D(0, 60, 200, 20));
    }

    private void setupWindowViewports() {
        windowViewports.put("settings_window", new Rectangle2D(0, 0, 400, 300));
        windowViewports.put("shop_window", new Rectangle2D(0, 300, 400, 300));
        windowViewports.put("inventory_window", new Rectangle2D(400, 0, 400, 300));
    }

    public ImageView getButton(String name) { return createView(buttonsSheet, buttonViewports.get(name)); }
    public ImageView getIcon(String name) { return createView(iconsSheet, iconViewports.get(name)); }
    public ImageView getHud(String name) { return createView(hudSheet, hudViewports.get(name)); }
    public ImageView getWindow(String name) { return createView(windowsSheet, windowViewports.get(name)); }

    private ImageView createView(Image sheet, Rectangle2D viewport) {
        if (sheet == null || viewport == null) return null;
        ImageView iv = new ImageView(sheet);
        iv.setViewport(viewport);
        return iv;
    }

    private void loadFonts() {
        try (InputStream is = getClass().getResourceAsStream("/fonts/MyFont.ttf")) {
            if (is != null) {
                Font f = Font.loadFont(is, 48);
                uiLarge = Font.font(f.getFamily(), 72);
                uiMedium = Font.font(f.getFamily(), 36);
                uiSmall = Font.font(f.getFamily(), 20);
            } else setDefaultFonts();
        } catch (Exception e) { setDefaultFonts(); }
    }

    private void setDefaultFonts() {
        uiLarge = Font.font("Verdana", 72);
        uiMedium = Font.font("Verdana", 36);
        uiSmall = Font.font("Verdana", 20);
    }

    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return new Image(is);
        } catch (Exception e) { return null; }
    }

    private void loadInterstitialVideo(String path) {
        try {
            Path tmp = ResourceUtils.copyResourceToTemp(path, ".mp4");
            if (tmp != null) interstitialVideo = new Media(tmp.toUri().toString());
        } catch (Exception ignored) {}
    }

    public void setDifficultyMultiplier(double m){ difficultyMultiplier = m; }
    public double getDifficultyMultiplier(){ return difficultyMultiplier; }
}
