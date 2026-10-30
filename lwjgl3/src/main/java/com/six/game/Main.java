package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import game.core.*;

import java.util.ArrayList;

public class Main extends Game {
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";
    private static final String FALLBACK_LANGUAGE = "zh";

    private Ui ui;

    @Override
    public void create() {
        ContentLoader loader = new ContentLoader();
        GameConfig config = loader.loadConfig(ModManager.loadWithMods("data/config.json", "config.json"));

        // 用 ModManager 加载所有 JSON
        String elementsJson   = ModManager.loadWithMods("data/elements.json", "elements.json");
        String verbsJson      = ModManager.loadWithMods("data/verbs.json", "verbs.json");
        String eventsJson     = ModManager.loadWithMods("data/events.json", "events.json");
        String endingsJson    = ModManager.loadWithMods("data/endings.json", "endings.json");
        String scenariosJson  = ModManager.loadWithMods("data/scenarios.json", "scenarios.json");

        Library library = loader.loadLibrary(elementsJson);
        loader.loadVerbs(verbsJson, library);
        loader.loadEndings(endingsJson, library);
        EventBook events = loader.loadEvents(eventsJson, library);
        loader.validate(library, events);
        ArrayList<ScenarioDef> scenarios = loader.loadScenarios(scenariosJson, library, events);

        String textsJson = ModManager.loadWithMods("data/text_" + config.language + ".json", "text_" + config.language + ".json");
        TextBook texts = loader.loadTexts(textsJson);
        String fontScan = elementsJson + verbsJson + eventsJson + endingsJson + scenariosJson + textsJson;

        if (!FALLBACK_LANGUAGE.equals(config.language)) {
            String fallbackJson = ModManager.loadWithMods("data/text_" + FALLBACK_LANGUAGE + ".json", "text_" + FALLBACK_LANGUAGE + ".json");
            texts = texts.withFallback(loader.loadTexts(fallbackJson));
            fontScan += fallbackJson;
        }

        ui = new Ui(Gdx.files.internal(FONT_PATH), fontScan, texts);

        // 检查是否有存档
        if (Gdx.files.local("savegame.json").exists()) {
            // 如果有存档，直接建一个状态并把 UI 传进去，让 GameScreen 去处理读档
            GameState state = new GameState(library, events);
            state.worldWidth = config.worldWidth;
            state.worldHeight = config.worldHeight;
            setScreen(new GameScreen(ui, state, config.speeds, true));
        } else {
            setScreen(new StartScreen(ui, scenarios, sc -> {
                GameState state = new GameState(library, events);
                state.worldWidth = config.worldWidth;
                state.worldHeight = config.worldHeight;
                state.begin(sc);
                setScreen(new GameScreen(ui, state, config.speeds, false));
            }));
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (ui != null) ui.dispose();
    }
}
