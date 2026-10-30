package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import java.util.HashMap;

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

    public void playBgm(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.equals(currentBgmPath) && currentBgm != null && currentBgm.isPlaying()) return;

        if (currentBgm != null) currentBgm.stop();

        if (!bgmCache.containsKey(path)) {
            try {
                Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
                music.setLooping(true);
                bgmCache.put(path, music);
                Gdx.app.log("AudioManager", "成功加载音频: " + path); // 控制台乱码没关系，能加载就行
            } catch (Exception e) {
                // 捕获异常，不让他闪退！直接打印错误并返回
                Gdx.app.error("AudioManager", "音频加载失败(请确认格式为有效的WAV或OGG): " + path, e);
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

    // 游戏彻底关闭时调用
    public void dispose() {
        for (Music m : bgmCache.values()) {
            if (m != null) m.dispose();
        }
        bgmCache.clear();
    }
}
