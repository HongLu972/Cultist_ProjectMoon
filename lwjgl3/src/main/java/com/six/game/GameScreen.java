package game;

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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import game.core.*;

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {
    private static final float STAGE_W = 1280, STAGE_H = 720;
    private static final String KIND_CARD = "card";
    private static final String KIND_SLOT = "slot";
    private static final String KIND_WINDOW = "window";

    private final Ui ui;
    private final GameState state;
    private final Clock clock = new Clock();

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

    public GameScreen(Ui ui, GameState state, float[] speeds, boolean loadSave) {
        this.ui = ui;
        this.state = state;
        this.infoPanel = new InfoPanel(ui);
        this.logPanel = new LogPanel(ui);
        this.pausedLabel = new Label(ui.texts.get("ui.paused"), ui.labelStyle(Color.SALMON));
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

        InputMultiplexer multiplexer = new InputMultiplexer(uiStage, worldStage);
        Gdx.input.setInputProcessor(multiplexer);

        setupWorldInput();
        setupGlobalDropTarget();

        state.addListener((situation, event) -> {
            String key = "event." + event.id + ".result";
            if (ui.texts.has(key)) logPanel.add(ui.texts.get(key), ui);
            state.chronicle.addRecord(state.totalTime, "结算事件: " + ui.texts.get("event." + event.id + ".name", event.id));
        });

        uiStage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.F5) { saveGame(); return true; }
                if (keycode == Input.Keys.F9) { loadGame(); return true; }
                return false;
            }
        });

        if (loadSave) {
            loadGame();
        } else {
            spawnTimeWindow();
            state.chronicle.addRecord(0, "研究所正式启动。");
        }
    }

    private void spawnTimeWindow() {
        EventDef genEvent = state.events.get("delta");
        if (genEvent != null) {
            Situation s = state.openEventWindow(genEvent, null, state.worldWidth - 400, state.worldHeight - 400);
            s.start();
            dirty = true;
        }
        EventDef timeEvent = state.events.get("time_A");
        if (timeEvent != null) {
            Situation s = state.openEventWindow(timeEvent, null, state.worldWidth - 400, state.worldHeight - 600);
            s.start();
            dirty = true;
        }
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

    private void setupGlobalDropTarget() {
        Actor bg = worldGroup.getChild(0);
        dnd.addTarget(new DragAndDrop.Target(bg) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                return true;
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                ElementInstance card = (ElementInstance) payload.getObject();
                detach(card);
                // 直接用相对背景的坐标
                card.x = x - grabX;
                card.y = y - grabY;
                dirty = true;
            }
        });
    }

    private void rebuild() {
        dnd.clear();

        for (int i = worldGroup.getChildren().size - 1; i >= 0; i--) {
            Actor a = worldGroup.getChildren().get(i);
            if (!(a instanceof WorldBackground)) a.remove();
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
            if (state.isInAnySlot(c)) continue;
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

    // --- 核心修复：回归 DragAndDrop 的虚影机制 ---
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
                p.setDragActor(new CardActor(ui, actor.card)); // 创建虚影
                actor.setVisible(false); // 隐藏原卡
                return p;
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer, DragAndDrop.Payload payload, DragAndDrop.Target target) {
                actor.setVisible(true);
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
    // ---------------------------------------------

    private void addCardRightClick(CardActor actor) {
        actor.addListener(new ClickListener(Input.Buttons.RIGHT) {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (state.isInAnySlot(actor.card)) {
                    detach(actor.card);
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
        return new String[] { title, ui.texts.get("element." + c.def.id + ".desc", "") };
    }

    private String[] slotInfo(String slotId) {
        Slot s = findSlot(slotId);
        if (s == null) return null;
        String title = ui.texts.get("slot." + s.defId + ".name", s.defId);
        return new String[] { title, ui.texts.get("slot." + s.defId + ".desc", "") };
    }

    private String[] windowInfo(String situationId) {
        Situation s = findSituation(situationId);
        if (s == null) return null;
        EventDef e = s.running != null ? s.running : s.selected;
        if (e == null) return new String[] { ui.texts.get("slot." + s.verbId + ".name", s.verbId), "" };
        return new String[] { ui.texts.get("event." + e.id + ".name", e.id), ui.texts.get("event." + e.id + ".desc", "") };
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

    private void saveGame() {
        SaveData data = state.createSaveData();
        String json = new Json().toJson(data);
        Gdx.files.local("savegame.json").writeString(json, false);
        logPanel.add("【系统】存档成功 (F5)", ui);
    }

    private void loadGame() {
        if (!Gdx.files.local("savegame.json").exists()) return;
        String json = Gdx.files.local("savegame.json").readString();
        SaveData data = new Json().fromJson(SaveData.class, json);
        state.loadFromSave(data);
        dirty = true;
        logPanel.add("【系统】读档成功 (F9)", ui);
        if (state.ending != null) {
            EndingDef def = state.library.ending(state.ending);
            ((com.badlogic.gdx.Game) Gdx.app.getApplicationListener()).setScreen(new EndingScreen(ui, state, def));
        }
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) clock.paused = !clock.paused;
        pausedLabel.setVisible(clock.paused);
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

        // DND 虚影完美跟随鼠标
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

    @Override public void show() {}
    @Override public void resize(int width, int height) {
        worldStage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { worldStage.dispose(); uiStage.dispose(); }
}
