package game;

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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 字体与基础样式。中文字体只生成"实际用到的字":
 * 数据文件里的所有字 + 下面 UI_CHARS 里的字。
 * 以后在代码里新增任何界面文字,记得把新字加进 UI_CHARS,否则显示为空白或方块。
 */
public class Ui implements Disposable {
    public static final String UI_CHARS = "工作开始进行中暂停加速剩余秒空格键切换时间流速拖动卡牌到槽位";

    public final BitmapFont font;
    private final Texture white;
    private final TextureRegionDrawable whiteDrawable;

    public Ui(FileHandle ttf, String contentText) {
        Set<Character> unique = new LinkedHashSet<>();
        String all = FreeTypeFontGenerator.DEFAULT_CHARS + UI_CHARS + contentText;
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
        s.disabled = rect(new Color(0.18f, 0.16f, 0.2f, 1f));
        return s;
    }

    @Override
    public void dispose() {
        font.dispose();
        white.dispose();
    }
}
