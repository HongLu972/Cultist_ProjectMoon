package com.six.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.six.game.Main;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;

public class Lwjgl3Launcher {

    // 👇 核心魔法：手写一个 JNA 接口，调用 Windows 底层 API
    public interface Shell32 extends Library {
        Shell32 INSTANCE = Native.load("shell32", Shell32.class);
        int SetCurrentProcessExplicitAppUserModelID(String AppID);
    }

    public static void main(String[] args) {
        // 👇 强行给进程打上专属身份 ID，让任务栏认准这个 exe！
        try {
            Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID("com.six.game.projectmoon");
            System.out.println("[Launcher] 任务栏身份 ID 绑定成功喵！");
        } catch (Throwable t) {
            System.err.println("[Launcher] 绑定失败: " + t.getMessage());
        }

        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

        String title = "月计模拟器";
        String iconPath = "img/icons/araya.png";

        // 读取配置文件
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

        // 👇 增强版：安全设置窗口左上角图标
        try {
            File pngFile = new File(iconPath);
            if (!pngFile.exists()) pngFile = new File("assets/" + iconPath);
            if (!pngFile.exists()) pngFile = new File("mods/Origin/" + iconPath);

            if (pngFile.exists()) {
                System.out.println("[Launcher] 使用绝对路径加载图标: " + pngFile.getAbsolutePath());
                configuration.setWindowIcon(pngFile.getAbsolutePath());
            } else {
                System.out.println("[Launcher] 找不到图标，尝试相对路径。");
                configuration.setWindowIcon(iconPath);
            }
        } catch (Throwable t) {
            // 终极防弹衣：无论发生什么图片解析错误，绝对不让游戏崩溃！
            System.err.println("[Launcher] 窗口图标加载失败，已跳过。");
        }

        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        configuration.setWindowedMode(1280, 720);

        return configuration;
    }
}
