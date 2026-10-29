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

/** 开局选择。开局来自 scenarios.json,名称/描述来自文字文件 scenario.<id>.name / .desc。 */
public class StartScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));

    public StartScreen(Ui ui, List<ScenarioDef> scenarios, Consumer<ScenarioDef> onChoose) {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label(ui.texts.get("ui.title"), ui.labelStyle(Color.GOLD))).padBottom(8).row();
        root.add(new Label(ui.texts.get("ui.choose_scenario"), ui.labelStyle(Color.LIGHT_GRAY))).padBottom(28).row();

        for (ScenarioDef sc : scenarios) {
            Table row = new Table();
            row.setBackground(ui.rect(new Color(0.16f, 0.13f, 0.21f, 1f)));
            row.pad(14);

            TextButton button = new TextButton(ui.texts.get("scenario." + sc.id + ".name", sc.id), ui.buttonStyle());
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onChoose.accept(sc);
                }
            });
            row.add(button).width(200).height(52);

            Label desc = new Label(ui.texts.get("scenario." + sc.id + ".desc", ""), ui.labelStyle(Color.LIGHT_GRAY));
            desc.setWrap(true);
            row.add(desc).width(560).padLeft(20);

            root.add(row).padBottom(12).row();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
    }
}
