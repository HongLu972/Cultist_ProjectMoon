package com.six.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.six.game.Main;

import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        String title = "月计模拟器";
        String iconPath = "img/icons/araya.png";

        // 1. 寻找 config.json
        File configFile = new File("data/config.json");
        if (!configFile.exists()) configFile = new File("mods/Origin/config.json");
        if (!configFile.exists()) configFile = new File("assets/data/config.json");

        if (configFile.exists()) {
            try {
                String json = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), "UTF-8");
                com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(json);
                if (root.has("windowTitle")) title = root.getString("windowTitle");
                if (root.has("windowIcon")) iconPath = root.getString("windowIcon");
            } catch (Exception e) {
                System.err.println("读取配置失败: " + e.getMessage());
            }
        }

        configuration.setTitle(title);

        // 👇 核心魔法：用最原生的 Java File 去满世界找这张图！
        File pngFile = new File(iconPath);
        if (!pngFile.exists()) pngFile = new File("assets/" + iconPath);
        if (!pngFile.exists()) pngFile = new File("mods/Origin/" + iconPath);

        // 如果还找不到，就暴力往上翻目录找 assets
        if (!pngFile.exists()) {
            File current = new File(System.getProperty("user.dir"));
            for (int i = 0; i < 4; i++) {
                File test = new File(current, "assets/" + iconPath);
                if (test.exists()) {
                    pngFile = test;
                    break;
                }
                current = current.getParentFile();
                if (current == null) break;
            }
        }

        // 只有确确实实找到了文件，才敢喂给 LibGDX，并且传绝对路径！
        if (pngFile.exists()) {
            System.out.println("[Launcher] 准备加载图标: " + pngFile.getAbsolutePath());
            try {
                // 注意：这里使用绝对路径，底层 C++ 能直接精准定位！
                configuration.setWindowIcon(pngFile.getAbsolutePath());
            } catch (Throwable t) {
                System.err.println("[Launcher] 警告: 图标损坏或尺寸过大，已放弃设置图标。");
            }
        } else {
            System.out.println("[Launcher] 找不到图标文件，跳过设置。");
            // 千万别调 setWindowIcon，直接跳过，游戏就能正常启动！
        }

        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 720);

        return configuration;
    }
}
