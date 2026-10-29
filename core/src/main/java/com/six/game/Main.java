package game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import game.core.ContentLoader;
import game.core.GameState;
import game.core.Library;
import game.core.RecipeBook;

/** 用这个类替换 gdx-liftoff 生成的 Main。 */
public class Main extends Game {
    // 换成你放进 assets/font/ 的中文字体文件名
    private static final String FONT_PATH = "font/NotoSansSC-Regular.ttf";

    private Ui ui;

    @Override
    public void create() {
        String elementsJson = Gdx.files.internal("data/elements.json").readString("UTF-8");
        String recipesJson = Gdx.files.internal("data/recipes.json").readString("UTF-8");

        ContentLoader loader = new ContentLoader();
        Library library = loader.loadLibrary(elementsJson);
        RecipeBook book = loader.loadRecipes(recipesJson, library);

        // 临时的开局:一个"工作"窗口(2 个槽)+ 几张牌
        GameState state = new GameState(library, book);
        state.addSituation("work", 2);
        state.addCard(library.element("health"));
        state.addCard(library.element("health"));
        state.addCard(library.element("reason"));
        state.addCard(library.element("passion"));
        state.addCard(library.element("lantern"));

        ui = new Ui(Gdx.files.internal(FONT_PATH), elementsJson + recipesJson);
        setScreen(new GameScreen(ui, state));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        ui.dispose();
    }
}
