package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import game.core.TextBook;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 字体、图片缓存与基础样式。
 * 中文字体只生成"实际用到的字":传入的所有数据文件文本里出现过的字。
 * 界面文字都放在 text_xx.json 里并被扫描,所以不需要手工维护字表。
 * 只有一种情况会漏字:你在 Java 代码里直接写了中文字符串——不要那样做,放进 text 文件。
 */
public class Ui implements Disposable {
    public final BitmapFont font;
    public final TextBook texts;
    private final Texture white;
    private final TextureRegionDrawable whiteDrawable;
    private final Map<String, Texture> textures = new HashMap<>();

    public Ui(FileHandle ttf, String contentText, TextBook texts) {
        this.texts = texts;

        Set<Character> unique = new LinkedHashSet<>();
        String all = FreeTypeFontGenerator.DEFAULT_CHARS + contentText;
        for (int i = 0; i < all.length(); i++) unique.add(all.charAt(i));
        StringBuilder chars = new StringBuilder();
        for (char c : unique) chars.append(c);

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(ttf);
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 22;
        p.characters = chars.toString();
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;
        font = gen.generateFont(p);
        gen.dispose();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();
        whiteDrawable = new TextureRegionDrawable(new TextureRegion(white));
    }

    /** 按路径取图片(带缓存)。路径为空或文件不存在时返回 null,调用方按"没有图片"处理。 */
    public Texture texture(String path) {
        if (path == null || path.isEmpty()) return null;
        if (textures.containsKey(path)) return textures.get(path);
        FileHandle fh = Gdx.files.internal(path);
        Texture t = null;
        if (fh.exists()) {
            t = new Texture(fh);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        textures.put(path, t);
        return t;
    }

    /** 1x1 的白色区域,用来在自绘的 Actor 里画纯色矩形(配合 batch.setColor)。 */
    public TextureRegion whiteRegion() {
        return whiteDrawable.getRegion();
    }

    /** 纯色矩形背景。 */
    public Drawable rect(Color c) {
        return whiteDrawable.tint(c);
    }

    public Label.LabelStyle labelStyle(Color c) {
        return new Label.LabelStyle(font, c);
    }

    public TextButton.TextButtonStyle buttonStyle() {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font;
        s.fontColor = Color.WHITE;
        s.disabledFontColor = Color.GRAY;
        s.up = rect(new Color(0.35f, 0.25f, 0.45f, 1f));
        s.down = rect(new Color(0.25f, 0.18f, 0.33f, 1f));
        s.over = rect(new Color(0.42f, 0.30f, 0.54f, 1f));
        s.checked = rect(new Color(0.62f, 0.45f, 0.20f, 1f));
        s.disabled = rect(new Color(0.18f, 0.16f, 0.2f, 1f));
        return s;
    }

    @Override
    public void dispose() {
        font.dispose();
        white.dispose();
        for (Texture t : textures.values()) {
            if (t != null) t.dispose();
        }
    }
}
