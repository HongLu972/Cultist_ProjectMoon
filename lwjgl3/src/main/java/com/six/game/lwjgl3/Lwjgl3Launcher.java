package com.six.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.six.game.Main;

import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        // 【核心修复】强制将工作目录设为 exe 所在的位置！
        // 这样无论玩家从哪里双击，或者用快捷方式，都能找到外部的 data 文件夹喵！
        try {
            String exePath = System.getProperty("jpackage.app-path");
            if (exePath != null) {
                File exeFile = new File(exePath);
                System.setProperty("user.dir", exeFile.getParent());
            }
        } catch (Exception e) {
            System.err.println("修正工作目录失败喵: " + e.getMessage());
        }

        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        String title = "月计模拟器";
        String iconPath = "libgdx128.png";

        try {
            File configFile = new File("data/config.json");
            if (!configFile.exists()) {
                configFile = new File("assets/data/config.json");
            }
            if (configFile.exists()) {
                // 核心修复：把 java.io.File 转成 LibGDX 的 FileHandle，或者直接读成字符串
                String jsonString = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), "UTF-8");

                com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(jsonString);
                if (root.has("windowTitle")) {
                    title = root.getString("windowTitle");
                }
                if (root.has("windowIcon")) {
                    iconPath = root.getString("windowIcon");
                }
            }
        } catch (Exception e) {
            System.err.println("读取窗口配置失败，使用默认值喵: " + e.getMessage());
        }

        configuration.setTitle(title);

        try {
            // 同样优先找外部图标
            File iconFile = new File(iconPath);
            if (!iconFile.exists()) {
                iconFile = new File("assets/" + iconPath);
            }

            if (iconFile.exists()) {
                configuration.setWindowIcon(iconFile.getPath(), iconFile.getPath(), iconFile.getPath(), iconFile.getPath());
            } else {
                configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
            }
        } catch (Exception e) {
            configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        }

        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 720);

        return configuration;
    }
}
