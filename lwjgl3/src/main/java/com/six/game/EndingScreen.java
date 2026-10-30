package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import game.core.*;
import java.util.List;

public class EndingScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final Ui ui;
    private final GameState state;
    private final EndingDef ending;

    private float timer = 0f;
    private Label mainTextLabel;
    private int currentSubtitleIndex = -1;
    private List<EndingDef.Subtitle> subtitles;
    private float maxDuration;

    public EndingScreen(Ui ui, GameState state, EndingDef ending) {
        this.ui = ui;
        this.state = state;
        this.ending = ending;

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label(ui.texts.get("ending." + ending.id + ".name", ending.id), ui.labelStyle(Color.GOLD))).padBottom(30).row();
        root.add(new Label(ui.texts.get("ending." + ending.id + ".desc", ""), ui.labelStyle(Color.WHITE))).padBottom(50).row();

        mainTextLabel = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        mainTextLabel.setWrap(true);
        root.add(mainTextLabel).width(800).padBottom(50).row();

        TextButton menuBtn = new TextButton("回到主菜单", ui.buttonStyle());
        menuBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                Gdx.files.local("savegame.json").delete();
                Gdx.app.exit();
            }
        });
        root.add(menuBtn).size(200, 50);

        // 加载时间轴数据
        if (ending.review != null) {
            subtitles = ending.review.subtitles;
            maxDuration = ending.review.duration;
        }
    }

    /** 核心：变量替换引擎 */
    private String parseText(String raw) {
        String text = raw;
        // 替换当前场上卡牌数量
        text = text.replace("{casualty}", String.valueOf(state.count("casualty")));
        text = text.replace("{readiness}", String.valueOf(state.count("readiness")));
        text = text.replace("{evacuation}", String.valueOf(state.count("evacuation")));
        text = text.replace("{evidence}", String.valueOf(state.count("evidence")));
        // 替换历史记录条数
        text = text.replace("{record_count}", String.valueOf(state.chronicle.history.size()));
        return text;
    }

    @Override
    public void render(float delta) {
        timer += delta;

        if (subtitles != null) {
            // 找出当前时间应该显示的字幕
            for (int i = currentSubtitleIndex + 1; i < subtitles.size(); i++) {
                if (timer >= subtitles.get(i).time) {
                    currentSubtitleIndex = i;
                    mainTextLabel.setText(parseText(subtitles.get(i).text));
                    break;
                }
            }
        }

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
