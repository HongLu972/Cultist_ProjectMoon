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

    // 👇 核心修复：音频穿透读取魔法
    private FileHandle findAudio(String path) {
        FileHandle fh = Gdx.files.internal(path);
        if (fh.exists()) return fh;

        fh = Gdx.files.local(path);
        if (fh.exists()) return fh;

        fh = Gdx.files.local("assets/" + path);
        if (fh.exists()) return fh;

        // 暴力向上级找 assets（针对 IDEA 运行环境）
        File current = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 4; i++) {
            File test = new File(current, "assets/" + path);
            if (test.exists()) return Gdx.files.absolute(test.getAbsolutePath());
            current = current.getParentFile();
            if (current == null) break;
        }
        return Gdx.files.internal(path); // 实在找不到就抛错吧
    }

    public void playBgm(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.equals(currentBgmPath) && currentBgm != null && currentBgm.isPlaying()) return;

        // 暴力防叠加
        for (Music m : bgmCache.values()) {
            if (m != null) { m.stop(); m.setPosition(0f); }
        }

        if (!bgmCache.containsKey(path)) {
            try {
                // 👇 用魔法方法找文件
                FileHandle fh = findAudio(path);
                Music music = Gdx.audio.newMusic(fh);
                music.setLooping(true);
                bgmCache.put(path, music);
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "Audio load failed: " + path, e);
                return; // 没声音就算了，绝对不让游戏崩溃喵！
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
}
