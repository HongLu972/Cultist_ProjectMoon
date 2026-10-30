package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import game.core.ScenarioDef;
import java.util.List;
import java.util.function.Consumer;

public class StartScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));

    public StartScreen(Ui ui, List<ScenarioDef> scenarios, Consumer<ScenarioDef> onNewGame, Runnable onLoadGame) {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label(ui.texts.get("ui.title"), ui.labelStyle(Color.GOLD))).padBottom(40).row();

        // 读取存档按钮
        if (Gdx.files.local("save_1.json").exists() || Gdx.files.local("save_2.json").exists()) {
            TextButton loadBtn = new TextButton("读取存档", ui.buttonStyle());
            loadBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onLoadGame.run();
                }
            });
            root.add(loadBtn).width(250).height(60).padBottom(20).row();
        }

        root.add(new Label("选择新开局", ui.labelStyle(Color.LIGHT_GRAY))).padBottom(20).row();

        for (ScenarioDef sc : scenarios) {
            TextButton button = new TextButton(ui.texts.get("scenario." + sc.id + ".name", sc.id), ui.buttonStyle());
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onNewGame.accept(sc);
                }
            });
            root.add(button).width(250).height(60).padBottom(10).row();
        }
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
