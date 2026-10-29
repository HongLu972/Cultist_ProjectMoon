package game.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个行动窗口(作业、研究、出门……)。
 * 状态:空闲(只有起始槽) -> 已选定事件(起始槽放了有触发器的牌,出现额外槽) -> 进行中 -> 结算后回到空闲。
 * UI 只应通过 canPlace / placeCard / takeCard / start 操作它。
 */
public class Situation {
    public final String situationId;
    public final String verbId;
    /** slots.get(0) 永远是起始槽。 */
    public final List<Slot> slots = new ArrayList<>();
    /** 起始牌触发的事件;空闲时为 null。 */
    public EventDef selected;
    /** 正在计时的事件(链式后续事件会替换它);没在运行时为 null。 */
    public EventDef running;
    public float timer;

    private final GameState state;

    Situation(GameState state, String situationId, VerbDef verb) {
        this.state = state;
        this.situationId = situationId;
        this.verbId = verb.id;
        slots.add(new Slot(situationId + ":" + verb.id, verb.id, Map.of(), false));
    }

    public boolean isRunning() {
        return running != null;
    }

    public Slot findSlot(String defId) {
        for (Slot s : slots) {
            if (s.defId.equals(defId)) return s;
        }
        return null;
    }

    /**
     * 能不能放:没在运行、槽空着、牌没锁定;
     * 起始槽要求这张牌对本行动有触发器,其他槽要求牌满足槽的 accepts。
     */
    public boolean canPlace(int slotIndex, ElementInstance card) {
        if (isRunning()) return false;
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;
        if (card == null || card.locked) return false;
        Slot slot = slots.get(slotIndex);
        if (slot.content != null) return false;
        if (slotIndex == 0) return card.def.eventFor(verbId) != null;
        return Aspects.satisfies(card.aspects(), slot.accepts);
    }

    public boolean placeCard(int slotIndex, ElementInstance card) {
        if (!canPlace(slotIndex, card)) return false;
        slots.get(slotIndex).content = card;
        if (slotIndex == 0) {
            select(state.events.require(card.def.eventFor(verbId)));
        }
        return true;
    }

    private void select(EventDef e) {
        selected = e;
        for (SlotDef sd : e.slots) {
            slots.add(new Slot(situationId + ":" + sd.id, sd.id, sd.accepts, sd.required));
        }
    }

    /** 把牌从槽里取走。运行中不允许。取走起始牌会让整个窗口回到空闲。 */
    public ElementInstance takeCard(int slotIndex) {
        if (isRunning()) return null;
        if (slotIndex < 0 || slotIndex >= slots.size()) return null;
        Slot slot = slots.get(slotIndex);
        ElementInstance c = slot.content;
        if (c == null) return null;
        slot.content = null;
        if (slotIndex == 0) reset();
        return c;
    }

    /** 回到空闲:所有槽里的牌回到桌面(解锁),额外槽消失,只保留起始槽。 */
    public void reset() {
        for (Slot s : slots) {
            if (s.content != null) {
                s.content.locked = false;
                s.content = null;
            }
        }
        while (slots.size() > 1) slots.remove(slots.size() - 1);
        selected = null;
        running = null;
        timer = 0f;
    }

    public Map<String, Integer> totalAspects() {
        Map<String, Integer> total = new HashMap<>();
        for (Slot s : slots) {
            if (s.content == null) continue;
            s.content.aspects().forEach((k, v) -> total.merge(k, v, Integer::sum));
        }
        return total;
    }

    /** 已选定事件、词条总量够、必填槽都放了牌。 */
    public boolean canStart() {
        if (isRunning() || selected == null) return false;
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
        for (Slot s : slots) {
            if (s.content != null) s.content.locked = true;
        }
        return true;
    }

    /** delta 已经乘过倍速。 */
    public void update(float delta) {
        if (running == null) return;
        timer -= delta;
        if (timer > 0) return;

        EventDef done = running;
        ActionContext ctx = new ActionContext(state, this, done);
        Action.runAll(done.action, ctx);
        state.fireEventCompleted(this, done);

        if (ctx.next != null) {
            // 链式推进:牌继续保持锁定,沿用当前槽位
            running = state.events.require(ctx.next);
            timer = running.time;
        } else {
            reset();
        }
    }
}
