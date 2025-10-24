package com.fbo.audio;

import com.fbo.graphics.AssetManager;
import com.fbo.util.ResourceUtils;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static final SoundManager INSTANCE = new SoundManager();
    private SoundManager(){}
    public static SoundManager get(){ return INSTANCE; }

    private MediaPlayer musicPlayer;
    private MediaPlayer interstitialVideoPlayer;
    private MediaPlayer interstitialAudioPlayer;

    private final Map<String, Media> sfxMedia = new HashMap<>();
    private final Map<String, String> sfxFiles = Map.of(
            "flap", "/media/flap.wav",
            "score", "/media/coin.wav",
            "death", "/media/death.mp3",
            "coin", "/media/coin.wav"
    );

    public void init(AssetManager assets){
        try {
            Media song = loadMedia("/media/song.mp3");
            if (song != null) {
                musicPlayer = new MediaPlayer(song);
                musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                musicPlayer.setVolume(0.7);
            }
        } catch (Exception ignored) {}

        for (var e : sfxFiles.entrySet()) {
            try {
                Media m = loadMedia(e.getValue());
                if (m != null) sfxMedia.put(e.getKey(), m);
            } catch (Exception ex) {
                System.err.println("Failed to preload SFX " + e.getKey() + ": " + ex.getMessage());
            }
        }
    }

    public void playMusic(){ if (musicPlayer != null) musicPlayer.play(); }
    public void pauseMusic(){ if (musicPlayer != null) musicPlayer.pause(); }
    public void resumeMusic(){ if (musicPlayer != null) musicPlayer.play(); }

    public void playSfx(String name){
        Media media = sfxMedia.get(name);
        if (media == null) return;

        try {
            MediaPlayer p = new MediaPlayer(media);
            p.setVolume(1.0);
            p.setOnEndOfMedia(p::dispose);
            p.setOnError(p::dispose);
            p.play();
        } catch (Exception e) {
            System.err.println("SFX playback error for '" + name + "': " + e.getMessage());
        }
    }

    public void playDeath() { playSfx("death"); }

    public void startInterstitial(Media video, Media audio, Runnable onEnd){
        pauseMusic();

        if (interstitialVideoPlayer != null){ interstitialVideoPlayer.stop(); interstitialVideoPlayer.dispose(); }
        if (interstitialAudioPlayer != null){ interstitialAudioPlayer.stop(); interstitialAudioPlayer.dispose(); }
        if (video != null) interstitialVideoPlayer = new MediaPlayer(video);
        if (audio != null) interstitialAudioPlayer = new MediaPlayer(audio);

        if (interstitialVideoPlayer != null) {
            interstitialVideoPlayer.setOnReady(() -> {
                if (interstitialAudioPlayer != null) interstitialAudioPlayer.play();
                interstitialVideoPlayer.play();
            });

            interstitialVideoPlayer.setOnEndOfMedia(() -> {
                endInterstitial();
                if (onEnd != null) onEnd.run();
            });

            interstitialVideoPlayer.setOnError(() -> {
                System.err.println("Video playback error: " + interstitialVideoPlayer.getError());
                endInterstitial();
                if (onEnd != null) onEnd.run();
            });
        } else {
            if (interstitialAudioPlayer != null) {
                interstitialAudioPlayer.setOnEndOfMedia(() -> {
                    endInterstitial();
                    if (onEnd != null) onEnd.run();
                });
                interstitialAudioPlayer.setOnError(() -> {
                    System.err.println("Audio playback error: " + interstitialAudioPlayer.getError());
                });
                interstitialAudioPlayer.play();
            } else {
                if (onEnd != null) onEnd.run();
                resumeMusic();
            }
        }
    }

    public void endInterstitial(){
        try {
            if (interstitialVideoPlayer != null){ interstitialVideoPlayer.stop(); interstitialVideoPlayer.dispose(); interstitialVideoPlayer = null; }
            if (interstitialAudioPlayer != null){ interstitialAudioPlayer.stop(); interstitialAudioPlayer.dispose(); interstitialAudioPlayer = null; }
        } catch (Exception ignored){}
        resumeMusic();
    }

    public Media loadMedia(String resource) {
        try {
            Path tmp = ResourceUtils.copyResourceToTemp(resource, guessSuffix(resource));
            if (tmp == null) {
                System.err.println("Resource not found: " + resource);
                return null;
            }
            return new Media(tmp.toUri().toString());
        } catch (Exception e){
            System.err.println("Media not loadable: " + resource + " -> " + e.getMessage());
            return null;
        }
    }

    private String guessSuffix(String resource) {
        if (resource == null) return null;
        String lower = resource.toLowerCase();
        if (lower.endsWith(".mp3")) return ".mp3";
        if (lower.endsWith(".wav")) return ".wav";
        if (lower.endsWith(".mp4")) return ".mp4";
        if (lower.endsWith(".ogg")) return ".ogg";
        return null;
    }
}
