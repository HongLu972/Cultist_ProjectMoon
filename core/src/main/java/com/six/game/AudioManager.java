package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import java.util.HashMap;
import java.io.File;

public class AudioManager {
    private static AudioManager instance;
    private Music currentBgm;
    private String currentBgmPath = "";
    private final HashMap<String, Music> bgmCache = new HashMap<>();
    private float volume = 0.5f;

    public static AudioManager getInstance() {
        if (instance == null) instance = new AudioManager();
        return instance;
    }

    // 👇 音乐路径兜底：内部找不到就去外部找！
    private FileHandle findAudio(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (fh.exists()) return fh;

        fh = Gdx.files.local(path);
        if (fh.exists()) return fh;

        fh = Gdx.files.local("assets/" + path);
        if (fh.exists()) return fh;

        fh = Gdx.files.local("mods/Origin/" + path);
        if (fh.exists()) return fh;

        // 终极绝杀：直接在当前运行目录找
        File file = new File(path);
        if (file.exists()) return Gdx.files.absolute(file.getAbsolutePath());

        return Gdx.files.internal(path);
    }

    public void playBgm(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.equals(currentBgmPath) && currentBgm != null && currentBgm.isPlaying()) return;

        for (Music m : bgmCache.values()) {
            if (m != null) { m.stop(); m.setPosition(0f); }
        }

        if (!bgmCache.containsKey(path)) {
            try {
                FileHandle fh = findAudio(path);
                Music music = Gdx.audio.newMusic(fh);
                music.setLooping(true);
                bgmCache.put(path, music);
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "Audio load failed: " + path, e);
                return;
            }
        }

        currentBgm = bgmCache.get(path);
        currentBgmPath = path;
        currentBgm.setVolume(volume);
        currentBgm.play();
    }

    public void setVolume(float v) {
        this.volume = Math.max(0f, Math.min(1f, v));
        if (currentBgm != null) currentBgm.setVolume(volume);
    }

    public float getVolume() { return volume; }

    public void dispose() {
        for (Music m : bgmCache.values()) {
            if (m != null) m.dispose();
        }
        bgmCache.clear();
    }

    public String getCurrentBgmPath() {
        return currentBgmPath;
    }
}
