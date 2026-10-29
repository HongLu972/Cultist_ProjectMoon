package game.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 按属性消耗:吃掉来源窗口槽里最多 amount 张带有 target 属性的牌。
 * 优先吃"属性总点数最少"的牌,尽量保住多功能的关键牌。
 */
public class ConsumeAspectEffect implements Effect {
    public String target;
    public int amount = 1;

    public ConsumeAspectEffect() {}

    public ConsumeAspectEffect(String target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(GameState state, Situation source) {
        List<ElementInstance> candidates = new ArrayList<>();
        for (Slot slot : source.slots) {
            ElementInstance c = slot.content;
            if (c != null && c.aspects().getOrDefault(target, 0) > 0) candidates.add(c);
        }
        candidates.sort(Comparator.comparingInt(c ->
                c.aspects().values().stream().mapToInt(Integer::intValue).sum()));
        for (int i = 0; i < Math.min(amount, candidates.size()); i++) {
            state.removeCard(candidates.get(i));
        }
    }
}
