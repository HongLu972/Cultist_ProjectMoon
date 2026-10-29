package game.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    /** 引擎向外通知事件的接口。界面用它写"记录"。situation 在自动事件时为 null。 */
    public interface Listener {
        void eventCompleted(Situation situation, EventDef event);
    }

    /** 不占窗口的后台事件(自动事件,或被自动事件 trigger 的事件)。 */
    public static class BackgroundRun {
        public final EventDef event;
        public float timer;
        public final boolean repeat;

        BackgroundRun(EventDef event, float timer, boolean repeat) {
            this.event = event;
            this.timer = timer;
            this.repeat = repeat;
        }
    }

    public final Library library;
    public final EventBook events;
    public final List<ElementInstance> cards = new ArrayList<>();
    public final List<Situation> situations = new ArrayList<>();
    public final List<BackgroundRun> backgrounds = new ArrayList<>();
    /** 所有随机判定都走这一个 Random。测试或回放时可以换成带种子的。 */
    public Random random = new Random();
    /** 非 null 表示游戏已结束,值是结局 id。结束后 tick 不再推进。 */
    public String ending;

    private final List<Listener> listeners = new ArrayList<>();

    // id 计数器属于存档的一部分,放在状态里而不是 static,测试互不污染。
    private int nextCardId = 0;
    private int nextSituationId = 0;

    public GameState(Library library, EventBook events) {
        this.library = library;
        this.events = events;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    void fireEventCompleted(Situation s, EventDef e) {
        for (Listener l : new ArrayList<>(listeners)) l.eventCompleted(s, e);
    }

    public ElementInstance addCard(Element def) {
        ElementInstance c = new ElementInstance(def, def.id + "#" + (nextCardId++));
        cards.add(c);
        return c;
    }

    /** 删除一张牌,同时把它从所有槽里清掉。 */
    public void removeCard(ElementInstance c) {
        cards.remove(c);
        for (Situation s : situations) {
            for (Slot slot : s.slots) {
                if (slot.content == c) slot.content = null;
            }
        }
    }

    public boolean isInAnySlot(ElementInstance card) {
        for (Situation s : situations) {
            for (Slot slot : s.slots) {
                if (slot.content == card) return true;
            }
        }
        return false;
    }

    public Situation addSituation(String verbId) {
        VerbDef verb = library.verb(verbId);
        Situation s = new Situation(this, verbId + "_" + (nextSituationId++), verb);
        situations.add(s);
        return s;
    }

    public long count(String elementId) {
        return cards.stream().filter(c -> c.def.id.equals(elementId)).count();
    }

    // ---------------------------------------------------------------- 后台事件

    /** 游戏开始时调用:为每个 auto 事件建立循环计时。 */
    public void startAutoEvents() {
        for (EventDef e : events.all()) {
            if ("auto".equals(e.start)) backgrounds.add(new BackgroundRun(e, e.time, true));
        }
    }

    /** 作为后台事件运行:time 为 0 则立刻结算,否则等 time 秒后结算一次。 */
    public void runBackground(String eventId) {
        EventDef e = events.require(eventId);
        if (e.time <= 0) {
            executeBackground(e);
        } else {
            backgrounds.add(new BackgroundRun(e, e.time, false));
        }
    }

    private void executeBackground(EventDef e) {
        ActionContext ctx = new ActionContext(this, null, e);
        Action.runAll(e.action, ctx);
        fireEventCompleted(null, e);
    }

    // ---------------------------------------------------------------- 时间推进

    /** 推进 dt 秒(已乘过倍速)。游戏结束后不再推进。 */
    public void tick(float dt) {
        if (ending != null) return;

        // 先给"已有的牌"计寿命,再跑事件:本次 tick 里新产生的牌(比如刚发放的疾病)不应该在出生的这一刻就老化
        for (ElementInstance c : new ArrayList<>(cards)) {
            if (!(c.def.lifetime > 0)) continue;
            c.remaining -= dt;
            if (c.remaining <= 0) expire(c);
        }

        for (Situation s : new ArrayList<>(situations)) s.update(dt);

        for (BackgroundRun r : new ArrayList<>(backgrounds)) {
            r.timer -= dt;
            if (r.timer > 0) continue;
            executeBackground(r.event);
            if (r.repeat) {
                r.timer += r.event.time;
            } else {
                backgrounds.remove(r);
            }
        }
    }

    private void expire(ElementInstance c) {
        removeCard(c);
        String to = c.def.decayTo;
        if (to != null && !to.isEmpty()) addCard(library.element(to));
    }
}
