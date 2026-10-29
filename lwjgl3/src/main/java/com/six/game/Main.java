package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import game.core.ContentLoader;
import game.core.EventBook;
import game.core.GameConfig;
import game.core.GameState;
import game.core.Library;
import game.core.ScenarioDef;
import game.core.TextBook;
import java.util.ArrayList;

public class Main extends Game {
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";
    private static final String FALLBACK_LANGUAGE = "zh";

    private Ui ui;

    @Override
    public void create() {
        ContentLoader loader = new ContentLoader();
        GameConfig config = loader.loadConfig(read("data/config.json"));

        String elementsJson   = read("data/elements.json");
        String verbsJson      = read("data/verbs.json");
        String eventsJson     = read("data/events.json");
        String endingsJson    = read("data/endings.json");
        String scenariosJson  = read("data/scenarios.json");

        // 加载顺序：卡牌 -> 行动 -> 结局 -> 事件（校验动作树引用）-> 校验卡牌引用 -> 开局
        Library library = loader.loadLibrary(elementsJson);
        loader.loadVerbs(verbsJson, library);
        loader.loadEndings(endingsJson, library);
        EventBook events = loader.loadEvents(eventsJson, library);
        loader.validate(library, events);
        ArrayList<ScenarioDef> scenarios = loader.loadScenarios(scenariosJson, library, events);

        // 文字
        String textsJson = read("data/text_" + config.language + ".json");
        TextBook texts = loader.loadTexts(textsJson);
        String fontScan = elementsJson + verbsJson + eventsJson + endingsJson + scenariosJson + textsJson;
        if (!FALLBACK_LANGUAGE.equals(config.language)) {
            String fallbackJson = read("data/text_" + FALLBACK_LANGUAGE + ".json");
            texts = texts.withFallback(loader.loadTexts(fallbackJson));
            fontScan += fallbackJson;
        }

        ui = new Ui(Gdx.files.internal(FONT_PATH), fontScan, texts);

        // 先进 StartScreen 选开局，选定后创建 GameState 并调用 begin(sc)
        setScreen(new StartScreen(ui, scenarios, sc -> {
            GameState state = new GameState(library, events);
            state.worldWidth  = config.worldWidth;
            state.worldHeight = config.worldHeight;
            state.begin(sc);
            setScreen(new GameScreen(ui, state, config.speeds));
        }));
    }

    private static String read(String path) {
        return Gdx.files.internal(path).readString("UTF-8");
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (ui != null) ui.dispose();
    }
}
