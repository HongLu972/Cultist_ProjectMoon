package game.core;

import java.util.HashMap;
import java.util.Map;

/** 一次动作树执行时的上下文。 */
public class ActionContext {
    public final GameState state;
    /** 在哪个窗口里执行;自动事件(没有窗口)时为 null。 */
    public final Situation situation;
    /** 正在执行的事件。 */
    public final EventDef event;
    /** 窗口内被 trigger 的后续事件 id。只取第一个。 */
    public String next;

    public ActionContext(GameState state, Situation situation, EventDef event) {
        this.state = state;
        this.situation = situation;
        this.event = event;
    }

    /** 有窗口时是窗口槽里的词条总量;没有窗口时是桌面上所有未锁定的牌(含堆叠数量)的词条总量。 */
    public Map<String, Integer> aspects() {
        if (situation != null) return situation.totalAspects();
        Map<String, Integer> total = new HashMap<>();
        for (ElementInstance c : state.cards) {
            if (c.locked) continue;
            c.aspects().forEach((k, v) -> total.merge(k, v * c.count, Integer::sum));
        }
        return total;
    }

    static boolean hasAspect(ElementInstance c, String target) {
        return c.aspects().getOrDefault(target, 0) > 0;
    }
}
