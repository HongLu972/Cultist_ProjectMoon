package com.six.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.six.game.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameScreen implements Screen {
    private static final float STAGE_W = 1280, STAGE_H = 720;
    private static final String KIND_CARD = "card";
    private static final String KIND_SLOT = "slot";
    private static final String KIND_WINDOW = "window";

    // --- 密教式地图小图标布局参数 ---
    private static final float TOKEN = 56f;      // 图标边长
    private static final Color TOKEN_BG = new Color(0.15f, 0.12f, 0.20f, 0.97f);
    private static final Color TOKEN_BG_HOVER = new Color(0.32f, 0.26f, 0.42f, 1f);

    private final Ui ui;
    private final GameState state;
    private final Clock clock = new Clock();

    private InputMultiplexer multiplexer;

    private final Stage worldStage = new Stage(new FitViewport(STAGE_W, STAGE_H));
    private final Stage uiStage = new Stage(new FitViewport(STAGE_W, STAGE_H));

    private final Group worldGroup = new Group();
    private final DragAndDrop dnd = new DragAndDrop();
    private final java.util.HashSet<String> seenIds = new java.util.HashSet<>();

    private final InfoPanel infoPanel;
    private final LogPanel logPanel;
    private final Table speedBar = new Table();
    private final Label pausedLabel;
    private final Table dockTip;        // 地图小图标悬停提示(uiStage 顶层,跨坐标换算)
    private final Label dockTipLabel;
    private final Vector2 tmp = new Vector2();

    private String selKind;
    private String selId;
    private float infoX, infoY;
    private String lastInfoTitle, lastInfoBody;

    private boolean dirty = true;
    private int lastSignature;
    private float grabX = CardActor.W / 2f, grabY = CardActor.H / 2f;

    private final Runnable onExitToMenu;

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

        setupWorldInput();

        multiplexer = new InputMultiplexer(uiStage, worldStage);
        Gdx.input.setInputProcessor(multiplexer);

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
                    openSystemMenu();
                    return true;
                }
                return false;
            }
        });

        // --- 左上角系统菜单按钮 ---
        Table topLeftTable = new Table();
        topLeftTable.setFillParent(true);
        topLeftTable.top().left().pad(8);

        TextButton modMenuBtn = new TextButton("☰ 系统菜单", ui.buttonStyle());
        modMenuBtn.addListener(new ClickListener(Input.Buttons.LEFT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openSystemMenu();
            }
        });

        topLeftTable.add(modMenuBtn).size(110, 34);
        uiStage.addActor(topLeftTable);

        // --- 密教式 UI:地图小图标 + 点开展开面板 + 悬停提示 ---
        dockTip = new Table();
        dockTipLabel = new Label("", ui.labelStyle(Color.GOLD));
        dockTip.add(dockTipLabel).pad(4, 8, 4, 8);
        dockTip.setBackground(ui.rect(new Color(0.10f, 0.08f, 0.14f, 0.95f)));
        dockTip.setTouchable(Touchable.disabled);
        dockTip.setVisible(false);
        uiStage.addActor(dockTip);

        if (state.situations.isEmpty()) {
            spawnInitialEvents();
        }
        String currentBgm = AudioManager.getInstance().getCurrentBgmPath();
        if (currentBgm == null || currentBgm.isEmpty() || currentBgm.equals("audio/bgm_menu.ogg")) {
            AudioManager.getInstance().playBgm("audio/bgm_game.ogg");
        }
    }

    // 👇 核心防御 1：坐标消毒剂
    private float sanitizeCoord(float val, float maxBound) {
        if (Float.isNaN(val) || Float.isInfinite(val)) return maxBound / 2f;
        // 如果超出世界地图边界 2000 像素，直接拉回中心
        if (val < -2000f || val > maxBound + 2000f) return maxBound / 2f;
        return val;
    }

    private void spawnInitialEvents() {
        float centerX = worldStage.getCamera().position.x;
        float centerY = worldStage.getCamera().position.y;

        EventDef genEvent = state.events.get("delta");
        if (genEvent != null) {
            Situation s = state.openEventWindow(genEvent, null, centerX + 300, centerY + 100);
            s.start();
        }

        EventDef timeEvent = state.events.get("time_A");
        if (timeEvent != null) {
            Situation s = state.openEventWindow(timeEvent, null, centerX + 300, centerY - 200);
            s.start();
        }

        state.chronicle.addRecord(0, "已至。");
    }

    /** 打开密教式系统菜单(独立全屏界面,风格同 ModEditorScreen)。 */
    private void openSystemMenu() {
        hideInfo();
        ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(
            new SystemMenuScreen(ui, state, this, onExitToMenu));
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

        // 新出现的局面:事件窗自动展开成面板,行动窗收成地图小图标
        for (Situation s : state.situations) {
            if (seenIds.add(s.situationId) && s.eventWindow) s.uiExpanded = true;
        }

        java.util.Set<ElementInstance> slotCards = new java.util.HashSet<>();
        for (Situation s : state.situations) {
            for (Slot slot : s.slots) {
                if (slot.content != null) slotCards.add(slot.content);
            }
        }

        buildTokens();

        for (ElementInstance c : state.cards) {
            if (slotCards.contains(c)) continue;
            CardActor a = new CardActor(ui, c);
            // 👇 核心防御 2：每次渲染前消毒卡牌坐标
            if (!Float.isNaN(c.x)) a.setPosition(sanitizeCoord(c.x, state.worldWidth), sanitizeCoord(c.y, state.worldHeight));
            addSource(a);
            addSelectOnClick(a);
            addCardRightClick(a);
            worldGroup.addActor(a);
        }

        buildExpandedPanels();
    }

    // ---------- 地图小图标(与卡牌同在世界层) ----------

    private void buildTokens() {
        for (Situation s : state.situations) {
            TokenActor t = new TokenActor(s);
            t.setPosition(sanitizeCoord(s.x, state.worldWidth), sanitizeCoord(s.y, state.worldHeight));
            worldGroup.addActor(t);

            final Situation fs = s;
            dnd.addTarget(new DragAndDrop.Target(t) {
                @Override
                public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    return firstPlaceableSlot(fs, (ElementInstance) payload.getObject()) >= 0;
                }

                @Override
                public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                    ElementInstance card = (ElementInstance) payload.getObject();
                    int idx = firstPlaceableSlot(fs, card);
                    if (idx >= 0) {
                        detach(card);
                        fs.placeCard(idx, card);
                        fs.uiExpanded = true; // 密教式:把牌丢到图标上,面板当场展开
                        dirty = true;
                    }
                }
            });
        }
    }

    private int firstPlaceableSlot(Situation s, ElementInstance card) {
        if (card == null || card.locked) return -1;
        for (int i = 0; i < s.slots.size(); i++) {
            if (s.canPlace(i, card)) return i;
        }
        return -1;
    }

    // ---------- 展开的局面面板(世界系,挂在图标上方) ----------

    private void buildExpandedPanels() {
        for (Situation s : state.situations) {
            if (!s.uiExpanded) continue;
            WindowView v = new WindowView(ui, s);
            addWindowClick(v);
            worldGroup.addActor(v);
            // 密教式:面板长在地图小图标上方,小图标保留作收起开关
            v.setPosition(
                sanitizeCoord(s.x, state.worldWidth),
                sanitizeCoord(s.y, state.worldHeight) + TOKEN + 6f);

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

    private void toggleExpand(Situation s) {
        s.uiExpanded = !s.uiExpanded;
        dirty = true;
    }

    private String situationTitle(Situation s) {
        if (s.eventWindow && s.selected != null) {
            return ui.texts.get("event." + s.selected.id + ".name", s.selected.id);
        }
        if (s.verbId != null) {
            return ui.texts.get("slot." + s.verbId + ".name", s.verbId);
        }
        return s.situationId;
    }

    // ---------- 图标悬停提示 ----------

    private void showDockTip(TokenActor t) {
        dockTipLabel.setText(situationTitle(t.s));
        dockTip.pack();
        tmp.set(t.getWidth() + 6f, t.getHeight() / 2f - dockTip.getHeight() / 2f);
        t.localToStageCoordinates(tmp);            // 图标 → 世界 Stage 坐标
        worldStage.stageToScreenCoordinates(tmp);  // → 屏幕像素
        uiStage.screenToStageCoordinates(tmp);     // → uiStage 逻辑坐标
        dockTip.setPosition(
            Math.min(tmp.x, STAGE_W - dockTip.getWidth() - 4f),
            Math.max(4f, Math.min(tmp.y, STAGE_H - dockTip.getHeight() - 4f)));
        dockTip.setVisible(true);
        dockTip.toFront();
    }

    private void hideDockTip() {
        dockTip.setVisible(false);
    }

    // ---------- 图标本体 ----------

    private class TokenActor extends Table {
        final Situation s;
        private final Table progressTrack;
        private final Table progressFill;
        private Cell<?> fillCell;

        TokenActor(Situation s) {
            this.s = s;
            setSize(TOKEN, TOKEN);
            setBackground(ui.rect(TOKEN_BG));
            setTouchable(Touchable.enabled);

            // 槽里有牌就展示牌面,没有就展示名称首字
            ElementInstance seed = s.slots.isEmpty() ? null : s.slots.get(0).content;
            Texture tex = (seed != null && seed.def.image != null && !seed.def.image.isEmpty())
                ? ui.texture(seed.def.image) : null;
            if (tex != null) {
                Image img = new Image(tex);
                img.setScaling(Scaling.fit);
                add(img).size(TOKEN - 10, TOKEN - 18).pad(5).row();
            } else {
                String title = situationTitle(s);
                String ch = title.isEmpty() ? "?" : new String(Character.toChars(title.codePointAt(0)));
                Label l = new Label(ch, ui.labelStyle(Color.GOLD));
                l.setTouchable(Touchable.disabled);
                add(l).expand().center().row();
            }

            progressTrack = new Table();
            progressTrack.setBackground(ui.rect(new Color(0.08f, 0.07f, 0.11f, 1f)));
            progressFill = new Table();
            progressFill.setBackground(ui.rect(new Color(0.62f, 0.45f, 0.20f, 1f)));
            fillCell = progressTrack.add(progressFill).height(4f).width(0f);
            progressTrack.setTouchable(Touchable.disabled);
            progressTrack.setVisible(false);
            add(progressTrack).fillX().height(4f).pad(3, 6, 5, 6);

            addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    setBackground(ui.rect(TOKEN_BG_HOVER));
                    showDockTip(TokenActor.this);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    if (toActor == null || !toActor.isDescendantOf(TokenActor.this)) {
                        setBackground(ui.rect(TOKEN_BG));
                        hideDockTip();
                    }
                }
            });
            addListener(new ClickListener(Input.Buttons.LEFT) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    toggleExpand(s);
                }
            });
            // 图标可在地图上拖动(移动后写回 s.x/s.y,随存档保存)
            addListener(new DragListener() {
                private float gx, gy;

                @Override
                public void dragStart(InputEvent event, float x, float y, int pointer) {
                    gx = x;
                    gy = y;
                }

                @Override
                public void drag(InputEvent event, float x, float y, int pointer) {
                    moveBy(x - gx, y - gy);
                    s.x = getX();
                    s.y = getY();
                }
            });
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            boolean run = s.isRunning() && s.running.time > 0;
            progressTrack.setVisible(run);
            if (run) {
                float frac = Math.min(1f, Math.max(0f, s.timer / s.running.time));
                fillCell.width((TOKEN - 12f) * frac);
            }
        }
    }

    // ---------- 拖拽 ----------

    private void addSource(CardActor actor) {
        dnd.addSource(new DragAndDrop.Source(actor) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (actor.card.locked) return null;
                // 👇 核心防御 3：防呆抓取坐标
                grabX = Float.isNaN(x) ? CardActor.W / 2f : x;
                grabY = Float.isNaN(y) ? CardActor.H / 2f : y;
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

                if (target == null) {
                    detach(card);
                    Vector2 stagePos = new Vector2(event.getStageX(), event.getStageY());
                    worldGroup.stageToLocalCoordinates(stagePos);

                    // 👇 核心防御 4：落地坐标消毒
                    float rawX = stagePos.x - grabX;
                    float rawY = stagePos.y - grabY;

                    card.x = sanitizeCoord(rawX, state.worldWidth);
                    card.y = sanitizeCoord(rawY, state.worldHeight);
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
                    actor.card.x = sanitizeCoord(stagePos.x, state.worldWidth);
                    actor.card.y = sanitizeCoord(stagePos.y, state.worldHeight);
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
        infoPanel.toFront();
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

        Map<String, Integer> currentAspects = c.visibleAspects();
        if (!currentAspects.isEmpty()) {
            body.append("\n\n").append(ui.texts.get("ui.aspects", "词条")).append(": ").append(aspectList(currentAspects));
        }

        if (c.def.lifetime > 0) {
            body.append("\n").append(ui.texts.get("ui.lifetime", "寿命")).append(": ")
                .append((int) Math.ceil(Math.max(0f, c.remaining))).append(" ").append(ui.texts.get("ui.seconds", "秒"));
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
        clock.update(delta, state);

        if (state.ending != null) {
            EndingDef def = state.library.ending(state.ending);
            ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(new EndingScreen(ui, state, def));
            return;
        }

        // 👇 核心防御 5：拖拽中绝对不重建！
        if (!dnd.isDragging() && (dirty || signature() != lastSignature)) {
            rebuild();
            lastSignature = signature();
            dirty = false;
        }
        refreshInfo();

        if (dnd.isDragging()) {
            Actor a = dnd.getDragActor();
            if (a != null) {
                // 虚影挂到 worldStage 顶层(源/目标已全部回到世界系,缩放下判定一致)
                if (a.getStage() == null) worldStage.addActor(a);
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                worldStage.screenToStageCoordinates(tmp);

                // 👇 核心防御 6：虚影跟随鼠标时也要消毒
                float targetX = sanitizeCoord(tmp.x - grabX, STAGE_W);
                float targetY = sanitizeCoord(tmp.y - grabY, STAGE_H);
                a.setPosition(targetX, targetY);
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
