package com.fbo;

import com.fbo.audio.SoundManager;
import com.fbo.config.GameConfig;
import com.fbo.graphics.AssetManager;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.CacheHint;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class FlappyBirdFX extends Application {
    private final Random rand = new Random();

    private Canvas canvas;
    private GraphicsContext gc;
    private StackPane rootPane;

    private double birdX;
    private Bird player;
    private final Deque<PipePair> pipes = new ArrayDeque<>();
    private final ArrayDeque<PipePair> pipePool = new ArrayDeque<>();

    private ParticleSystem particles;

    private int score = 0;
    private boolean gameOver = false;
    private boolean paused = false;
    private boolean showHighscorePage = false;
    private boolean interstitialActive = false;

    private int shakeFrames = 0;
    private double shakeX = 0, shakeY = 0;

    private MediaView mediaView;
    private MediaPlayer interstitialVideoPlayer;
    private Pane interstitialContainer;

    private Stage primaryStageRef;

    private String currentUser = "Player";
    private final HashMap<String, Integer> highscores = new HashMap<>();

    private AssetManager assets;
    private SoundManager sound;

    private double totalPlayTime = 0;
    private double screenW, screenH;

    private static final int INTERSTITIAL_CHECK_INTERVAL = 5;
    private static final double INTERSTITIAL_PROBABILITY = 0.30;

    private int lastInterstitialScore = -999;

    private boolean showMainMenu = true;
    private boolean pausedBeforeMenu = false;

    private Path highscoresFilePath;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStageRef = primaryStage;

        assets = AssetManager.get();
        sound = SoundManager.get();

        primaryStage.setTitle("Flappy Bird FX — Interstitial Edition");

        rootPane = new StackPane();
        Scene scene = new Scene(rootPane, 700, 900);
        canvas = new Canvas();
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.heightProperty().bind(scene.heightProperty());
        rootPane.getChildren().add(canvas);

        gc = canvas.getGraphicsContext2D();

        highscoresFilePath = Paths.get(System.getProperty("user.home"), ".flappybirdfx", "highscores.properties");
        loadHighscores();

        assets.loadAll();
        sound.init(assets);
        particles = new ParticleSystem();

        initPool();
        initGame();

        showMainMenu = true;
        pausedBeforeMenu = paused;
        paused = true;

        scene.setOnKeyPressed(e -> handleInput(e.getCode()));
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> handleMouseMoved(e.getX(), e.getY()));
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> handleMousePressed(e.getX(), e.getY()));
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> handleMouseReleased(e.getX(), e.getY()));

        canvas.widthProperty().addListener((obs, oldV, newV) -> {
            screenW = newV.doubleValue();
            layoutInterstitialVideoOnResize();
        });
        canvas.heightProperty().addListener((obs, oldV, newV) -> {
            screenH = newV.doubleValue();
            layoutInterstitialVideoOnResize();
        });

        new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long now) {
                if (last == 0) last = now;
                double dt = Math.min(0.033, (now - last) / 1e9);
                last = now;

                screenW = canvas.getWidth();
                screenH = canvas.getHeight();

                update(dt);
                render();
            }
        }.start();

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        saveHighscores();
        try {
            if (interstitialVideoPlayer != null) {
                interstitialVideoPlayer.stop();
                interstitialVideoPlayer.dispose();
            }
        } catch (Exception ignored) {}
        try {
            if (sound != null) {
                sound.endInterstitial();
            }
        } catch (Exception ignored) {}
    }

    private void handleInput(KeyCode code) {
        if (interstitialActive) return;

        if (code == KeyCode.M) {
            toggleMainMenu();
            return;
        }

        if (code == KeyCode.ESCAPE) {
            if (gameOver) changeUser();
            else togglePause();
            return;
        }

        if (paused) {
            if (code == KeyCode.SPACE || code == KeyCode.ESCAPE) togglePause();
            return;
        }

        if (!gameOver) {
            if (code == KeyCode.SPACE) {
                player.flap();
                sound.playSfx("flap");
            }
        } else {
            if (code == KeyCode.SPACE) resetGame();
            else if (code == KeyCode.CONTROL) showHighscorePage = !showHighscorePage;
        }
    }

    private void handleMouseMoved(double mx, double my) {
        if (showMainMenu) {
            String id = UI.mainMenuButtonIdAt(mx, my);
            if (id != null) UI.hoveredButtonId = id;
            else {
                int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
                if (diffIdx >= 0) UI.hoveredButtonId = "difficulty-" + diffIdx;
                else UI.hoveredButtonId = null;
            }
        } else {
            int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
            if (diffIdx >= 0) UI.hoveredButtonId = "difficulty-" + diffIdx;
            else UI.hoveredButtonId = null;
        }
    }

    private void handleMousePressed(double mx, double my) {
        if (showMainMenu) {
            String id = UI.mainMenuButtonIdAt(mx, my);
            if (id != null) UI.pressedButtonId = id;
            else {
                int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
                if (diffIdx >= 0) UI.pressedButtonId = "difficulty-" + diffIdx;
                else UI.pressedButtonId = null;
            }
        } else {
            int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
            if (diffIdx >= 0) UI.pressedButtonId = "difficulty-" + diffIdx;
            else UI.pressedButtonId = null;
        }
    }

    private void handleMouseReleased(double mx, double my) {
        String pressed = UI.pressedButtonId;
        UI.pressedButtonId = null;

        if (pressed == null) {
            if (interstitialActive) return;
            if (paused) { togglePause(); return; }
            if (!gameOver) {
                player.flap();
                sound.playSfx("flap");
            } else {
                resetGame();
            }
            return;
        }

        if (showMainMenu) {
            for (UI.MenuButton mb : UI.mainMenuButtons) {
                if (pressed.equals(mb.id)) {
                    switch (mb.id) {
                        case "start-game":
                            resetGame();
                            showMainMenu = false;
                            paused = false;
                            break;
                        case "difficulty":
                            UI.showDifficultyModal(primaryStageRef, this::setDifficulty, assets);
                            break;
                        case "change-player":
                            changeUser();
                            break;
                        case "quit":
                            Platform.exit();
                            break;
                    }
                    return;
                }
            }
            int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
            if (diffIdx >= 0) {
                UI.showDifficultyModal(primaryStageRef, this::setDifficulty, assets);
                return;
            }
            return;
        }

        int diffIdx = UI.difficultyButtonIndexAt(mx, my, screenW, screenH);
        if (diffIdx >= 0) {
            UI.showDifficultyModal(primaryStageRef, this::setDifficulty, assets);
            return;
        }

        if (paused) { togglePause(); return; }
        if (!gameOver) {
            player.flap();
            sound.playSfx("flap");
        } else {
            resetGame();
        }
    }

    private void initPool() {
        pipePool.clear();
        for (int i = 0; i < GameConfig.POOL_SIZE; i++) pipePool.add(new PipePair(0, 0, assets));
    }

    private void initGame() {
        player = new Bird(GameConfig.BIRD_SIZE, assets);
        birdX = 200;
        pipes.clear();
        score = 0;
        gameOver = false;
        paused = false;
        showHighscorePage = false;
        totalPlayTime = 0;
        shakeFrames = 0;
        synchronized (highscores) {
            highscores.putIfAbsent(currentUser, highscores.getOrDefault(currentUser, 0));
        }
        lastInterstitialScore = -999;

        double startX = 700;
        for (int i = 0; i < 4; i++) {
            PipePair p = obtainPipe();
            p.reset(startX + i * GameConfig.PIPE_SPACING, chooseGapY(screenH), GameConfig.INITIAL_PIPE_GAP);
            pipes.add(p);
        }

        sound.playMusic();
    }

    private PipePair obtainPipe() {
        PipePair p = pipePool.pollFirst();
        if (p == null) p = new PipePair(0, 0, assets);
        return p;
    }

    private void releasePipe(PipePair p) {
        pipePool.offerLast(p);
    }

    private double chooseGapY(double screenHeight) {
        double min = 150 + GameConfig.INITIAL_PIPE_GAP / 2.0;
        double max = (screenHeight <= 0 ? 900 : screenHeight) - 150 - GameConfig.INITIAL_PIPE_GAP / 2.0;
        return min + rand.nextDouble() * Math.max(0, max - min);
    }

    private void update(double dt) {
        if (interstitialActive) return;

        if (gameOver) {
            if (shakeFrames > 0) {
                shakeFrames--;
                shakeX = rand.nextInt(24) - 12;
                shakeY = rand.nextInt(24) - 12;
            } else {
                shakeX = shakeY = 0;
            }
            particles.update(dt);
            player.update(dt);
            return;
        }

        if (paused) {
            particles.update(dt);
            player.update(dt);
            return;
        }

        totalPlayTime += dt;

        player.update(dt);
        player.setX(birdX);

        double difficultyFactor = GameConfig.getDifficultyFactor(totalPlayTime, score, assets.getDifficultyMultiplier());
        double pipeSpeed = GameConfig.BASE_PIPE_SPEED * difficultyFactor;
        int currentGap = Math.max(120, GameConfig.INITIAL_PIPE_GAP - (int) (score * 1.5 + Math.log1p(totalPlayTime) * 6));

        ArrayList<PipePair> recycled = null;
        for (PipePair p : pipes) {
            p.x -= pipeSpeed * dt;
            if (p.collidesWith(player)) triggerDeath();
            if (p.x + GameConfig.PIPE_WIDTH < -20) {
                if (recycled == null) recycled = new ArrayList<>();
                recycled.add(p);
            }
        }

        if (recycled != null) {
            for (PipePair p : recycled) {
                pipes.remove(p);
                double newX = Math.max(screenW, 800) + GameConfig.PIPE_SPACING;
                p.reset(newX, chooseGapY(screenH), currentGap);
                pipes.addLast(p);
                score++;
                sound.playSfx("score");
                maybeTriggerInterstitial();
            }
        }

        if (player.getY() < 0 || player.getY() + GameConfig.BIRD_SIZE > (screenH <= 0 ? 900 : screenH)) {
            triggerDeath();
        }

        particles.update(dt);
    }

    private void maybeTriggerInterstitial() {
        if (interstitialActive) return;

        if (score > 0 && (score % INTERSTITIAL_CHECK_INTERVAL == 0) && score != lastInterstitialScore) {
            lastInterstitialScore = score;
            double roll = rand.nextDouble();
            if (roll <= INTERSTITIAL_PROBABILITY) {
                Media video = assets.interstitialVideo;
                Media audio = sound.loadMedia("/media/gaster.wav");

                sound.startInterstitial(video, audio, this::endInterstitialCallback);

                if (video != null) {
                    showInterstitialVideoLeftHalfAndResizeWindow(video);
                }

                interstitialActive = true;
            }
        }
    }

    private void render() {
        gc.clearRect(0, 0, screenW, screenH);

        if (interstitialActive) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, screenW, screenH);

            if (assets != null && assets.interstitialPlaceholder != null) {
                double iw = assets.interstitialPlaceholder.getWidth();
                double ih = assets.interstitialPlaceholder.getHeight();
                if (iw > 0 && ih > 0) {
                    double scale = Math.min(screenW / iw, screenH / ih);
                    double w = iw * scale;
                    double h = ih * scale;
                    gc.drawImage(assets.interstitialPlaceholder, (screenW - w) / 2.0, (screenH - h) / 2.0, w, h);
                }
            }
            return;
        }

        gc.save();
        gc.translate(shakeX, shakeY);

        if (showMainMenu) {
            UI.renderParallaxBackground(gc, screenW, screenH, assets, totalPlayTime);
            UI.renderMainMenu(gc, screenW, screenH, assets);
            gc.restore();
            return;
        }

        UI.renderParallaxBackground(gc, screenW, screenH, assets, totalPlayTime);
        for (PipePair p : pipes) p.render(gc, screenH);
        particles.render(gc);
        player.render(gc);
        UI.renderHUD(gc, screenW, screenH, score, currentUser, assets);

        if (paused) UI.renderPauseOverlay(gc, screenW, screenH, assets);
        if (gameOver) {
            if (showHighscorePage)
                UI.renderHighscorePage(gc, screenW, screenH, highscores, currentUser, assets);
            else {
                UI.renderGameOver(gc, screenW, screenH, score, highscores.getOrDefault(currentUser, 0), assets);
                UI.renderDifficultyButtons(gc, screenW, screenH, assets);
            }
        }

        gc.restore();
    }

    private void triggerDeath() {
        if (!gameOver) {
            gameOver = true;
            shakeFrames = 24;
            sound.playDeath();
            synchronized (highscores) {
                int prev = highscores.getOrDefault(currentUser, 0);
                if (score > prev) {
                    highscores.put(currentUser, score);
                    saveHighscores();
                }
            }
            particles.spawnDeathEffect(player.getCenterX(), player.getCenterY(), assets.getDifficultyMultiplier());
            player.startDeathFade();
        }
    }

    private void resetGame() {
        for (PipePair p : pipes) releasePipe(p);
        pipes.clear();
        player.reset();
        particles.clear();
        score = 0;
        gameOver = false;
        paused = false;
        showHighscorePage = false;
        totalPlayTime = 0;
        lastInterstitialScore = -999;

        double startX = 700;
        for (int i = 0; i < 4; i++) {
            PipePair p = obtainPipe();
            p.reset(startX + i * GameConfig.PIPE_SPACING, chooseGapY(screenH), GameConfig.INITIAL_PIPE_GAP);
            pipes.add(p);
        }

        sound.playMusic();
    }

    private void changeUser() {
        Platform.runLater(() -> {
            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(primaryStageRef);
            dialog.initModality(Modality.APPLICATION_MODAL);

            VBox container = new VBox(12);
            container.setPadding(new Insets(18));
            container.setAlignment(Pos.CENTER_LEFT);

            BackgroundFill fill = new BackgroundFill(Color.web("#202030", 0.95), new CornerRadii(12), Insets.EMPTY);
            container.setBackground(new Background(fill));
            container.setEffect(new DropShadow(12, Color.rgb(0,0,0,0.6)));

            Label title = new Label("Change Player");
            title.setTextFill(Color.web("#FFDDAA"));
            title.setStyle("-fx-font-weight: bold;");
            title.setWrapText(true);
            title.setMaxWidth(380);
            try { title.setFont(assets.uiMedium); } catch (Exception ignored){ title.setStyle(title.getStyle() + "-fx-font-size:18px;"); }

            Label hint = new Label("Enter a name to save highscore under:");
            hint.setTextFill(Color.web("#E0E0FF"));
            try { hint.setFont(assets.uiSmall); } catch (Exception ignored){ }

            TextField tf = new TextField(currentUser);
            tf.setPromptText("Your name");
            tf.setPrefWidth(380);
            tf.setStyle("-fx-background-radius: 8; -fx-padding: 8 10 8 10; -fx-focus-color: #82CFFD;");

            HBox buttons = new HBox(10);
            buttons.setAlignment(Pos.CENTER_RIGHT);

            Button cancel = new Button("Cancel");
            cancel.setStyle("-fx-background-radius:8; -fx-background-color: #444; -fx-text-fill: white;");
            cancel.setOnAction(e -> dialog.close());

            Button save = new Button("Save");
            save.setStyle("-fx-background-radius:8; -fx-background-color: linear-gradient(#66CCFF, #3388EE); -fx-text-fill: white; -fx-font-weight: bold;");
            save.setOnAction(e -> {
                String name = tf.getText();
                if (name != null && !name.trim().isEmpty()) {
                    currentUser = name.trim();
                    synchronized (highscores) {
                        highscores.putIfAbsent(currentUser, highscores.getOrDefault(currentUser, 0));
                        saveHighscores();
                    }
                }
                dialog.close();
                resetGame();
            });

            buttons.getChildren().addAll(cancel, save);

            if (assets != null && assets.logo != null) {
                Image logo = assets.logo;
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(logo);
                double maxW = 60;
                double scale = Math.min(maxW / logo.getWidth(), 1.0);
                iv.setFitWidth(logo.getWidth() * scale);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setCache(true);
                iv.setCacheHint(CacheHint.SPEED);

                HBox top = new HBox(12);
                top.setAlignment(Pos.CENTER_LEFT);
                top.getChildren().addAll(iv, title);
                container.getChildren().addAll(top, hint, tf, buttons);
            } else {
                container.getChildren().addAll(title, hint, tf, buttons);
            }

            Scene s = new Scene(container);
            s.setFill(Color.TRANSPARENT);
            dialog.setScene(s);

            dialog.setWidth(440);
            dialog.setHeight(220);
            dialog.setResizable(false);
            dialog.centerOnScreen();

            dialog.showAndWait();
        });
    }

    private void togglePause() {
        paused = !paused;
        if (paused) sound.pauseMusic();
        else sound.resumeMusic();
    }

    private void toggleMainMenu() {
        if (interstitialActive) return;

        if (!showMainMenu) {
            pausedBeforeMenu = paused;
            paused = true;
            showMainMenu = true;
        } else {
            showMainMenu = false;
            paused = pausedBeforeMenu;
        }
    }

    private void setDifficulty(double multiplier) {
        assets.setDifficultyMultiplier(multiplier);
    }

    private void showInterstitialVideoLeftHalfAndResizeWindow(Media video) {
        hideInterstitialVideo();

        try {
            interstitialVideoPlayer = new MediaPlayer(video);
            interstitialVideoPlayer.setMute(true);

            mediaView = new MediaView(interstitialVideoPlayer);
            mediaView.setPreserveRatio(true);
            mediaView.setSmooth(true);
            mediaView.setMouseTransparent(true);

            interstitialContainer = new Pane();

            interstitialVideoPlayer.setOnReady(() -> {
                double vw = video.getWidth();
                double vh = video.getHeight();

                if (vw <= 0 || vh <= 0) {
                    vw = Math.max(1280, screenW * 2);
                    vh = Math.max(720, screenH);
                }

                double nativeLeftW = vw / 2.0;
                double nativeLeftH = vh;

                Rectangle2D screenBounds = getScreenBoundsForStage(primaryStageRef);
                double maxW = screenBounds.getWidth() * 0.95;
                double maxH = screenBounds.getHeight() * 0.95;

                double scale = Math.min(maxW / nativeLeftW, maxH / nativeLeftH);
                if (scale <= 0) scale = 1.0;

                double finalWidth = nativeLeftW * scale;
                double finalHeight = nativeLeftH * scale;

                double mediaViewWidth = vw * scale;
                double mediaViewHeight = vh * scale;

                Platform.runLater(() -> {
                    try {
                        primaryStageRef.setWidth(finalWidth);
                        primaryStageRef.setHeight(finalHeight);

                        interstitialContainer.setPrefSize(finalWidth, finalHeight);
                        interstitialContainer.setMinSize(finalWidth, finalHeight);
                        interstitialContainer.setMaxSize(finalWidth, finalHeight);
                        Rectangle clip = new Rectangle(finalWidth, finalHeight);
                        interstitialContainer.setClip(clip);

                        mediaView.setFitWidth(mediaViewWidth);
                        mediaView.setFitHeight(mediaViewHeight);
                        mediaView.setLayoutX(0);
                        mediaView.setLayoutY(0);

                        interstitialContainer.getChildren().clear();
                        interstitialContainer.getChildren().add(mediaView);

                        rootPane.getChildren().add(interstitialContainer);

                        gc.setFill(Color.BLACK);
                        gc.fillRect(0, 0, finalWidth, finalHeight);
                    } catch (Exception e) {
                        System.err.println("Failed to layout interstitial container: " + e.getMessage());
                    }
                });

                interstitialVideoPlayer.play();
            });

            interstitialVideoPlayer.setOnEndOfMedia(() -> {
                Platform.runLater(this::hideInterstitialVideo);
            });

            interstitialVideoPlayer.setOnError(() -> {
                System.err.println("Interstitial video player error: " + interstitialVideoPlayer.getError());
                Platform.runLater(() -> {
                    hideInterstitialVideo();
                    endInterstitialCallback();
                });
            });

        } catch (Exception e) {
            System.err.println("Failed to create interstitial MediaPlayer: " + e.getMessage());
            hideInterstitialVideo();
        }
    }

    private void layoutInterstitialVideoOnResize() {
        if (interstitialContainer == null || mediaView == null || interstitialVideoPlayer == null) return;
        Media media = interstitialVideoPlayer.getMedia();
        if (media == null) return;

        double vw = media.getWidth();
        double vh = media.getHeight();
        if (vw <= 0 || vh <= 0) return;

        double nativeLeftW = vw / 2.0;
        double nativeLeftH = vh;

        Rectangle2D screenBounds = getScreenBoundsForStage(primaryStageRef);
        double maxW = screenBounds.getWidth() * 0.95;
        double maxH = screenBounds.getHeight() * 0.95;

        double scale = Math.min(maxW / nativeLeftW, maxH / nativeLeftH);
        if (scale <= 0) scale = 1.0;

        double finalWidth = nativeLeftW * scale;
        double finalHeight = nativeLeftH * scale;

        double mediaViewWidth = vw * scale;
        double mediaViewHeight = vh * scale;

        Platform.runLater(() -> {
            interstitialContainer.setPrefSize(finalWidth, finalHeight);
            interstitialContainer.setMinSize(finalWidth, finalHeight);
            interstitialContainer.setMaxSize(finalWidth, finalHeight);
            interstitialContainer.setClip(new Rectangle(finalWidth, finalHeight));

            mediaView.setFitWidth(mediaViewWidth);
            mediaView.setFitHeight(mediaViewHeight);
            mediaView.setLayoutX(0);
            mediaView.setLayoutY(0);

            primaryStageRef.setWidth(finalWidth);
            primaryStageRef.setHeight(finalHeight);
        });
    }

    private Rectangle2D getScreenBoundsForStage(Stage stage) {
        try {
            double sx = stage.getX();
            double sy = stage.getY();
            double sw = stage.getWidth();
            double sh = stage.getHeight();
            for (Screen s : Screen.getScreens()) {
                Rectangle2D bounds = s.getVisualBounds();
                if (bounds.contains(sx + sw/2, sy + sh/2)) return bounds;
            }
        } catch (Exception ignored){}
        return Screen.getPrimary().getVisualBounds();
    }

    private void hideInterstitialVideo() {
        if (interstitialVideoPlayer != null) {
            try {
                interstitialVideoPlayer.stop();
                interstitialVideoPlayer.dispose();
            } catch (Exception ignored) {}
            interstitialVideoPlayer = null;
        }
        if (interstitialContainer != null) {
            Platform.runLater(() -> {
                rootPane.getChildren().remove(interstitialContainer);
            });
            interstitialContainer = null;
        }
        mediaView = null;
    }

    private void endInterstitialCallback() {
        Platform.runLater(() -> {
            hideInterstitialVideo();
            interstitialActive = false;
        });
    }

    private void loadHighscores() {
        try {
            if (Files.exists(highscoresFilePath)) {
                Properties p = new Properties();
                try (InputStream is = Files.newInputStream(highscoresFilePath, StandardOpenOption.READ)) {
                    p.load(is);
                }
                synchronized (highscores) {
                    highscores.clear();
                    for (String name : p.stringPropertyNames()) {
                        String val = p.getProperty(name);
                        try {
                            int v = Integer.parseInt(val.trim());
                            highscores.put(name, v);
                        } catch (NumberFormatException ex) {
                            System.err.println("Invalid highscore value for '" + name + "': " + val);
                        }
                    }
                }
            } else {
                Path dir = highscoresFilePath.getParent();
                if (dir != null && !Files.exists(dir)) {
                    try { Files.createDirectories(dir); } catch (IOException ignore) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load highscores: " + e.getMessage());
        }
    }

    private void saveHighscores() {
        try {
            Path dir = highscoresFilePath.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Properties p = new Properties();
            synchronized (highscores) {
                for (Map.Entry<String, Integer> e : highscores.entrySet()) {
                    p.setProperty(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            Path tmp = Files.createTempFile("highscores", ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.WRITE)) {
                p.store(os, "FlappyBirdFX highscores (username=score)");
            }
            Files.move(tmp, highscoresFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("Failed to save highscores: " + e.getMessage());
        }
    }
}
