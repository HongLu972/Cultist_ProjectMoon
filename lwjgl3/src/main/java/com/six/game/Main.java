package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import game.core.ContentLoader;
import game.core.EventBook;
import game.core.GameConfig;
import game.core.GameState;
import game.core.Library;
import game.core.TextBook;
import game.core.VerbDef;

public class Main extends Game {
    // 换成你放进 assets/font/ 的中文字体文件名
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";
    private static final String FALLBACK_LANGUAGE = "zh";

    private Ui ui;

    @Override
    public void create() {
        ContentLoader loader = new ContentLoader();
        GameConfig config = loader.loadConfig(read("data/config.json"));

        String elementsJson = read("data/elements.json");
        String verbsJson = read("data/verbs.json");
        String eventsJson = read("data/events.json");

        // 加载顺序:卡牌 -> 行动 -> 事件(校验动作树引用)-> 校验卡牌引用
        Library library = loader.loadLibrary(elementsJson);
        loader.loadVerbs(verbsJson, library);
        EventBook events = loader.loadEvents(eventsJson, library);
        loader.validate(library, events);

        // 文字:当前语言缺的 key 回退到备用语言(zh),再缺才显示 [key]
        String textsJson = read("data/text_" + config.language + ".json");
        TextBook texts = loader.loadTexts(textsJson);
        String fontScan = elementsJson + verbsJson + eventsJson + textsJson;
        if (!FALLBACK_LANGUAGE.equals(config.language)) {
            String fallbackJson = read("data/text_" + FALLBACK_LANGUAGE + ".json");
            texts = texts.withFallback(loader.loadTexts(fallbackJson));
            fontScan += fallbackJson;
        }

        // 临时的开局:每个行动一个窗口 + 几张牌 + 启动自动事件(时光流逝)
        GameState state = new GameState(library, events);
        for (VerbDef v : library.verbs()) state.addSituation(v.id);
        for (int i = 0; i < 2; i++) state.addCard(library.element("health"));
        for (int i = 0; i < 3; i++) state.addCard(library.element("coin"));
        state.addCard(library.element("reason"));
        state.addCard(library.element("passion"));
        state.addCard(library.element("lantern"));
        state.startAutoEvents();

        ui = new Ui(Gdx.files.internal(FONT_PATH), fontScan, texts);
        setScreen(new GameScreen(ui, state, config.speeds));
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
