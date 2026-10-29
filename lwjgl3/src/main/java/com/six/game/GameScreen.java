package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
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

/**
 * 桌面主界面。原则:引擎状态是唯一真相,界面只是它的投影。
 * 状态一变(或拖拽落下),就整体重建界面和拖拽注册——简单、不会漂移。
 * 重建不能发生在拖拽过程中,否则被拖的 Actor 会被销毁。
 * 选中状态用 id 保存(不持有 Actor),重建后依然有效。
 * 描述文字默认不显示:点卡牌、槽位、窗口标题才在点击处弹出信息框,点空白处收起。
 */
public class GameScreen implements Screen {
    private static final float STAGE_W = 1280, STAGE_H = 720;
    private static final String KIND_CARD = "card";
    private static final String KIND_SLOT = "slot";
    private static final String KIND_WINDOW = "window";

    private final Ui ui;
    private final GameState state;
    private final Clock clock = new Clock();
    private final Stage stage = new Stage(new FitViewport(STAGE_W, STAGE_H));
    private final DragAndDrop dnd = new DragAndDrop();
    private final Table root = new Table();
    private final InfoPanel infoPanel;
    private final LogPanel logPanel;
    private final Table speedBar = new Table();
    private final Label pausedLabel;
    private final Vector2 tmp = new Vector2();

    // 拖拽时"抓在牌的哪个位置"(相对牌左下角)。每帧据此摆放被拖的牌,保证牌紧贴鼠标。
    private float grabX = CardActor.W / 2f, grabY = CardActor.H / 2f;

    private String selKind;          // null 表示没有选中
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

        dnd.setKeepWithinStage(false);
        root.setFillParent(true);
        stage.addActor(root);
        stage.addActor(infoPanel); // 浮层,盖在 root 之上
        infoPanel.setVisible(false);

