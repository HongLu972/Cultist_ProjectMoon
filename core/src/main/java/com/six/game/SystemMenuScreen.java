package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.six.game.core.GameState;

/** 密教式系统菜单：独立全屏界面(风格同 ModEditorScreen)，ESC 或"返回游戏"回到对局。 */
public class SystemMenuScreen implements Screen {
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final Ui ui;
    private final GameState state;
    private final GameScreen game;
    private final Runnable onExitToMenu;
    private Table mainWindow;
    private Table settingsWindow;
    private Label langHint;

    public SystemMenuScreen(Ui ui, GameState state, GameScreen game, Runnable onExitToMenu) {
        this.ui = ui;
        this.state = state;
        this.game = game;
        this.onExitToMenu = onExitToMenu;

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(ui.rect(new Color(0.12f, 0.14f, 0.18f, 1f)));
        stage.addActor(root);

        Table window = new Table();
        window.setBackground(ui.rect(new Color(0.2f, 0.16f, 0.26f, 1f)));
        window.pad(28);

        window.add(new Label(ui.texts.get("ui.menu_title", "系统菜单"), ui.labelStyle(Color.GOLD))).padBottom(24).row();

        window.add(menuBtn(ui.texts.get("ui.menu_resume", "▶  返回游戏"), this::backToGame)).width(300).height(52).padBottom(12).row();
        window.add(menuBtn(ui.texts.get("ui.menu_save", "💾 保存游戏"), this::openSave)).width(300).height(52).padBottom(12).row();
        window.add(menuBtn(ui.texts.get("ui.menu_load", "📂 读取游戏"), this::openLoad)).width(300).height(52).padBottom(12).row();
        window.add(menuBtn(ui.texts.get("ui.menu_mods", "🛠️ 打开 Mod 管理器"), this::openMods)).width(300).height(52).padBottom(12).row();
        window.add(menuBtn(ui.texts.get("ui.settings_title", "⚙ 设置"), this::openSettings)).width(300).height(52).padBottom(12).row();
        window.add(menuBtn(ui.texts.get("ui.menu_exit", "🏠 保存并回到主菜单"), this::exitToMenu)).width(300).height(52).padBottom(20).row();

        // 音量行：实时调节
        Table volRow = new Table();
        Label volLabel = new Label(ui.texts.get("ui.menu_volume", "音量"), ui.labelStyle(Color.WHITE));
        volRow.add(volLabel).width(90).left().padRight(10);
        Slider volSlider = new Slider(0f, 1f, 0.01f, false, ui.sliderStyle());
        volSlider.setValue(AudioManager.getInstance().getVolume());
        volSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                AudioManager.getInstance().setVolume(volSlider.getValue());
            }
        });
        volRow.add(volSlider).width(190).height(30);
        window.add(volRow).row();

        mainWindow = window;
        root.add(mainWindow).row();

        // --- 设置子界面(目前只有语言) ---
        settingsWindow = new Table();
        settingsWindow.setBackground(ui.rect(new Color(0.2f, 0.16f, 0.26f, 1f)));
        settingsWindow.pad(28);
        settingsWindow.add(new Label(ui.texts.get("ui.settings_title", "⚙ 设置"), ui.labelStyle(Color.GOLD))).padBottom(24).row();

        Table langRow = new Table();
        langRow.add(new Label(ui.texts.get("ui.settings_language", "语言"), ui.labelStyle(Color.WHITE))).width(90).left().padRight(10);
        SelectBox<String> langBox = new SelectBox<>(ui.selectBoxStyle());
        langBox.setItems("中文", "English");
        String cur = ((Main) Gdx.app.getApplicationListener()).config.language;
        langBox.setSelected("en".equals(cur) ? "English" : "中文");
        langHint = new Label(ui.texts.get("ui.settings_hint", "重启游戏后生效喵"), ui.labelStyle(Color.SALMON));
        langHint.setVisible(false);
        langBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveLanguage("English".equals(langBox.getSelected()) ? "en" : "zh");
                langHint.setVisible(true);
            }
        });
        langRow.add(langBox).width(190).height(38);
        settingsWindow.add(langRow).padBottom(18).row();
        settingsWindow.add(langHint).padBottom(18).row();
        settingsWindow.add(menuBtn(ui.texts.get("ui.back", "返回"), this::showMainMenuWindow)).width(300).height(52).row();
        settingsWindow.setVisible(false);
        root.add(settingsWindow);

        // ESC 也能返回游戏
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    backToGame();
                    return true;
                }
                return false;
            }
        });
    }

    private TextButton menuBtn(String text, Runnable action) {
        TextButton b = new TextButton(text, ui.buttonStyle());
        b.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return b;
    }

    private void backToGame() {
        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(game);
    }

    private void openSave() {
        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(
            new SaveLoadScreen(ui, SaveLoadScreen.Mode.SAVE, state, this::backToGame));
    }

    private void openLoad() {
        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(
            new SaveLoadScreen(ui, SaveLoadScreen.Mode.LOAD, null, this::backToGame));
    }

    private void openMods() {
        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(new ModEditorScreen(ui));
    }

    private void openSettings() {
        mainWindow.setVisible(false);
        settingsWindow.setVisible(true);
    }

    private void showMainMenuWindow() {
        settingsWindow.setVisible(false);
        mainWindow.setVisible(true);
    }

    /** 语言偏好写回 data/config.json(ModManager 外部优先读取的位置),下次启动生效。 */
    private void saveLanguage(String lang) {
        try {
            Main main = (Main) Gdx.app.getApplicationListener();
            main.config.language = lang;
            com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
            json.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
            Gdx.files.local("data/").mkdirs();
            Gdx.files.local("data/config.json").writeString(json.prettyPrint(main.config), false, "UTF-8");
        } catch (Throwable ignored) {
            // 只读目录等场景写不进就算了,仅本次会话不保存
        }
    }

    private void exitToMenu() {
        onExitToMenu.run();
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
        stage.getViewport().update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
