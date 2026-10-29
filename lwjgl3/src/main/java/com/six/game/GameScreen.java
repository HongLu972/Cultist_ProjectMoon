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
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import game.core.Clock;
import game.core.ElementInstance;
import game.core.EventDef;
import game.core.GameState;
import game.core.Situation;
import game.core.Slot;
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

    private final Stage worldStage = new Stage(new FitViewport(STAGE_W, STAGE_H));
    private final Stage uiStage = new Stage(new FitViewport(STAGE_W, STAGE_H));

    private final Group worldGroup = new Group();

    private final InfoPanel infoPanel;
    private final LogPanel logPanel;
    private final Table speedBar = new Table();
    private final Label pausedLabel;
    private final Vector2 tmp = new Vector2();

    private String selKind;
    private String selId;
    private float infoX, infoY;
    private String lastInfoTitle, lastInfoBody;

    private Table endingOverlay;
    private boolean dirty = true;
    private int lastSignature;

    public GameScreen(Ui ui, GameState state, float[] speeds) {
        this.ui = ui;
        this.state = state;
        this.infoPanel = new InfoPanel(ui);
        this.logPanel = new LogPanel(ui);
        this.pausedLabel = new Label(ui.texts.get("ui.paused"), ui.labelStyle(Color.SALMON));
        this.pausedLabel.setVisible(false);
        buildSpeedBar(speeds);

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

        state.addListener((situation, event) -> {
            String key = "event." + event.id + ".result";
            if (ui.texts.has(key)) {
                logPanel.add(ui.texts.get(key), ui);
            } else {
                logPanel.add("【" + ui.texts.get("event." + event.id + ".name", event.id) + "】已结算", ui);
            }
        });

        // 开局立刻生成第一个“时间流逝”窗口
        spawnTimeWindow();
    }

    private void spawnTimeWindow() {
        EventDef timeEvent = state.events.get("time_A"); // 改这里
        if (timeEvent != null) {
            Situation s = state.openEventWindow(timeEvent, null, state.worldWidth - 400, state.worldHeight - 400);
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
                    if (event.getTarget() instanceof CardActor) return false;
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
        for (int i = worldGroup.getChildren().size - 1; i >= 0; i--) {
            Actor a = worldGroup.getChildren().get(i);
            if (!(a instanceof WorldBackground)) a.remove();
        }

        List<WindowView> views = new ArrayList<>();
        for (Situation s : state.situations) {
            WindowView v = new WindowView(ui, s);
            v.setPosition(s.x, s.y);
            addWindowClick(v);
            addWindowDrag(v); // 整个窗口绑定拖拽
            worldGroup.addActor(v);
            views.add(v);
        }

        for (ElementInstance c : state.cards) {
            if (state.isInAnySlot(c)) continue;
            CardActor a = new CardActor(ui, c);
            if (!Float.isNaN(c.x)) a.setPosition(c.x, c.y);
            addCardDrag(a);
            addSelectOnClick(a);
            addCardRightClick(a);
            worldGroup.addActor(a);
        }

        for (WindowView v : views) {
            for (SlotActor sa : v.slotActors) {
                addSelectOnClick(sa);
                if (sa.cardActor != null) {
                    addCardDrag(sa.cardActor);
                    addSelectOnClick(sa.cardActor);
                    addCardRightClick(sa.cardActor);
                }
            }
        }
    }

    private void addWindowDrag(WindowView view) {
        view.addListener(new DragListener() {
            private float startX, startY, actorX, actorY;
            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                // 记录拖拽初始位置，防止窗口跳动
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

    private void addCardDrag(CardActor actor) {
        actor.addListener(new DragListener() {
            private float offsetX, offsetY;
            private boolean isDragging = false;

            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                if (actor.card.locked) return;
                isDragging = true;
                offsetX = x;
                offsetY = y;
                actor.toFront(); // 拖拽时置顶
                hideInfo();
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                if (!isDragging) return;
                // 使用 Stage 坐标实时跟随鼠标，完美解决卡顿和偏移
                Vector2 stage = new Vector2(event.getStageX(), event.getStageY());
                worldGroup.stageToLocalCoordinates(stage);
                actor.setPosition(stage.x - offsetX, stage.y - offsetY);
            }

            @Override
            public void dragStop(InputEvent event, float x, float y, int pointer) {
                if (!isDragging) return;
                isDragging = false;

                Vector2 stagePos = new Vector2(event.getStageX(), event.getStageY());
                boolean droppedInSlot = false;

                // 物理碰撞检测：鼠标松开的位置是否覆盖了某个 Slot
                for (Actor child : worldGroup.getChildren()) {
                    if (child instanceof WindowView wv) {
                        for (SlotActor sa : wv.slotActors) {
                            Vector2 local = sa.stageToLocalCoordinates(new Vector2(stagePos));
                            if (local.x >= 0 && local.x <= sa.getWidth() && local.y >= 0 && local.y <= sa.getHeight()) {
                                if (wv.situation.canPlace(sa.index, actor.card)) {
                                    detach(actor.card);
                                    wv.situation.placeCard(sa.index, actor.card);
                                    droppedInSlot = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (droppedInSlot) break;
                }

                if (!droppedInSlot) {
                    actor.card.x = actor.getX();
                    actor.card.y = actor.getY();
                }
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
        view.titleLabel.addListener(new ClickListener(Input.Buttons.LEFT) {
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
        return new String[] { title, body.toString() };
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
        for (ElementInstance c : state.cards) {
            if (c.instanceId.equals(instanceId)) return c;
        }
        return null;
    }

    private Slot findSlot(String slotId) {
        for (Situation s : state.situations) {
            for (Slot slot : s.slots) {
                if (slot.id.equals(slotId)) return slot;
            }
        }
        return null;
    }

    private Situation findSituation(String situationId) {
        for (Situation s : state.situations) {
            if (s.situationId.equals(situationId)) return s;
        }
        return null;
    }

    private void showEnding() {
        hideInfo();
        endingOverlay = new Table();
        endingOverlay.setFillParent(true);
        endingOverlay.setBackground(ui.rect(new Color(0f, 0f, 0f, 0.85f)));
        endingOverlay.setTouchable(Touchable.enabled);
        Label t = new Label(ui.texts.get("ending." + state.ending + ".name", state.ending), ui.labelStyle(Color.GOLD));
        Label b = new Label(ui.texts.get("ending." + state.ending + ".desc", ""), ui.labelStyle(Color.LIGHT_GRAY));
        b.setWrap(true);
        endingOverlay.add(t).row();
        endingOverlay.add(b).width(520).padTop(16);
        uiStage.addActor(endingOverlay);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) clock.paused = !clock.paused;
        pausedLabel.setVisible(clock.paused);
        clock.update(delta, state);

        if (state.ending != null && endingOverlay == null) showEnding();

        if (dirty || signature() != lastSignature) {
            rebuild();
            lastSignature = signature();
            dirty = false;
        }
        refreshInfo();

        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        worldStage.act(delta);
        worldStage.draw();
        uiStage.act(delta);
        uiStage.draw();
    }

    private int signature() {
        int h = state.cards.size();
        for (ElementInstance c : state.cards) {
            h = 31 * h + c.instanceId.hashCode() + (c.locked ? 1 : 0);
        }
        for (Situation s : state.situations) {
            h = 31 * h + (s.isRunning() ? 1 : 0);
            h = 31 * h + s.slots.size();
            h = 31 * h + (s.selected == null ? 0 : s.selected.id.hashCode());
            for (Slot slot : s.slots) {
                h = 31 * h + (slot.content == null ? 0 : slot.content.instanceId.hashCode());
            }
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
