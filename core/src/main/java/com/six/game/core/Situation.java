package game.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 一个行动窗口(工作、学习、梦境……)。UI 只应通过 placeCard / takeCard / start 操作它。 */
public class Situation {
    public final String situationId;
    public final String verbId;
    public final List<Slot> slots = new ArrayList<>();
    public Recipe running;
    public float timer;

    Situation(String situationId, String verbId, int slotCount) {
        this.situationId = situationId;
        this.verbId = verbId;
        for (int i = 0; i < slotCount; i++) {
            slots.add(new Slot(situationId + "_slot" + i));
        }
    }

    public boolean isRunning() {
        return running != null;
    }

    /** 把牌放进槽。运行中、槽已占用、牌被锁定时拒绝。 */
    public boolean placeCard(int slotIndex, ElementInstance card) {
        if (isRunning()) return false;
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;
        if (card == null || card.locked) return false;
        Slot slot = slots.get(slotIndex);
        if (slot.content != null) return false;
        slot.content = card;
        return true;
    }

    /** 把牌从槽里取走。运行中不允许。 */
    public ElementInstance takeCard(int slotIndex) {
        if (isRunning()) return null;
        if (slotIndex < 0 || slotIndex >= slots.size()) return null;
        Slot slot = slots.get(slotIndex);
        ElementInstance c = slot.content;
        slot.content = null;
        return c;
    }

    public Map<String, Integer> totalAspects() {
        Map<String, Integer> total = new HashMap<>();
        for (Slot s : slots) {
            if (s.content == null) continue;
            s.content.aspects().forEach((k, v) -> total.merge(k, v, Integer::sum));
        }
        return total;
    }

    /** 玩家点"开始"。已在运行、或没有匹配配方时返回 false。 */
    public boolean start(RecipeBook book) {
        if (isRunning()) return false;
        Recipe r = book.match(verbId, totalAspects());
        if (r == null) return false;
        running = r;
        timer = r.duration;
        for (Slot s : slots) {
            if (s.content != null) s.content.locked = true;
        }
        return true;
    }

    /** delta 已经乘过 timeScale。 */
    public void update(float delta, GameState state) {
        if (running == null) return;
        timer -= delta;
        if (timer > 0) return;

        Recipe done = running;
        running = null;
        for (Effect e : done.effects) e.apply(state, this);

        Recipe next = chooseNext(done, state);
        if (next != null) {
            // 链式推进:牌继续保持锁定
            running = next;
            timer = next.duration;
        } else {
            for (Slot s : slots) {
                if (s.content != null) s.content.locked = false;
            }
        }
    }

    /** 按顺序检查 linked:条件满足且概率命中的第一个胜出。注意条件看的是"结算之后"的槽。 */
    private Recipe chooseNext(Recipe done, GameState state) {
        if (done.linked.isEmpty()) return null;
        Map<String, Integer> have = totalAspects();
        for (Recipe.Link link : done.linked) {
            if (!RecipeBook.satisfies(have, link.requires)) continue;
            if (state.random.nextInt(100) >= link.chance) continue;
            Recipe next = state.book.get(link.id);
            if (next == null) {
                throw new IllegalStateException("配方 " + done.id + " 链接到不存在的配方: " + link.id);
            }
            return next;
        }
        return null;
    }
}
