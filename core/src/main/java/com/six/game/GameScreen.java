package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.six.game.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameScreen implements Screen {
    private static final float STAGE_W = 1280, STAGE_H = 720;
    private static final String KIND_CARD = "card";
    private static final String KIND_SLOT = "slot";
    private static final String KIND_WINDOW = "window";

    private final Ui ui;
    private final GameState state;
    private final Clock clock = new Clock();

    private InputMultiplexer multiplexer;

    private final Stage worldStage = new Stage(new FitViewport(STAGE_W, STAGE_H));
    private final Stage uiStage = new Stage(new FitViewport(STAGE_W, STAGE_H));

    private final Group worldGroup = new Group();
    private final DragAndDrop dnd = new DragAndDrop();

    private final InfoPanel infoPanel;
    private final LogPanel logPanel;
    private final Table speedBar = new Table();
    private final Label pausedLabel;
    private final Vector2 tmp = new Vector2();

    private String selKind;
    private String selId;
    private float infoX, infoY;
    private String lastInfoTitle, lastInfoBody;

    private boolean dirty = true;
    private int lastSignature;
    private float grabX = CardActor.W / 2f, grabY = CardActor.H / 2f;

    private final Runnable onExitToMenu;
    private Table pauseMenu;
    private Label volumeLabel;

    public GameScreen(Ui ui, GameState state, float[] speeds, Runnable onExitToMenu) {
        this.ui = ui;
        this.state = state;
        this.onExitToMenu = onExitToMenu;

        this.infoPanel = new InfoPanel(ui);
        this.logPanel = new LogPanel(ui);
        this.pausedLabel = new Label(ui.texts.get("ui.paused", "已暂停"), ui.labelStyle(Color.SALMON));
        this.pausedLabel.setVisible(false);

        buildSpeedBar(speeds);
        dnd.setKeepWithinStage(false);

        worldGroup.setSize(state.worldWidth, state.worldHeight);
        worldGroup.addActor(new WorldBackground(ui, state.worldWidth, state.worldHeight));
        worldGroup.setPosition((STAGE_W - state.worldWidth) / 2f, (STAGE_H - state.worldHeight) / 2f);
        worldStage.addActor(worldGroup);

        Table uiRoot = new Table();
        uiRoot.setFillParent(true);
        uiRoot.add(speedBar).expandX().right().pad(8).row();
        uiRoot.add().expand().row();
        uiRoot.add(logPanel).width(LogPanel.W).right().bottom().pad(8);
        uiStage.addActor(uiRoot);
        uiStage.addActor(infoPanel);
        infoPanel.setVisible(false);

        setupPauseMenu();
        setupWorldInput();

        // 修复 1：声明并赋值
        multiplexer = new InputMultiplexer(uiStage, worldStage);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setInputProcessor(multiplexer); // 修复 2：加上这句，不然鼠标点不动

        state.addListener((situation, event) -> {
            String key = "event." + event.id + ".result";
            if (ui.texts.has(key)) logPanel.add(ui.texts.get(key), ui);
            state.chronicle.addRecord(state.totalTime, "结算事件: " + ui.texts.get("event." + event.id + ".name", event.id));
        });

        // 全局按键监听
        uiStage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.SPACE) {
                    clock.paused = !clock.paused;
                    pausedLabel.setVisible(clock.paused);
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    togglePauseMenu();
                    return true;
                }
                return false;
            }
        });

        // 1. 伪造一个左上角的窗口图标（比如一个齿轮或者你喜欢的图片）
        Image modIcon = new Image(ui.rect(new Color(0.4f, 0.3f, 0.5f, 1f))); // 如果没图片，就用纯色块
        modIcon.setSize(32, 32);

        // 2. 监听右键点击
        modIcon.addListener(new ClickListener(Input.Buttons.RIGHT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showModSystemMenu(modIcon);
            }
        });

        // --- 左上角系统菜单按钮 ---
        Table topLeftTable = new Table();
        topLeftTable.setFillParent(true);
        topLeftTable.top().left().pad(8);

        // 直接用个明晃晃的按钮，不怕找不到喵！
        TextButton modMenuBtn = new TextButton("☰ 系统菜单", ui.buttonStyle());
        modMenuBtn.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showModSystemMenu(modMenuBtn);
            }
        });

        topLeftTable.add(modMenuBtn).size(110, 34);
        uiStage.addActor(topLeftTable);
        if (state.situations.isEmpty()) {
            spawnInitialEvents();
        }
        AudioManager.getInstance().playBgm("audio/bgm_game.ogg");
    }

    private void spawnInitialEvents() {
        // 获取当前摄像机中心点（世界坐标）
        float centerX = worldStage.getCamera().position.x;
        float centerY = worldStage.getCamera().position.y;

        EventDef genEvent = state.events.get("delta");
        if (genEvent != null) {
            // 绝望发生器生成在中心偏右上
            Situation s = state.openEventWindow(genEvent, null, centerX + 300, centerY + 100);
            s.start();
        }

        EventDef timeEvent = state.events.get("time_A");
        if (timeEvent != null) {
            // 时间流逝生成在中心偏右下
            Situation s = state.openEventWindow(timeEvent, null, centerX + 300, centerY - 200);
            s.start();
        }

        state.chronicle.addRecord(0, "已至。");
    }

    private void togglePauseMenu() {
        boolean nowVisible = !pauseMenu.isVisible();
        pauseMenu.setVisible(nowVisible);
        if (nowVisible) {
            pauseMenu.toFront();
            clock.paused = true;
            pausedLabel.setVisible(true);
        } else {
            clock.paused = false;
            pausedLabel.setVisible(false);
        }
    }

    private void setupPauseMenu() {
        pauseMenu = new Table();
        pauseMenu.setFillParent(true);
        pauseMenu.setBackground(ui.rect(new Color(0f, 0f, 0f, 0.85f)));
        pauseMenu.setVisible(false);
        pauseMenu.setTouchable(Touchable.enabled);

        Table window = new Table();
        window.setBackground(ui.rect(new Color(0.2f, 0.16f, 0.26f, 1f)));
        window.pad(20);

        window.add(new Label("暂停 / 设置", ui.labelStyle(Color.GOLD))).padBottom(20).row();

        TextButton saveBtn = new TextButton("保存游戏", ui.buttonStyle());
        saveBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(
                    new SaveLoadScreen(ui, SaveLoadScreen.Mode.SAVE, state, () -> {
                        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(GameScreen.this);
                    })
                );
            }
        });
        window.add(saveBtn).width(250).height(50).padBottom(10).row();

        TextButton loadBtn = new TextButton("读取游戏", ui.buttonStyle());
        loadBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(
                    new SaveLoadScreen(ui, SaveLoadScreen.Mode.LOAD, null, () -> {
                        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(GameScreen.this);
                    })
                );
            }
        });
        window.add(loadBtn).width(250).height(50).padBottom(10).row();

        Table volRow = new Table();
        volumeLabel = new Label("音量: " + (int)(AudioManager.getInstance().getVolume()*100) + "%", ui.labelStyle(Color.WHITE));
        volRow.add(volumeLabel).width(150);
        TextButton volDown = new TextButton("-", ui.buttonStyle());
        TextButton volUp = new TextButton("+", ui.buttonStyle());

        volDown.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                AudioManager.getInstance().setVolume(AudioManager.getInstance().getVolume() - 0.1f);
                volumeLabel.setText("音量: " + (int)(AudioManager.getInstance().getVolume()*100) + "%");
            }
        });
        volUp.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                AudioManager.getInstance().setVolume(AudioManager.getInstance().getVolume() + 0.1f);
                volumeLabel.setText("音量: " + (int)(AudioManager.getInstance().getVolume()*100) + "%");
            }
        });
        volRow.add(volDown).size(40).padLeft(10);
        volRow.add(volUp).size(40).padLeft(5);
        window.add(volRow).padBottom(20).row();

        TextButton menuBtn = new TextButton("保存并回到主菜单", ui.buttonStyle());
        menuBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent e, Actor a) {
                onExitToMenu.run();
            }
        });
        window.add(menuBtn).width(250).height(50).row();

        pauseMenu.add(window);
        uiStage.addActor(pauseMenu);
    }

    private void showModSystemMenu(Actor anchor) {
        // 防止重复点击生成多个菜单
        for (Actor child : uiStage.getActors()) {
            if (child instanceof Table && "mod_menu".equals(child.getName())) {
                child.remove();
                return; // 如果菜单已经存在，点击就关闭它
            }
        }

        Table menuTable = new Table();
        menuTable.setName("mod_menu");
        menuTable.setBackground(ui.rect(new Color(0.2f, 0.16f, 0.26f, 1f)));
        menuTable.pad(10);

        TextButton btnEditor = new TextButton("🛠️ 打开 Mod 管理器", ui.buttonStyle());
        btnEditor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(new ModEditorScreen(ui));
                if (menuTable.getParent() != null) menuTable.remove();
            }
        });
        menuTable.add(btnEditor).width(200).height(40).row();

        // 计算按钮在屏幕上的坐标，让菜单贴边弹出
        Vector2 pos = anchor.localToStageCoordinates(new Vector2(0, 0));
        // 放在按钮正下方
        menuTable.setPosition(pos.x, pos.y - menuTable.getPrefHeight() - 10);
        uiStage.addActor(menuTable);
    }

    private void buildSpeedBar(float[] speeds) {
        speedBar.add(pausedLabel).padRight(14);
        ButtonGroup<TextButton> group = new ButtonGroup<>();
        for (float s : speeds) {
            final float speed = s;
            TextButton b = new TextButton(speedLabel(speed), ui.buttonStyle());
            group.add(b);
            b.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (b.isChecked()) clock.speed = speed;
                }
            });
            speedBar.add(b).size(70, 34).padLeft(4);
            if (speed == 1f) b.setChecked(true);
        }
    }

    private static String speedLabel(float s) {
        return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "x";
    }

    private void setupWorldInput() {
        worldStage.getRoot().addListener(new InputListener() {
            private float lastStageX, lastStageY;
            private boolean draggingView;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.LEFT && (event.getTarget() == worldStage.getRoot() || event.getTarget() instanceof WorldBackground)) {
                    hideInfo();
                    return true;
                }
                if (button == Input.Buttons.RIGHT) {
                    if (event.getTarget() instanceof CardActor || event.getTarget() instanceof SlotActor) return false;
                    lastStageX = event.getStageX();
                    lastStageY = event.getStageY();
                    draggingView = true;
                    return true;
                }
                return false;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (draggingView) {
                    float dx = event.getStageX() - lastStageX;
                    float dy = event.getStageY() - lastStageY;
                    worldGroup.moveBy(dx, dy);
                    lastStageX = event.getStageX();
                    lastStageY = event.getStageY();
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                draggingView = false;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                OrthographicCamera cam = (OrthographicCamera) worldStage.getCamera();
                cam.zoom = Math.max(0.5f, Math.min(2.0f, cam.zoom + amountY * 0.1f));
                cam.update();
                return true;
            }
        });
    }

    private void rebuild() {
        dnd.clear();

        for (int i = worldGroup.getChildren().size - 1; i >= 0; i--) {
            Actor a = worldGroup.getChildren().get(i);
            if (!(a instanceof WorldBackground)) a.remove();
        }

        // 👇 核心修复：收集所有在槽里的卡牌实例
        java.util.Set<ElementInstance> slotCards = new java.util.HashSet<>();
        for (Situation s : state.situations) {
            for (Slot slot : s.slots) {
                if (slot.content != null) slotCards.add(slot.content);
            }
        }

        List<WindowView> views = new ArrayList<>();
        for (Situation s : state.situations) {
            WindowView v = new WindowView(ui, s);
            v.setPosition(s.x, s.y);
            addWindowClick(v);
            addWindowDrag(v);
            worldGroup.addActor(v);
            views.add(v);
        }

        for (ElementInstance c : state.cards) {
            if (slotCards.contains(c)) continue; // 👈 如果卡牌在这个集合里，就不画在桌面上
            CardActor a = new CardActor(ui, c);
            if (!Float.isNaN(c.x)) a.setPosition(c.x, c.y);
            addSource(a);
            addSelectOnClick(a);
            addCardRightClick(a);
            worldGroup.addActor(a);
        }

        for (WindowView v : views) {
            for (SlotActor sa : v.slotActors) {
                addSlotTarget(sa);
                addSelectOnClick(sa);
                if (sa.cardActor != null) {
                    addSource(sa.cardActor);
                    addSelectOnClick(sa.cardActor);
                    addCardRightClick(sa.cardActor);
                }
            }
        }
    }

    private void addWindowDrag(WindowView view) {
        view.header.addListener(new DragListener() {
            private float startX, startY, actorX, actorY;
            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                startX = event.getStageX();
                startY = event.getStageY();
                actorX = view.getX();
                actorY = view.getY();
            }
            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                float dx = event.getStageX() - startX;
                float dy = event.getStageY() - startY;
                view.setPosition(actorX + dx, actorY + dy);
                view.situation.x = view.getX();
                view.situation.y = view.getY();
            }
        });
    }

    private void addSource(CardActor actor) {
        dnd.addSource(new DragAndDrop.Source(actor) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (actor.card.locked) return null;
                grabX = x;
                grabY = y;
                hideInfo();
                DragAndDrop.Payload p = new DragAndDrop.Payload();
                p.setObject(actor.card);
                p.setDragActor(new CardActor(ui, actor.card));
                actor.setVisible(false);
                return p;
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                actor.setVisible(true);
                ElementInstance card = (ElementInstance) payload.getObject();

                // 如果没有丢进槽位，就根据鼠标位置更新卡牌的世界坐标
                if (target == null) {
                    detach(card);
                    Vector2 stagePos = new Vector2(event.getStageX(), event.getStageY());
                    worldGroup.stageToLocalCoordinates(stagePos);
                    card.x = stagePos.x - grabX;
                    card.y = stagePos.y - grabY;
                    dirty = true;
                }
            }
        });
    }

    private void addSlotTarget(SlotActor sa) {
        dnd.addTarget(new DragAndDrop.Target(sa) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                ElementInstance card = (ElementInstance) payload.getObject();
                if (sa.situation.slots.get(0).content == card) return false;
                return sa.situation.canPlace(sa.index, card);
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                ElementInstance card = (ElementInstance) payload.getObject();
                detach(card);
                sa.situation.placeCard(sa.index, card);
                dirty = true;
            }
        });
    }

    private void addCardRightClick(CardActor actor) {
        actor.addListener(new ClickListener(Input.Buttons.RIGHT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (state.isInAnySlot(actor.card)) {
                    detach(actor.card);
                    Vector2 stagePos = new Vector2(event.getStageX(), event.getStageY());
                    worldGroup.stageToLocalCoordinates(stagePos);
                    actor.card.x = stagePos.x;
                    actor.card.y = stagePos.y;
                    dirty = true;
                }
                event.stop();
            }
        });
    }

    private void detach(ElementInstance card) {
        for (Situation s : state.situations) {
            for (int i = 0; i < s.slots.size(); i++) {
                if (s.slots.get(i).content == card) {
                    s.takeCard(i);
                    return;
                }
            }
        }
    }

    private void addSelectOnClick(CardActor actor) {
        actor.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(KIND_CARD, actor.card.instanceId, event);
            }
        });
    }

    private void addSelectOnClick(SlotActor actor) {
        actor.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() != actor) return;
                select(KIND_SLOT, actor.situation.slots.get(actor.index).id, event);
            }
        });
    }

    private void addWindowClick(WindowView view) {
        view.header.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(KIND_WINDOW, view.situation.situationId, event);
            }
        });
    }

    private void select(String kind, String id, InputEvent event) {
        selKind = kind;
        selId = id;
        infoX = event.getStageX();
        infoY = event.getStageY();
        lastInfoTitle = null;
    }

    private void hideInfo() {
        selKind = null;
        selId = null;
        infoPanel.setVisible(false);
    }

    private void refreshInfo() {
        if (selKind == null) return;
        String[] info;
        switch (selKind) {
            case KIND_CARD: info = cardInfo(selId); break;
            case KIND_SLOT: info = slotInfo(selId); break;
            default: info = windowInfo(selId); break;
        }
        if (info == null) { hideInfo(); return; }
        if (!info[0].equals(lastInfoTitle) || !info[1].equals(lastInfoBody)) {
            infoPanel.show(info[0], info[1]);
            infoPanel.place(infoX, infoY, STAGE_W, STAGE_H);
            lastInfoTitle = info[0];
            lastInfoBody = info[1];
        }
        infoPanel.setVisible(true);
    }

    private String[] cardInfo(String instanceId) {
        ElementInstance c = findCard(instanceId);
        if (c == null) return null;
        String title = ui.texts.get("element." + c.def.id + ".name", c.def.id);
        StringBuilder body = new StringBuilder(ui.texts.get("element." + c.def.id + ".desc", ""));

        // 👈 核心修复：用 visibleAspects()，隐藏属性就不会显示在面板上了喵！
        Map<String, Integer> currentAspects = c.visibleAspects();

        if (!currentAspects.isEmpty()) {
            body.append("\n\n").append(ui.texts.get("ui.aspects", "词条")).append(": ").append(aspectList(currentAspects));
        }
        return new String[] { title, body.toString() };
    }

    private String[] slotInfo(String slotId) {
        Slot s = findSlot(slotId);
        if (s == null) return null;
        String title = ui.texts.get("slot." + s.defId + ".name", s.defId);
        StringBuilder body = new StringBuilder(ui.texts.get("slot." + s.defId + ".desc", ""));

        body.append("\n\n");
        if (state.library.hasVerb(s.defId)) {
            body.append(ui.texts.get("ui.seed_hint", "放入卡牌开始"));
        } else if (s.accepts.isEmpty()) {
            body.append(ui.texts.get("ui.accepts_any", "接受任何卡牌"));
        } else {
            body.append(ui.texts.get("ui.accepts", "接受")).append(": ").append(aspectList(s.accepts));
        }
        return new String[] { title, body.toString() };
    }

    private String[] windowInfo(String situationId) {
        Situation s = findSituation(situationId);
        if (s == null) return null;
        EventDef e = s.running != null ? s.running : s.selected;
        if (e == null) return new String[] { ui.texts.get("slot." + s.verbId + ".name", s.verbId), "" };
        return new String[] { ui.texts.get("event." + e.id + ".name", e.id), ui.texts.get("event." + e.id + ".desc", "") };
    }

    private String aspectList(Map<String, Integer> aspects) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : new TreeMap<>(aspects).entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ui.texts.get("aspect." + e.getKey() + ".name", e.getKey())).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    private ElementInstance findCard(String instanceId) {
        for (ElementInstance c : state.cards) if (c.instanceId.equals(instanceId)) return c;
        return null;
    }

    private Slot findSlot(String slotId) {
        for (Situation s : state.situations)
            for (Slot slot : s.slots)
                if (slot.id.equals(slotId)) return slot;
        return null;
    }

    private Situation findSituation(String situationId) {
        for (Situation s : state.situations) if (s.situationId.equals(situationId)) return s;
        return null;
    }

    @Override
    public void render(float delta) {
        // 只要 clock.paused 是 true，时间就不会走
        clock.update(delta, state);

        if (state.ending != null) {
            EndingDef def = state.library.ending(state.ending);
            ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(new EndingScreen(ui, state, def));
            return;
        }

        if (dirty || signature() != lastSignature) {
            rebuild();
            lastSignature = signature();
            dirty = false;
        }
        refreshInfo();

        if (dnd.isDragging()) {
            Actor a = dnd.getDragActor();
            if (a != null) {
                if (a.getStage() == null) worldStage.addActor(a);
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                worldStage.screenToStageCoordinates(tmp);
                a.setPosition(tmp.x - grabX, tmp.y - grabY);
            }
        }

        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        worldStage.act(delta);
        worldStage.draw();
        uiStage.act(delta);
        uiStage.draw();
    }

    private int signature() {
        int h = state.cards.size();
        for (ElementInstance c : state.cards) h = 31 * h + c.instanceId.hashCode() + (c.locked ? 1 : 0);
        for (Situation s : state.situations) {
            h = 31 * h + (s.isRunning() ? 1 : 0);
            h = 31 * h + s.slots.size();
            h = 31 * h + (s.selected == null ? 0 : s.selected.id.hashCode());
            for (Slot slot : s.slots) h = 31 * h + (slot.content == null ? 0 : slot.content.instanceId.hashCode());
        }
        return h;
    }

    @Override
    public void show() {
        if (multiplexer != null) {
            Gdx.input.setInputProcessor(multiplexer);
        }
    }
    @Override public void resize(int width, int height) {
        worldStage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { worldStage.dispose(); uiStage.dispose(); }
}