        // 点在空白处(没有点中任何 Actor)就收起信息框
        stage.getRoot().addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (event.getTarget() == stage.getRoot()) hideInfo();
            }
        });

        // 事件结算时,把 event.<id>.result 写进记录
        state.addListener((situation, event) -> {
            String key = "event." + event.id + ".result";
            if (ui.texts.has(key)) logPanel.add(ui.texts.get(key));
        });
    }

    // ---------------------------------------------------------------- 倍速条

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

    // ---------------------------------------------------------------- 重建界面

    private void rebuild() {
        dnd.clear();
        root.clear();

        root.add(speedBar).expandX().right().pad(8).row();

        Table windows = new Table();
        List<SituationView> views = new ArrayList<>();
        for (Situation s : state.situations) {
            SituationView v = new SituationView(ui, s);
            views.add(v);
            windows.add(v).top().pad(6);
            addWindowClick(v);
        }
        root.add(windows).expand().top().row();

        Table tray = new Table();
        tray.setBackground(ui.rect(new Color(0.10f, 0.08f, 0.13f, 1f)));
        tray.pad(10).top().left();
        int col = 0;
        for (ElementInstance c : state.cards) {
            if (state.isInAnySlot(c)) continue;
            CardActor a = new CardActor(ui, c);
            tray.add(a).size(CardActor.W, CardActor.H).pad(6);
            addSource(a);
            addSelectOnClick(a);
            if (++col % 8 == 0) tray.row();
        }
        Table bottom = new Table();
        bottom.add(tray).growX().height(240);
        bottom.add(logPanel).width(LogPanel.W).height(240).padLeft(8);
        root.add(bottom).growX();

        for (SituationView v : views) {
            for (SlotActor sa : v.slotActors) {
                addSlotTarget(sa);
                addSelectOnClick(sa);
                if (sa.cardActor != null) {
                    addSource(sa.cardActor);
                    addSelectOnClick(sa.cardActor);
                }
            }
        }
        addTrayTarget(tray);
    }

    // ---------------------------------------------------------------- 选中与信息框

    private void select(String kind, String id, InputEvent event) {
        selKind = kind;
        selId = id;
        infoX = event.getStageX();
        infoY = event.getStageY();
        lastInfoTitle = null; // 强制刷新并重新定位
    }

    private void addSelectOnClick(CardActor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(KIND_CARD, actor.card.instanceId, event);
            }
        });
    }

    private void addSelectOnClick(SlotActor actor) {
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 点在槽里的牌上时由牌自己处理;只有点在槽本身(空槽或边框)才选中槽
                if (event.getTarget() != actor) return;
                select(KIND_SLOT, actor.situation.slots.get(actor.index).id, event);
            }
        });
    }

    private void addWindowClick(SituationView view) {
        view.titleLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                select(KIND_WINDOW, view.situation.situationId, event);
            }
        });
    }

    private void hideInfo() {
        selKind = null;
        selId = null;
        lastInfoTitle = null;
        lastInfoBody = null;
        infoPanel.setVisible(false);
    }

    /** 每帧调用:内容(比如倒计时)变了才重新排版。 */
    private void refreshInfo() {
        if (selKind == null) return;
        String[] info;
        switch (selKind) {
            case KIND_CARD:
                info = cardInfo(selId);
                break;
            case KIND_SLOT:
                info = slotInfo(selId);
                break;
            default:
                info = windowInfo(selId);
                break;
        }
        if (info == null) { // 被选中的东西已经不存在了(比如卡牌被消耗)
            hideInfo();
            return;
        }
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
        if (!c.def.aspects.isEmpty()) {
            body.append("\n\n").append(ui.texts.get("ui.aspects")).append(": ").append(aspectList(c.def.aspects));
        }
        if (c.def.lifetime > 0) {
            body.append("\n").append(ui.texts.get("ui.lifetime")).append(": ")
                    .append((int) Math.ceil(Math.max(0f, c.remaining))).append(ui.texts.get("ui.seconds"));
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
            body.append(ui.texts.get("ui.seed_hint"));      // 起始槽:放牌来开始
        } else if (s.accepts.isEmpty()) {
            body.append(ui.texts.get("ui.accepts_any"));
        } else {
            body.append(ui.texts.get("ui.accepts")).append(": ").append(aspectList(s.accepts));
        }
        return new String[] { title, body.toString() };
    }

    private String[] windowInfo(String situationId) {
        Situation s = findSituation(situationId);
        if (s == null) return null;
        EventDef e = s.running != null ? s.running : s.selected;
        if (e == null) {
            return new String[] {
                ui.texts.get("slot." + s.verbId + ".name", s.verbId),
                ui.texts.get("slot." + s.verbId + ".desc", "") + "\n\n" + ui.texts.get("ui.seed_hint")
            };
        }
        StringBuilder body = new StringBuilder(ui.texts.get("event." + e.id + ".desc", ""));
        if (s.running != null) {
            body.append("\n\n").append(ui.texts.get("ui.remaining")).append(" ")
                    .append((int) Math.ceil(Math.max(0f, s.timer))).append(" ").append(ui.texts.get("ui.seconds"));
        }
        return new String[] { ui.texts.get("event." + e.id + ".name", e.id), body.toString() };
    }

    /** 词条列表,如 "身体 x1, 精神 x2"。按 key 排序,显示顺序稳定。 */
    private String aspectList(Map<String, Integer> aspects) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : new TreeMap<>(aspects).entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ui.texts.get("aspect." + e.getKey() + ".name", e.getKey())).append(" x").append(e.getValue());
        }
        return sb.toString();
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

    // ---------------------------------------------------------------- 拖拽

    private void addSource(CardActor actor) {
        dnd.addSource(new DragAndDrop.Source(actor) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (actor.card.locked) return null; // 计时中的牌拖不动
                grabX = x; // 按下位置相对于这张牌
                grabY = y;
                hideInfo();
                DragAndDrop.Payload p = new DragAndDrop.Payload();
                p.setObject(actor.card);
                p.setDragActor(new CardActor(ui, actor.card));
                return p;
            }
        });
    }

    private void addSlotTarget(SlotActor sa) {
        dnd.addTarget(new DragAndDrop.Target(sa) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                ElementInstance card = (ElementInstance) payload.getObject();
                Situation s = sa.situation;
                // 起始牌不能拖进自己窗口的其他槽(取走起始牌会让那些槽消失)
                if (s.slots.get(0).content == card) return false;
                // 规则全部由引擎判断,界面不重复写
                return s.canPlace(sa.index, card);
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

    /** 托盘也是目标:把槽里的牌拖回桌面。 */
    private void addTrayTarget(Table tray) {
        dnd.addTarget(new DragAndDrop.Target(tray) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                ElementInstance card = (ElementInstance) payload.getObject();
                return state.isInAnySlot(card) && !card.locked;
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                detach((ElementInstance) payload.getObject());
                dirty = true;
            }
        });
    }

    /** 把牌从它当前所在的槽里取出来(如果在槽里的话)。取走起始牌会让那个窗口回到空闲。 */
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

    // ---------------------------------------------------------------- 结局

    private void showEnding() {
        hideInfo();
        endingOverlay = new Table();
        endingOverlay.setFillParent(true);
        endingOverlay.setBackground(ui.rect(new Color(0f, 0f, 0f, 0.85f)));
        endingOverlay.setTouchable(Touchable.enabled); // 挡住下面的操作
        Label t = new Label(ui.texts.get("ending." + state.ending + ".name", state.ending), ui.labelStyle(Color.GOLD));
        Label b = new Label(ui.texts.get("ending." + state.ending + ".desc", ""), ui.labelStyle(Color.LIGHT_GRAY));
        b.setWrap(true);
        endingOverlay.add(t).row();
        endingOverlay.add(b).width(520).padTop(16);
        stage.addActor(endingOverlay);
    }

    // ---------------------------------------------------------------- 主循环

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) clock.paused = !clock.paused;
        pausedLabel.setVisible(clock.paused);
        clock.update(delta, state);

        if (state.ending != null && endingOverlay == null) showEnding();

        if (!dnd.isDragging() && (dirty || signature() != lastSignature)) {
            rebuild();
            lastSignature = signature();
            dirty = false;
        }
        refreshInfo();

        // 被拖的牌:每帧按鼠标位置摆放,抓在哪里就保持在哪里(避免依赖 DragAndDrop 内部的偏移约定)
        if (dnd.isDragging()) {
            Actor a = dnd.getDragActor();
            if (a != null) {
                tmp.set(Gdx.input.getX(), Gdx.input.getY());
                stage.screenToStageCoordinates(tmp);
                a.setPosition(tmp.x - grabX, tmp.y - grabY);
            }
        }

        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        stage.act(delta);
        stage.draw();
    }

    /** 轻量指纹:牌的集合、锁定状态、槽的数量与内容、运行状态有变化就会变。 */
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

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
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
