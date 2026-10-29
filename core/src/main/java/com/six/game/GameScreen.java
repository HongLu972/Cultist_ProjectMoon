package game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import game.core.Clock;
import game.core.ElementInstance;
import game.core.GameState;
import game.core.Situation;
import game.core.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * 桌面主界面。原则:引擎状态是唯一真相,界面只是它的投影。
 * 状态一变(或拖拽落下),就整体重建界面和拖拽注册——简单、不会漂移。
 * 重建不能发生在拖拽过程中,否则被拖的 Actor 会被销毁。
 */
public class GameScreen implements Screen {
    private final Ui ui;
    private final GameState state;
    private final Clock clock = new Clock();
    private final Stage stage = new Stage(new FitViewport(1280, 720));
    private final DragAndDrop dnd = new DragAndDrop();
    private final Table root = new Table();

    private boolean dirty = true;
    private int lastSignature;

    public GameScreen(Ui ui, GameState state) {
        this.ui = ui;
        this.state = state;
        // 让拖拽中的牌以光标为中心
        dnd.setDragActorPosition(-CardActor.W / 2f, CardActor.H / 2f);
        root.setFillParent(true);
        stage.addActor(root);
    }

    // ---------------------------------------------------------------- 重建界面

    private void rebuild() {
        dnd.clear();
        root.clear();

        Table windows = new Table();
        List<SituationView> views = new ArrayList<>();
        for (Situation s : state.situations) {
            SituationView v = new SituationView(ui, state, s, verbLabel(s.verbId));
            views.add(v);
            windows.add(v).top().pad(10);
        }
        root.add(windows).expand().top().row();

        Table tray = new Table();
        tray.setBackground(ui.rect(new Color(0.10f, 0.08f, 0.13f, 1f)));
        tray.pad(10).top().left();
        int col = 0;
        for (ElementInstance c : state.cards) {
            if (isInAnySlot(c)) continue;
            CardActor a = new CardActor(ui, c);
            tray.add(a).size(CardActor.W, CardActor.H).pad(6);
            addSource(a);
            if (++col % 10 == 0) tray.row();
        }
        root.add(tray).growX().height(240);

        for (SituationView v : views) {
            for (SlotActor sa : v.slotActors) {
                addSlotTarget(sa);
                if (sa.cardActor != null) addSource(sa.cardActor);
            }
        }
        addTrayTarget(tray);
    }

    // ---------------------------------------------------------------- 拖拽

    private void addSource(CardActor actor) {
        dnd.addSource(new DragAndDrop.Source(actor) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (actor.card.locked) return null; // 计时中的牌拖不动
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
                return !s.isRunning() && !card.locked && s.slots.get(sa.index).content == null;
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
                return isInAnySlot(card) && !card.locked;
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                detach((ElementInstance) payload.getObject());
                dirty = true;
            }
        });
    }

    private boolean isInAnySlot(ElementInstance card) {
        for (Situation s : state.situations) {
            for (Slot slot : s.slots) {
                if (slot.content == card) return true;
            }
        }
        return false;
    }

    /** 把牌从它当前所在的槽里取出来(如果在槽里的话)。 */
    private void detach(ElementInstance card) {
        for (Situation s : state.situations) {
            for (int i = 0; i < s.slots.size(); i++) {
                if (s.slots.get(i).content == card) s.takeCard(i);
            }
        }
    }

    // ---------------------------------------------------------------- 主循环

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            clock.timeScale = clock.timeScale == 0f ? 1f : 0f;
        }
        clock.update(delta, state);

        if (!dnd.isDragging() && (dirty || signature() != lastSignature)) {
            rebuild();
            lastSignature = signature();
            dirty = false;
        }

        ScreenUtils.clear(0.06f, 0.05f, 0.08f, 1f);
        stage.act(delta);
        stage.draw();
    }

    /** 轻量指纹:牌的集合、锁定状态、槽内容、运行状态有变化就会变。 */
    private int signature() {
        int h = state.cards.size();
        for (ElementInstance c : state.cards) {
            h = 31 * h + c.instanceId.hashCode() + (c.locked ? 1 : 0);
        }
        for (Situation s : state.situations) {
            h = 31 * h + (s.isRunning() ? 1 : 0);
            for (Slot slot : s.slots) {
                h = 31 * h + (slot.content == null ? 0 : slot.content.instanceId.hashCode());
            }
        }
        return h;
    }

    private static String verbLabel(String verbId) {
        return switch (verbId) {
            case "work" -> "工作";
            default -> verbId;
        };
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
