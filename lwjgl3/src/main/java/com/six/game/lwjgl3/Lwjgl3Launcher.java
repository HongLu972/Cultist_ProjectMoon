package com.six.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.six.game.Main;

import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        // 强制锁定工作目录，防呆防错喵
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
        File activeConfigFile = null;

        // 1. 寻找外部 data/config.json
        File extDataConfig = new File("data/config.json");
        if (extDataConfig.exists()) {
            activeConfigFile = extDataConfig;
        } else {
            // 2. 【核心修复】扫描 mods 文件夹，找 mods/xxx/config.json
            File modsDir = new File("mods");
            if (modsDir.exists() && modsDir.isDirectory()) {
                File[] modDirs = modsDir.listFiles();
                if (modDirs != null) {
                    for (File mod : modDirs) {
                        if (mod.isDirectory()) {
                            File modConfig = new File(mod, "config.json");
                            if (modConfig.exists()) {
                                activeConfigFile = modConfig;
                                break; // 找到第一个就用
                            }
                        }
                    }
                }
            }
        }

        // 3. 外面都没有，用开发环境的内部数据兜底
        if (activeConfigFile == null) {
            activeConfigFile = new File("assets/data/config.json");
        }

        // 读取并应用配置
        if (activeConfigFile != null && activeConfigFile.exists()) {
            try {
                String jsonString = new String(java.nio.file.Files.readAllBytes(activeConfigFile.toPath()), "UTF-8");
                com.badlogic.gdx.utils.JsonValue root = new com.badlogic.gdx.utils.JsonReader().parse(jsonString);

                if (root.has("windowTitle")) title = root.getString("windowTitle");
                if (root.has("windowIcon")) iconPath = root.getString("windowIcon");

                // 4. 【核心修复】确定图标的真实路径
                File iconFile = null;
                // 优先相对于配置文件所在目录（比如 mods/OfficialMod/icon.png）
                if (activeConfigFile.getParentFile() != null) {
                    iconFile = new File(activeConfigFile.getParentFile(), iconPath);
                }
                // 如果没找到，试试相对于游戏根目录
                if (iconFile == null || !iconFile.exists()) {
                    iconFile = new File(iconPath);
                }
                // 如果还没有，试试 assets 目录（开发环境）
                if (!iconFile.exists()) {
                    iconFile = new File("assets/" + iconPath);
                }

                if (iconFile != null && iconFile.exists()) {
                    System.out.println("[Launcher] 成功找到图标了喵: " + iconFile.getAbsolutePath());
                    configuration.setWindowIcon(iconFile.getPath(), iconFile.getPath(), iconFile.getPath(), iconFile.getPath());
                } else {
                    System.out.println("[Launcher] 警告: 找不到图标文件 " + iconPath + "，使用默认图标。");
                    configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
                }

            } catch (Exception e) {
                System.err.println("[Launcher] 读取窗口配置失败，使用默认值喵: " + e.getMessage());
                configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
            }
        } else {
            configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        }

        configuration.setTitle(title);
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 720);

        return configuration;
    }
}
