package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.six.game.core.GameState;
import com.six.game.core.SaveData;

public class SaveLoadScreen implements Screen {
    public enum Mode { SAVE, LOAD }

    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final Ui ui;
    private final Mode mode;
    private final GameState currentState;
    private final Runnable onBack;

    private final Table listTable;

    public SaveLoadScreen(Ui ui, Mode mode, GameState currentState, Runnable onBack) {
        this.ui = ui;
        this.mode = mode;
        this.currentState = currentState;
        this.onBack = onBack;

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        String titleText = mode == Mode.SAVE ? "保存游戏" : "读取游戏";
        root.add(new Label(titleText, ui.labelStyle(Color.GOLD))).padBottom(20).row();

        listTable = new Table();
        ScrollPane scroll = new ScrollPane(listTable);
        scroll.setFadeScrollBars(false);
        root.add(scroll).size(850, 400).padBottom(20).row();

        TextButton backBtn = new TextButton("返回", ui.buttonStyle());
        backBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBack.run();
            }
        });
        root.add(backBtn).width(200).height(50);

        refreshList();
    }

    private void refreshList() {
        listTable.clear();
        for (int i = 1; i <= 20; i++) {
            final int slot = i;
            String saveName = getSaveName(slot);
            boolean exists = !saveName.equals("空存档");

            // 如果存档存在，显示名字；不存在，显示空
            String btnText = slot + " : " + saveName;
            TextButton slotBtn = new TextButton(btnText, ui.buttonStyle());

            if (mode == Mode.LOAD && !exists) {
                slotBtn.setDisabled(true);
            }

            slotBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (mode == Mode.SAVE) {
                        showNameDialog(slot);
                    } else {
                        loadFromSlot(slot);
                    }
                }
            });
            listTable.add(slotBtn).width(400).height(50).pad(10);
            if (i % 2 == 0) listTable.row();
        }
    }

    private String getSaveName(int slot) {
        FileHandle file = Gdx.files.local("saves/slot_" + slot + ".json");
        if (!file.exists()) return "空存档";
        try {
            SaveData data = new com.badlogic.gdx.utils.Json().fromJson(SaveData.class, file.readString());
            if (data.saveName != null && !data.saveName.isEmpty()) {
                return data.saveName;
            }
        } catch (Exception e) {
            return "损坏的存档";
        }
        return "未命名存档";
    }

    // 弹出输入名字的窗口
    private void showNameDialog(int slot) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(ui.rect(new Color(0, 0, 0, 0.8f)));
        overlay.setTouchable(Touchable.enabled); // 拦截底层点击

        Table window = new Table();
        window.setBackground(ui.rect(new Color(0.2f, 0.16f, 0.26f, 1f)));
        window.pad(20);

        window.add(new Label("存档命名 (槽位 " + slot + ")", ui.labelStyle(Color.GOLD))).padBottom(20).row();

        TextField input = new TextField("", ui.textFieldStyle());
        input.setMessageText("输入存档备注...");
        window.add(input).width(300).height(40).padBottom(20).row();

        Table btnRow = new Table();
        TextButton confirmBtn = new TextButton("保存", ui.buttonStyle());
        confirmBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = input.getText().trim();
                if (name.isEmpty()) name = "未命名存档";
                saveToSlot(slot, name);
                overlay.remove();
                onBack.run();
            }
        });

        TextButton cancelBtn = new TextButton("取消", ui.buttonStyle());
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
            }
        });

        btnRow.add(confirmBtn).width(120).height(40).padRight(10);
        btnRow.add(cancelBtn).width(120).height(40);
        window.add(btnRow).row();

        overlay.add(window);
        stage.addActor(overlay);
        stage.setKeyboardFocus(input); // 自动把光标聚焦到输入框
    }

    private void saveToSlot(int slot, String name) {
        if (currentState == null) return;
        SaveData data = currentState.createSaveData();
        data.saveName = name;
        String json = new com.badlogic.gdx.utils.Json().toJson(data);
        Gdx.files.local("saves/").mkdirs();
        Gdx.files.local("saves/slot_" + slot + ".json").writeString(json, false);
    }

    private void loadFromSlot(int slot) {
        String json = Gdx.files.local("saves/slot_" + slot + ".json").readString();
        SaveData data = new com.badlogic.gdx.utils.Json().fromJson(SaveData.class, json);
        ((Main) Gdx.app.getApplicationListener()).loadFromData(data);
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
    @Override public void hide() { Gdx.input.setInputProcessor(null); }
    @Override public void dispose() { stage.dispose(); }
}
