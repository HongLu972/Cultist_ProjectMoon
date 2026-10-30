package game; // 如果你的包名是 com.six.game，请改为 package com.six.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import game.core.*;

import java.util.ArrayList;

public class Main extends Game {
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";
    private static final String FALLBACK_LANGUAGE = "zh";

    private Ui ui;
    private Library library;
    private EventBook events;
    private ArrayList<ScenarioDef> scenarios;
    private GameConfig config;

    @Override
    public void create() {
        ContentLoader loader = new ContentLoader();
        config = loader.loadConfig(ModManager.loadWithMods("data/config.json", "config.json"));

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

        String textsJson = ModManager.loadWithMods("data/text_" + config.language + ".json", "text_" + config.language + ".json");
        TextBook texts = loader.loadTexts(textsJson);
        String fontScan = elementsJson + verbsJson + eventsJson + endingsJson + scenariosJson + textsJson;

        if (!FALLBACK_LANGUAGE.equals(config.language)) {
            String fallbackJson = ModManager.loadWithMods("data/text_" + FALLBACK_LANGUAGE + ".json", "text_" + FALLBACK_LANGUAGE + ".json");
            texts = texts.withFallback(loader.loadTexts(fallbackJson));
            fontScan += fallbackJson;
        }

        ui = new Ui(Gdx.files.internal(FONT_PATH), fontScan, texts);

        // 播放主菜单音乐
        AudioManager.getInstance().playBgm("audio/bgm_menu.wav");

        showMainMenu();
    }

    private void showMainMenu() {
        setScreen(new StartScreen(ui, scenarios,
            sc -> startNewGame(sc),
            () -> loadGameSave()
        ));
    }

    private void startNewGame(ScenarioDef sc) {
        GameState state = new GameState(library, events);
        state.worldWidth = config.worldWidth;
        state.worldHeight = config.worldHeight;
        state.begin(sc);

        setScreen(new GameScreen(ui, state, config.speeds, null, this::showMainMenu));
    }

    private void loadGameSave() {
        if (!Gdx.files.local("save_1.json").exists()) return;

        GameState state = new GameState(library, events);
        state.worldWidth = config.worldWidth;
        state.worldHeight = config.worldHeight;

        // 传入 "save_1.json" 表示读取存档，并传入返回主菜单的回调
        setScreen(new GameScreen(ui, state, config.speeds, "save_1.json", this::showMainMenu));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (ui != null) ui.dispose();
        AudioManager.getInstance().dispose(); // 释放音频
    }
}
