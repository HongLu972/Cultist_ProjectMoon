package com.six.game.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.six.game.Main;
import java.io.File;

public class Lwjgl3Launcher {
    /** 依次尝试这些位置:exe 同级、以及 gradle run(工作目录是 assets/)时的上一级。 */
    private static final String[] ICON_CANDIDATES = {
        "mods/Origin/img/icons/araya.png",
        "../mods/Origin/img/icons/araya.png",
        "araya.png",
        "../araya.png",
    };
    private static final int[] ICON_SIZES = { 256, 128, 64, 48, 32, 16 };

    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("月计模拟器");
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setWindowedMode(1280, 720);

        // 图标在窗口创建后设置:此时 Gdx.files 和 Pixmap 都可用,可以自己生成多尺寸的方形图标
        configuration.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void created(Lwjgl3Window window) {
                applyWindowIcon(window);
            }
        });
        return configuration;
    }

    private static void applyWindowIcon(Lwjgl3Window window) {
        Pixmap source = null;
        Pixmap[] icons = new Pixmap[ICON_SIZES.length];
        try {
            FileHandle handle = null;
            // 优先用 jar 内置图标（core/src/main/resources/icon.png，打包后随 jar 走，exe 不再依赖外部文件）
            FileHandle cp = Gdx.files.classpath("icon.png");
            if (cp.exists()) {
                handle = cp;
                System.out.println("[Icon] 使用 jar 内置图标");
            } else {
                File file = null;
                for (String candidate : ICON_CANDIDATES) {
                    File f = new File(candidate);
                    if (f.exists()) { file = f; break; }
                }
                if (file == null) {
                    System.out.println("[Icon] 没找到图标文件,工作目录: " + new File(".").getAbsolutePath());
                    return;
                }
                handle = Gdx.files.absolute(file.getAbsolutePath());
                System.out.println("[Icon] 使用外部图标 " + file.getAbsolutePath());
            }
            source = new Pixmap(handle);
            System.out.println("[Icon] 图标尺寸: " + source.getWidth() + "x" + source.getHeight());

            // 居中裁成正方形;竖图想取偏上的脸,可以把 sy 改成 0
            int side = Math.min(source.getWidth(), source.getHeight());
            int sx = (source.getWidth() - side) / 2;
            int sy = (source.getHeight() - side) / 2;

            for (int i = 0; i < ICON_SIZES.length; i++) {
                int s = ICON_SIZES[i];
                Pixmap p = new Pixmap(s, s, Pixmap.Format.RGBA8888);
                p.setBlending(Pixmap.Blending.None);   // 原样拷贝像素和透明度,不混合
                p.setFilter(Pixmap.Filter.BiLinear);
                p.drawPixmap(source, sx, sy, side, side, 0, 0, s, s);
                icons[i] = p;
            }
            window.setIcon(icons);
        } catch (Throwable t) {
            System.out.println("[Icon] 设置图标失败: " + t);   // 图标失败不影响游戏
        } finally {
            if (source != null) source.dispose();
            for (Pixmap p : icons) if (p != null) p.dispose();
        }
    }
}
