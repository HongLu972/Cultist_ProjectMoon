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

        // 1. 如果当前已经在放这首歌，且真的在播放，就别打断它（防止重复播放）
        if (path.equals(currentBgmPath) && currentBgm != null && currentBgm.isPlaying()) {
            return;
        }

        // 2. 暴力防叠加：遍历缓存里的所有音乐，全部强制暂停并重置
        for (Music m : bgmCache.values()) {
            if (m != null) {
                m.stop(); // 停止
                m.setPosition(0f); // 回到开头（防止某些底层没停干净）
            }
        }

        // 3. 加载新歌
        if (!bgmCache.containsKey(path)) {
            try {
                Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
                music.setLooping(true);
                bgmCache.put(path, music);
                Gdx.app.log("AudioManager", "成功加载音频: " + path);
            } catch (Exception e) {
                Gdx.app.error("AudioManager", "音频加载失败: " + path, e);
                return;
            }
        }

        // 4. 播放新歌
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
