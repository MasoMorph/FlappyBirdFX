package com.fbo;

import com.fbo.graphics.AssetManager;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class UI {
    private UI() {}

    public static final class MenuButton {
        public final String id;
        public final double x, y, w, h;
        public MenuButton(String id, double x, double y, double w, double h) {
            this.id = id; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    public static final List<MenuButton> mainMenuButtons = new ArrayList<>();
    public static String hoveredButtonId = null;
    public static String pressedButtonId = null;

    public static void renderParallaxBackground(javafx.scene.canvas.GraphicsContext gc, double w, double h, AssetManager assets, double totalTime) {
        gc.setFill(Color.SKYBLUE);
        gc.fillRect(0, 0, w, h);

        Image bg2 = assets != null ? assets.backgroundLayer2 : null;
        if (bg2 != null) {
            double imgW = bg2.getWidth();
            double imgH = bg2.getHeight();
            double scale = (imgH > 0) ? (h / imgH) : 1.0;
            double drawW = imgW * scale;
            double drawH = imgH * scale;

            double speed = 18.0;
            double xOffset = (totalTime * speed) % drawW;
            double y = (h - drawH) / 2.0;
            gc.save();
            for (double x = -xOffset; x < w; x += drawW) {
                gc.drawImage(bg2, x, y, drawW, drawH);
            }
            gc.restore();
        }

        Image bg1 = assets != null ? assets.backgroundLayer1 : null;
        if (bg1 != null) {
            double imgW = bg1.getWidth();
            double imgH = bg1.getHeight();
            double scale = (imgH > 0) ? (h / imgH) : 1.0;
            double drawW = imgW * scale;
            double drawH = imgH * scale;

            double speed = 60.0;
            double xOffset = (totalTime * speed) % drawW;
            double y = (h - drawDHEstimate(drawH));
            gc.save();
            for (double x = -xOffset; x < w; x += drawW) {
                gc.drawImage(bg1, x, y, drawW, drawH);
            }
            gc.restore();
        }
    }
    private static double drawDHEstimate(double drawH) {
        return drawH;
    }

    private static Font uiLarge(AssetManager assets) {
        return (assets != null && assets.uiLarge != null) ? assets.uiLarge : Font.font("Arial", 72);
    }
    private static Font uiMedium(AssetManager assets) {
        return (assets != null && assets.uiMedium != null) ? assets.uiMedium : Font.font("Arial", 36);
    }
    private static Font uiSmall(AssetManager assets) {
        return (assets != null && assets.uiSmall != null) ? assets.uiSmall : Font.font("Arial", 20);
    }

    public static void renderHUD(javafx.scene.canvas.GraphicsContext gc, double w, double h, int score, String user, AssetManager assets) {
        gc.setFill(Color.WHITE);
        gc.setFont(uiLarge(assets));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.valueOf(score), w / 2, 80);

        gc.setFont(uiSmall(assets));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("Player: " + user, 20, 30);
    }

    public static void renderGameOver(javafx.scene.canvas.GraphicsContext gc, double w, double h, int score, int highscore, AssetManager assets) {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.WHITE);
        gc.setFont(uiLarge(assets));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("GAME OVER", w / 2, h / 2 - 100);

        gc.setFont(uiMedium(assets));
        gc.fillText("Score: " + score, w / 2, h / 2 - 20);
        gc.fillText("Best: " + highscore, w / 2, h / 2 + 30);

        gc.setFont(uiSmall(assets));
        gc.fillText("Press SPACE to restart", w / 2, h / 2 + 100);
        gc.fillText("Press CTRL for highscores", w / 2, h / 2 + 140);
        gc.fillText("Press ESC to change user", w / 2, h / 2 + 180);
    }

    public static void renderHighscorePage(javafx.scene.canvas.GraphicsContext gc, double w, double h, java.util.HashMap<String, Integer> highscores, String currentUser, AssetManager assets) {
        gc.setFill(Color.rgb(0, 0, 0, 0.8));
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.GOLD);
        gc.setFont(uiLarge(assets));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("HIGHSCORES", w / 2, 100);

        gc.setFill(Color.WHITE);
        gc.setFont(uiMedium(assets));

        int y = 180;
        for (Map.Entry<String, Integer> entry : highscores.entrySet()) {
            String display = entry.getKey() + ": " + entry.getValue();
            gc.setFill(entry.getKey().equals(currentUser) ? Color.YELLOW : Color.WHITE);
            gc.fillText(display, w / 2, y);
            y += 50;
        }

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(uiSmall(assets));
        gc.fillText("Press CTRL to return", w / 2, h - 50);
    }

    public static void renderPauseOverlay(javafx.scene.canvas.GraphicsContext gc, double w, double h, AssetManager assets) {
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.WHITE);
        gc.setFont(uiLarge(assets));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PAUSED", w / 2, h / 2);

        gc.setFont(uiSmall(assets));
        gc.fillText("Press ESC or SPACE to resume", w / 2, h / 2 + 60);
    }

    public static void renderInterstitial(javafx.scene.canvas.GraphicsContext gc, double w, double h, AssetManager assets) {
        gc.setFill(Color.rgb(20, 20, 40));
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.CYAN);
        gc.setFont(uiLarge(assets));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("BONUS ROUND!", w / 2, h / 2 - 50);

        gc.setFont(uiMedium(assets));
        gc.setFill(Color.WHITE);
        gc.fillText("Get ready for increased speed!", w / 2, h / 2 + 20);
    }

    public static void renderDifficultyButtons(javafx.scene.canvas.GraphicsContext gc, double w, double h, AssetManager assets) {
        double buttonY = 50;
        double buttonX = w - 180;
        double buttonW = 150;
        double buttonH = 40;
        double spacing = 50;

        String[] labels = {"Easy", "Normal", "Hard"};
        javafx.scene.paint.Color[] colors = {javafx.scene.paint.Color.web("#76c893"), javafx.scene.paint.Color.web("#FFD66B"), javafx.scene.paint.Color.web("#F07167")};

        for (int i = 0; i < labels.length; i++) {
            double by = buttonY + i * spacing;
            String id = "difficulty-" + i;
            boolean hovered = id.equals(hoveredButtonId);
            boolean pressed = id.equals(pressedButtonId);

            javafx.scene.paint.Color base = colors[i];
            javafx.scene.paint.Color fill = hovered ? base.brighter() : base;
            double inset = pressed ? 4.0 : 0.0;

            gc.setFill(fill);
            gc.fillRoundRect(buttonX + inset, by + inset, buttonW - inset * 2, buttonH - inset * 2, 8, 8);

            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.setFont(Font.font("Arial", 18));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(labels[i], buttonX + buttonW / 2, by + 27);
        }
    }

    public static int difficultyButtonIndexAt(double mx, double my, double w, double h) {
        double buttonY = 50;
        double buttonX = w - 180;
        double buttonW = 150;
        double buttonH = 40;
        double spacing = 50;
        for (int i = 0; i < 3; i++) {
            double by = buttonY + i * spacing;
            if (mx >= buttonX && mx <= buttonX + buttonW && my >= by && my <= by + buttonH)
                return i;
        }
        return -1;
    }

    public static void showDifficultyModal(Stage owner, Consumer<Double> setDifficultyCallback, AssetManager assets) {
        javafx.application.Platform.runLater(() -> {
            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);

            VBox root = new VBox(12);
            root.setPadding(new Insets(18));
            root.setBackground(new Background(new BackgroundFill(Color.web("#1e2330", 0.98), new CornerRadii(10), Insets.EMPTY)));
            root.setEffect(new DropShadow(12, Color.rgb(0,0,0,0.6)));

            javafx.scene.control.Label title = new javafx.scene.control.Label("Select Difficulty");
            title.setTextFill(Color.WHITE);
            title.setFont(uiMedium(assets));

            HBox buttons = new HBox(12);
            buttons.setPadding(new Insets(6));
            buttons.setAlignment(javafx.geometry.Pos.CENTER);

            Button easy = new Button("Easy");
            Button normal = new Button("Normal");
            Button hard = new Button("Hard");

            easy.setStyle("-fx-background-color: #76c893; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 16;");
            normal.setStyle("-fx-background-color: #FFD66B; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 8 16;");
            hard.setStyle("-fx-background-color: #F07167; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16;");

            easy.setOnAction(e -> { setDifficultyCallback.accept(0.7); dialog.close(); });
            normal.setOnAction(e -> { setDifficultyCallback.accept(1.0); dialog.close(); });
            hard.setOnAction(e -> { setDifficultyCallback.accept(1.5); dialog.close(); });

            buttons.getChildren().addAll(easy, normal, hard);

            Button cancel = new Button("Cancel");
            cancel.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
            cancel.setOnAction(e -> dialog.close());

            root.getChildren().addAll(title, buttons, cancel);

            Scene s = new Scene(root);
            s.setFill(Color.TRANSPARENT);
            dialog.setScene(s);

            dialog.setWidth(420);
            dialog.setHeight(180);
            dialog.setResizable(false);
            dialog.centerOnScreen();
            dialog.showAndWait();
        });
    }

    public static void renderMainMenu(javafx.scene.canvas.GraphicsContext gc, double w, double h, AssetManager assets) {
        mainMenuButtons.clear();

        gc.setFill(Color.rgb(10, 12, 20, 0.6));
        gc.fillRect(0, 0, w, h);

        double logoTopMargin = Math.max(24, h * 0.06);
        double logoMaxW = Math.min(260, w * 0.45);
        double logoY = logoTopMargin;

        if (assets != null && assets.logo != null) {
            Image logo = assets.logo;
            double scale = Math.min(logoMaxW / logo.getWidth(), 1.0);
            double lw = logo.getWidth() * scale;
            double lh = logo.getHeight() * scale;
            double lx = (w - lw) / 2.0;
            gc.drawImage(logo, lx, logoY, lw, lh);
            logoY += lh + 18;
        } else {
            gc.setFill(Color.web("#FFE8A8"));
            gc.setFont(uiLarge(assets));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("FLAPPY BIRD FX", w / 2, logoY + 40);
            logoY += 80;
        }

        String[] labels = {"Start Game", "Difficulty", "Change Player", "Quit"};
        double buttonW = Math.min(360, w * 0.55);
        double buttonH = 60;
        double bx = (w - buttonW) / 2.0;
        double byStart = logoY + 6;
        double spacing = 18;

        for (int i = 0; i < labels.length; i++) {
            double by = byStart + i * (buttonH + spacing);
            String id = labels[i].toLowerCase().replace(' ', '-');
            boolean hovered = id.equals(hoveredButtonId);
            boolean pressed = id.equals(pressedButtonId);

            Color bg = switch (i) {
                case 0 -> Color.web("#66CCFF");
                case 1 -> Color.web("#FFD66B");
                case 2 -> Color.web("#76c893");
                default -> Color.web("#F07167");
            };

            Color fill = hovered ? bg.brighter() : bg;
            double inset = pressed ? 4.0 : 0.0;

            gc.setFill(fill);
            gc.fillRoundRect(bx + inset, by + inset, buttonW - inset * 2, buttonH - inset * 2, 14, 14);

            gc.setFill(Color.WHITE);
            gc.setFont(uiMedium(assets));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(labels[i], bx + buttonW / 2.0, by + buttonH / 2.0 + 10);

            mainMenuButtons.add(new MenuButton(id, bx, by, buttonW, buttonH));
        }

        gc.setFill(Color.rgb(255,255,255,0.85));
        gc.setFont(uiSmall(assets));
        gc.fillText("Press M anytime to return to this menu. Use ESC to change user.", w / 2, byStart + labels.length * (buttonH + spacing) + 28);

        renderDifficultyButtons(gc, w, h, assets);
    }

    public static String mainMenuButtonIdAt(double mx, double my) {
        for (MenuButton mb : mainMenuButtons) {
            if (mx >= mb.x && mx <= mb.x + mb.w && my >= mb.y && my <= mb.y + mb.h) {
                return mb.id;
            }
        }
        return null;
    }
}
