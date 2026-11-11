package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.six.game.core.TextBook;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Ui implements Disposable {
    public final BitmapFont font;
    public final TextBook texts;
    private final Texture white;
    private final TextureRegionDrawable whiteDrawable;
    private final Map<String, Texture> textures = new HashMap<>();

    public Ui(FileHandle ttf, String contentText, TextBook texts) {
        this.texts = texts;

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(ttf);
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 22;
        p.minFilter = Texture.TextureFilter.Linear;
        p.magFilter = Texture.TextureFilter.Linear;

        // 1. 绝对关闭动态生成（这是导致闪退的元凶）
        p.incremental = false;

        // 2. 动态收集字符集：JSON里的文字 + 英文字母标点 + 常用汉字
        Set<Character> unique = new LinkedHashSet<>();

        // 添加基础英文字符
        String defaultChars = FreeTypeFontGenerator.DEFAULT_CHARS;
        for (int i = 0; i < defaultChars.length(); i++) {
            unique.add(defaultChars.charAt(i));
        }

        // 添加 JSON 中扫描出来的剧情文字
        for (int i = 0; i < contentText.length(); i++) {
            unique.add(contentText.charAt(i));
        }

        // 👇 核心修复：强行预加载 GB2312 常用汉字（约 6763 个），这样存档输入名字就不会吞字了
        for (char c = 0x4E00; c <= 0x9FA5; c++) {
            unique.add(c);
        }

        StringBuilder chars = new StringBuilder();
        for (char c : unique) chars.append(c);
        p.characters = chars.toString();

        // 3. 把图集开大到 4096x4096，足以塞下六千多个汉字
        p.packer = new com.badlogic.gdx.graphics.g2d.PixmapPacker(4096, 4096, Pixmap.Format.RGBA8888, 2, false, new com.badlogic.gdx.graphics.g2d.PixmapPacker.GuillotineStrategy());

        font = gen.generateFont(p);
        gen.dispose();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white = new Texture(pm);
        pm.dispose();
        whiteDrawable = new TextureRegionDrawable(new TextureRegion(white));
    }

    public Texture texture(String path) {
        if (path == null || path.isEmpty()) return null;
        if (textures.containsKey(path)) return textures.get(path);

        // 核心：先去 mods 目录里找有没有同路径的图片
        FileHandle fh = findInMods(path);
        Texture t = null;
        if (fh != null && fh.exists()) {
            t = new Texture(fh);
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        textures.put(path, t);
        return t;
    }

    // 新增一个辅助方法：遍历 mods 文件夹找资源
    private FileHandle findInMods(String path) {
        FileHandle modsDir = Gdx.files.local("mods");
        if (modsDir.exists() && modsDir.isDirectory()) {
            for (FileHandle mod : modsDir.list()) {
                if (!mod.isDirectory()) continue;
                // 看看 Mod 文件夹里是不是有这个相对路径的文件
                FileHandle target = mod.child(path);
                if (target.exists()) {
                    return target; // 找到了，返回 Mod 里的文件
                }
            }
        }
        // 如果 Mod 里没找到，返回内置路径
        return Gdx.files.internal(path);
    }

    public TextureRegion whiteRegion() {
        return whiteDrawable.getRegion();
    }

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

    public TextField.TextFieldStyle textFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        // 修复：去掉前面的 ui.
        style.background = rect(new Color(0.1f, 0.15f, 0.3f, 1f));
        style.focusedBackground = rect(new Color(0.2f, 0.25f, 0.45f, 1f));
        style.cursor = rect(Color.WHITE);
        style.selection = rect(new Color(0.5f, 0.4f, 0.6f, 1f));
        return style;
    }

    // 👇 新增：下拉框专属样式
    public SelectBox.SelectBoxStyle selectBoxStyle() {
        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.background = rect(new Color(0.1f, 0.15f, 0.3f, 1f));
        style.scrollStyle = new ScrollPane.ScrollPaneStyle();

        style.listStyle = new List.ListStyle();
        style.listStyle.font = font;
        style.listStyle.fontColorSelected = Color.WHITE;
        style.listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        style.listStyle.selection = rect(new Color(0.4f, 0.3f, 0.5f, 1f));
        style.listStyle.background = rect(new Color(0.2f, 0.16f, 0.26f, 1f));
        return style;
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
