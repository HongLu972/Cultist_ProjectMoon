package com.six.game; // 如果你的包名是 com.six.game，请改为 package com.six.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.six.game.core.*;

import java.util.ArrayList;

public class Main extends Game {
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";
    private static final String FALLBACK_LANGUAGE = "zh";

    private Ui ui;
    private Library library;
    private EventBook events;
    private ArrayList<ScenarioDef> scenarios;
    public GameConfig config;

    @Override
    public void create() {
        ContentLoader loader = new ContentLoader();
        config = loader.loadConfig(ModManager.loadWithMods("data/config.json", "config.json"));

        // 应用 config.json 的窗口标题（窗口已创建，反射调用 LWJGL3 的 setTitle，避免 core 依赖桌面后端）
        if (config.windowTitle != null && !config.windowTitle.isEmpty()) {
            try {
                Object graphics = Gdx.graphics;
                Object window = graphics.getClass().getMethod("getWindow").invoke(graphics);
                window.getClass().getMethod("setTitle", String.class).invoke(window, config.windowTitle);
            } catch (Throwable ignored) { /* 非 LWJGL3 后端时忽略 */ }
        }

        String elementsJson   = ModManager.loadWithMods("data/elements.json", "elements.json");
        String verbsJson      = ModManager.loadWithMods("data/verbs.json", "verbs.json");
        String eventsJson     = ModManager.loadWithMods("data/events.json", "events.json");
        String endingsJson    = ModManager.loadWithMods("data/endings.json", "endings.json");
        String scenariosJson  = ModManager.loadWithMods("data/scenarios.json", "scenarios.json");

        library = loader.loadLibrary(elementsJson);
        loader.loadVerbs(verbsJson, library);
        loader.loadEndings(endingsJson, library);
        events = loader.loadEvents(eventsJson, library);
        loader.validate(library, events);
        scenarios = loader.loadScenarios(scenariosJson, library, events);

        // 模组脚本（mods/<模组>/scripts/ 下的 .jar 或 .java）：所有数据加载完成后执行
        ModScriptLoader.runScripts(library, events, config);

        String textsJson = ModManager.loadWithMods("data/text_" + config.language + ".json", "text_" + config.language + ".json");
        TextBook texts = loader.loadTexts(textsJson);
        String fontScan = elementsJson + verbsJson + eventsJson + endingsJson + scenariosJson + textsJson;

        if (!FALLBACK_LANGUAGE.equals(config.language)) {
            String fallbackJson = ModManager.loadWithMods("data/text_" + FALLBACK_LANGUAGE + ".json", "text_" + FALLBACK_LANGUAGE + ".json");
            texts = texts.withFallback(loader.loadTexts(fallbackJson));
            fontScan += fallbackJson;
        }

        FileHandle fontFile = Gdx.files.internal(FONT_PATH);

        // 如果 jar 包里没有，尝试去 mods/Origin 里找
        if (!fontFile.exists()) {
            fontFile = Gdx.files.local("mods/Origin/" + FONT_PATH);
        }
        // 如果还没有，尝试在外部的 font 文件夹找
        if (!fontFile.exists()) {
            fontFile = Gdx.files.local(FONT_PATH);
        }
        // 如果实在没有，暴力向上找 assets
        if (!fontFile.exists()) {
            java.io.File current = new java.io.File(System.getProperty("user.dir"));
            for (int i = 0; i < 4; i++) {
                java.io.File test = new java.io.File(current, "assets/" + FONT_PATH);
                if (test.exists()) {
                    fontFile = Gdx.files.absolute(test.getAbsolutePath());
                    break;
                }
                current = current.getParentFile();
                if (current == null) break;
            }
        }

        if (!fontFile.exists()) {
            System.err.println("【致命错误】找遍了宇宙也找不到字体喵！");
        }

        ui = new Ui(fontFile, fontScan, texts);

        // 播放主菜单音乐
        AudioManager.getInstance().playBgm("audio/bgm_menu.ogg");

        showMainMenu();
    }

    public void loadFromData(SaveData data) {
        GameState state = new GameState(library, events);
        state.worldWidth = config.worldWidth;
        state.worldHeight = config.worldHeight;
        state.loadFromSave(data);
        // 传回调
        setScreen(new GameScreen(ui, state, config.speeds, this::showMainMenu));
    }

    // 确保这个方法存在，而且访问修饰符是 public
    public void showMainMenu() {
        setScreen(new StartScreen(ui, scenarios,
            sc -> startNewGame(sc),
            () -> {
                setScreen(new SaveLoadScreen(ui, SaveLoadScreen.Mode.LOAD, null, () -> showMainMenu()));
            }
        ));
    }

    private void startNewGame(ScenarioDef sc) {
        GameState state = new GameState(library, events);
        state.worldWidth = config.worldWidth;
        state.worldHeight = config.worldHeight;
        state.begin(sc);
        // 注意这里：去掉了 null 和旧参数，直接传回调
        setScreen(new GameScreen(ui, state, config.speeds, this::showMainMenu));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (ui != null) ui.dispose();
        AudioManager.getInstance().dispose(); // 释放音频
    }
}
