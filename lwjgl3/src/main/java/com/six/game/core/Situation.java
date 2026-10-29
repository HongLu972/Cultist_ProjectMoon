package game.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Situation {
    public final String situationId;
    public final String verbId;
    public final boolean eventWindow;
    public final List<Slot> slots = new ArrayList<>();
    public EventDef selected;
    public EventDef running;
    public float timer;
    public float x, y;
    public boolean finished;

    private final GameState state;
    private final boolean hasCarrier;

    Situation(GameState state, String situationId, VerbDef verb) {
        this.state = state;
        this.situationId = situationId;
        this.verbId = verb.id;
        this.eventWindow = false;
        this.hasCarrier = false;
        slots.add(new Slot(situationId + ":" + verb.id, verb.id, Map.of(), false));
    }

    Situation(GameState state, String situationId, EventDef event, ElementInstance carrier) {
        this.state = state;
        this.situationId = situationId;
        this.verbId = null;
        this.eventWindow = true;
        this.hasCarrier = carrier != null;
        this.selected = event;
        if (carrier != null) {
            Slot s = new Slot(situationId + ":carrier", "carrier", Map.of(), false);
            s.content = carrier;
            slots.add(s);
        }
        for (SlotDef sd : event.slots) {
            slots.add(new Slot(situationId + ":" + sd.id, sd.id, sd.accepts, sd.required));
        }
    }

    public boolean isRunning() { return running != null; }

    public boolean isSeed(int i) {
        return i == 0 && (!eventWindow || hasCarrier);
    }

    public Slot findSlot(String defId) {
        for (Slot s : slots) {
            if (s.defId.equals(defId)) return s;
        }
        return null;
    }

    public boolean canPlace(int slotIndex, ElementInstance card) {
        if (finished) return false;
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;
        if (card == null || card.locked) return false;

        Slot slot = slots.get(slotIndex);
        if (slot.content != null) return false;

        // 如果事件正在运行中，允许强行往空的强制吸入槽里放牌
        if (isRunning()) {
            if (running.forceDrainAspect != null && slotIndex == 0) {
                return card.aspects().getOrDefault(running.forceDrainAspect, 0) > 0;
            }
            return false;
        }

        if (!eventWindow && slotIndex == 0) return card.def.eventFor(verbId) != null;
        return Aspects.satisfies(card.aspects(), slot.accepts);
    }

    public boolean placeCard(int slotIndex, ElementInstance card) {
        if (!canPlace(slotIndex, card)) return false;
        ElementInstance placed = state.splitOne(card);
        slots.get(slotIndex).content = placed;

        // 如果事件正在运行中，放进去后直接锁定
        if (isRunning()) {
            placed.locked = true;
            return true;
        }

        if (!eventWindow && slotIndex == 0) {
            selected = state.events.require(placed.def.eventFor(verbId));
            for (SlotDef sd : selected.slots) {
                slots.add(new Slot(situationId + ":" + sd.id, sd.id, sd.accepts, sd.required));
            }
            if (selected.autoStart && canStart()) start();
        }
        return true;
    }

    public ElementInstance takeCard(int slotIndex) {
        if (isRunning() || finished) return null;
        if (slotIndex < 0 || slotIndex >= slots.size()) return null;
        Slot slot = slots.get(slotIndex);
        ElementInstance c = slot.content;
        if (c == null) return null;
        slot.content = null;
        if (isSeed(slotIndex)) {
            if (eventWindow) finish();
            else reset();
        }
        return c;
    }

    void cardRemoved(int slotIndex) {
        if (isRunning() || finished) return;
        if (isSeed(slotIndex)) {
            if (eventWindow) finish();
            else reset();
        }
    }

    public void reset() {
        boolean released = releaseCards();
        while (slots.size() > 1) slots.remove(slots.size() - 1);
        selected = null;
        running = null;
        timer = 0f;
        if (released) state.placeNewCards(x, y);
    }

    private void finish() {
        releaseCards();
        slots.clear();
        selected = null;
        running = null;
        timer = 0f;
        finished = true;
        state.placeNewCards(x, y);
    }

    private boolean releaseCards() {
        boolean released = false;
        for (Slot s : slots) {
            if (s.content != null) {
                s.content.locked = false;
                s.content.x = Float.NaN;
                s.content.y = Float.NaN;
                s.content = null;
                released = true;
            }
        }
        return released;
    }

    public Map<String, Integer> totalAspects() {
        Map<String, Integer> total = new HashMap<>();
        for (Slot s : slots) {
            if (s.content == null) continue;
            ElementInstance c = s.content;
            c.aspects().forEach((k, v) -> total.merge(k, v * c.count, Integer::sum));
        }
        return total;
    }

    public boolean canStart() {
        if (isRunning() || finished || selected == null) return false;
        if (!Aspects.satisfies(totalAspects(), selected.input)) return false;
        for (Slot s : slots) {
            if (s.required && s.content == null) return false;
        }
        return true;
    }

    public boolean start() {
        if (!canStart()) return false;
        running = selected;
        timer = running.time;

        // 开始瞬间先吸一次
        tryForceDrain();
        // 再锁定
        for (Slot s : slots) {
            if (s.content != null) s.content.locked = true;
        }
        return true;
    }

    public void update(float delta) {
        if (running == null || finished) return;

        // 【核心修复】运行期间，每一帧都持续尝试吸入
        tryForceDrain();

        timer -= delta;
        if (timer > 0) return;

        EventDef done = running;
        ActionContext ctx = new ActionContext(state, this, done);
        Action.runAll(done.action, ctx);
        state.fireEventCompleted(this, done);
        state.placeNewCards(x, y);

        if (ctx.next != null) {
            running = state.events.require(ctx.next);
            timer = running.time;
        } else if (eventWindow) {
            finish();
        } else {
            reset();
        }
    }

    // 封装强制吸入逻辑，供 start 和 update 调用
    private void tryForceDrain() {
        if (running == null || running.forceDrainAspect == null) return;
        if (slots.isEmpty() || slots.get(0).content != null) return;

        for (ElementInstance c : state.cards) {
            if (!c.locked && !state.isInAnySlot(c) && c.aspects().getOrDefault(running.forceDrainAspect, 0) > 0) {
                slots.get(0).content = state.splitOne(c);
                slots.get(0).content.locked = true;
                break;
            }
        }
    }
}
