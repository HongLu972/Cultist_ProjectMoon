package game.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    /** 引擎向外通知事件的接口。界面用它写"记录"。situation 在后台事件时为 null。 */
    public interface Listener {
        void eventCompleted(Situation situation, EventDef event);
    }

    /** 不占窗口的后台事件(自动事件,或被后台事件 trigger 的事件)。 */
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

    /** 相邻两个窗口之间默认的水平间距。 */
    public static final float WINDOW_SPACING = 560f;
    private static final float CARD_STEP_X = 112f, CARD_STEP_Y = 150f;

    public final Library library;
    public final EventBook events;
    /** 桌面上所有的牌(含窗口槽里的)。越靠后越在上层。 */
    public final List<ElementInstance> cards = new ArrayList<>();
    public final List<Situation> situations = new ArrayList<>();
    public final List<BackgroundRun> backgrounds = new ArrayList<>();
    /** 所有随机判定都走这一个 Random。测试或回放时可以换成带种子的。 */
    public Random random = new Random();
    /** 非 null 表示游戏已结束,值是结局 id。结束后 tick 不再推进。 */
    public String ending;
    /** 世界地图大小。 */
    public float worldWidth = 3200f, worldHeight = 1800f;

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

    // ---------------------------------------------------------------- 开局

    /** 按开局设定初始化:解锁行动窗口、发牌、启动自动事件。 */
    public void begin(ScenarioDef sc) {
        for (String v : sc.verbs) unlockVerb(v);
        for (EventDef.Product p : sc.cards) addCards(library.element(p.card), p.amount);
        for (String e : sc.autoEvents) startAuto(e);
        placeNewCards(worldWidth / 2f - 200f, worldHeight / 2f - 100f);
    }

    // ---------------------------------------------------------------- 窗口

    /** 解锁一个行动窗口(已经有了就返回现有的)。新窗口顺着已有的往右排。 */
    public Situation unlockVerb(String verbId) {
        int n = 0;
        for (Situation s : situations) {
            if (s.eventWindow) continue;
            if (s.verbId.equals(verbId)) return s;
            n++;
        }
        VerbDef verb = library.verb(verbId);
        Situation s = new Situation(this, verbId + "_" + (nextSituationId++), verb);
        s.x = clamp(worldWidth / 2f - 200f + n * WINDOW_SPACING, 10f, worldWidth - 520f);
        s.y = clamp(worldHeight / 2f - 100f, 10f, worldHeight - 360f);
        situations.add(s);
        return s;
    }

    /**
     * 为 slot 为 none 的 manual 事件开一个临时的事件窗口。carrier 是触发它的牌(可为 null)。
     * 没有要等玩家做的事(无 input、无额外槽)或者事件声明了 autoStart 时,自动开始。
     */
    public Situation openEventWindow(EventDef e, ElementInstance carrier, float x, float y) {
        // 简单错开：如果这个位置有窗口了，就向右下偏移一点
        for (Situation existing : situations) {
            if (Math.abs(existing.x - x) < 100 && Math.abs(existing.y - y) < 100) {
                x += 60;
                y -= 60;
            }
        }

        Situation s = new Situation(this, "ev_" + e.id + "_" + (nextSituationId++), e, carrier);
        s.x = clamp(x, 10f, worldWidth - 520f);
        s.y = clamp(y, 10f, worldHeight - 360f);
        situations.add(s);
        boolean nothingToWaitFor = e.input.isEmpty() && e.slots.isEmpty();
        if ((e.autoStart || nothingToWaitFor) && s.canStart()) s.start();
        return s;
    }

    /** 双击"使用"一张牌:触发它的 use 触发器。成功返回 true。 */
    public boolean useCard(ElementInstance card) {
        if (ending != null || card.locked || isInAnySlot(card)) return false;
        String eventId = card.def.eventFor(Element.USE);
        if (eventId == null) return false;
        EventDef e = events.require(eventId);
        float cx = Float.isNaN(card.x) ? worldWidth / 2f : card.x;
        float cy = Float.isNaN(card.y) ? worldHeight / 2f : card.y;
        ElementInstance carrier = splitOne(card);
        openEventWindow(e, carrier, cx - 40f, cy + 170f);
        return true;
    }

    // ---------------------------------------------------------------- 牌

    /** 创建 n 张牌:可堆叠的并进桌面上现成的那摞(没有就新开一摞),不可堆叠的各自独立。返回最后涉及的那张/那摞。 */
    public ElementInstance addCards(Element def, int n) {
        if (n <= 0) throw new IllegalArgumentException("数量必须大于 0: " + n);
        if (def.stackable) {
            for (ElementInstance c : cards) {
                if (c.def == def && !c.locked && !isInAnySlot(c)) {
                    c.count += n;
                    return c;
                }
            }
            ElementInstance c = newCard(def);
            c.count = n;
            return c;
        }
        ElementInstance last = null;
        for (int i = 0; i < n; i++) last = newCard(def);
        return last;
    }

    public ElementInstance addCard(Element def) {
        return addCards(def, 1);
    }

    private ElementInstance newCard(Element def) {
        ElementInstance c = new ElementInstance(def, def.id + "#" + (nextCardId++));
        cards.add(c);
        return c;
    }

    /** 整摞删除,同时把它从所有槽里清掉。空闲窗口丢了起始牌会跟着收起。 */
    public void removeCard(ElementInstance c) {
        cards.remove(c);
        for (Situation s : new ArrayList<>(situations)) {
            for (int i = 0; i < s.slots.size(); i++) {
                if (s.slots.get(i).content == c) {
                    s.slots.get(i).content = null;
                    s.cardRemoved(i);
                    break;
                }
            }
        }
    }

    /** 从一摞里拿走 n 张;拿光了这摞就消失。 */
    public void removeFromStack(ElementInstance c, int n) {
        c.count -= n;
        if (c.count <= 0) removeCard(c);
    }

    /** 从一摞里拆出一张(新的独立实例,继承位置和寿命)。本来就只有一张则原样返回。 */
    public ElementInstance splitOne(ElementInstance stack) {
        if (stack.count <= 1) return stack;
        stack.count--;
        ElementInstance one = newCard(stack.def);
        one.remaining = stack.remaining;
        one.x = stack.x;
        one.y = stack.y;
        return one;
    }

    public boolean canMerge(ElementInstance src, ElementInstance dst) {
        return src != dst && src.def == dst.def && src.def.stackable
                && !src.locked && !dst.locked && !isInAnySlot(dst);
    }

    /** 把 src 整摞并进 dst。 */
    public void mergeInto(ElementInstance src, ElementInstance dst) {
        if (!canMerge(src, dst)) return;
        dst.count += src.count;
        if (dst.def.lifetime > 0) dst.remaining = Math.min(dst.remaining, src.remaining);
        removeCard(src);
    }

    /** 把牌提到最上层(绘制顺序里排到最后)。 */
    public void bringToFront(ElementInstance c) {
        if (cards.remove(c)) cards.add(c);
    }

    public boolean isInAnySlot(ElementInstance card) {
        for (Situation s : situations) {
            for (Slot slot : s.slots) {
                if (slot.content == card) return true;
            }
        }
        return false;
    }

    /** 桌上这种牌的总张数(堆叠的按张数算)。 */
    public long count(String elementId) {
        long total = 0;
        for (ElementInstance c : cards) {
            if (c.def.id.equals(elementId)) total += c.count;
        }
        return total;
    }

    /** 给所有还没位置的牌(不在槽里的)排位置:从 (nearX, nearY) 的下方依次摆开。 */
    public void placeNewCards(float nearX, float nearY) {
        int k = 0;
        for (ElementInstance c : cards) {
            if (!Float.isNaN(c.x) || isInAnySlot(c)) continue;
            c.x = clamp(nearX + (k % 5) * CARD_STEP_X, 10f, worldWidth - CARD_STEP_X);
            c.y = clamp(nearY - 170f - (k / 5) * CARD_STEP_Y, 10f, worldHeight - CARD_STEP_Y);
            k++;
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---------------------------------------------------------------- 后台事件

    /** 启动一个 auto 事件的自动循环;已经在循环则什么都不做。 */
    public void startAuto(String eventId) {
        EventDef e = events.require(eventId);
        for (BackgroundRun r : backgrounds) {
            if (r.event == e && r.repeat) return;
        }
        backgrounds.add(new BackgroundRun(e, e.time, true));
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
        placeNewCards(worldWidth / 2f - 200f, worldHeight / 2f - 100f);
    }

    // ---------------------------------------------------------------- 时间推进

    /** 推进 dt 秒(已乘过倍速)。游戏结束后不再推进。 */
    public void tick(float dt) {
        if (ending != null) return;

        // 先给"已有的牌"计寿命,再跑事件:本次 tick 里新产生的牌不应该在出生的这一刻就老化
        for (ElementInstance c : new ArrayList<>(cards)) {
            if (!(c.def.lifetime > 0)) continue;
            c.remaining -= dt;
            if (c.remaining <= 0) expire(c);
        }

        for (Situation s : new ArrayList<>(situations)) s.update(dt);
        situations.removeIf(s -> s.finished);

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

        placeNewCards(worldWidth / 2f - 200f, worldHeight / 2f - 100f);
    }

    private void expire(ElementInstance c) {
        float ox = c.x, oy = c.y;
        removeCard(c);
        String to = c.def.decayTo;
        if (to != null && !to.isEmpty()) {
            ElementInstance n = addCard(library.element(to));
            if (Float.isNaN(n.x) && !Float.isNaN(ox)) {
                n.x = ox;
                n.y = oy;
            }
        }
    }
}
