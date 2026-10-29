package game.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** 所有动作节点类型。在 events.json 里用 "type" 字段指定。 */
public final class Actions {
    private Actions() {}

    /** "create":在桌面上创建 amount 张 target 卡。永远成功。 */
    public static class Create extends Action {
        public String target;
        public int amount = 1;

        public Create() {}

        public Create(String target, int amount) {
            this.target = target;
            this.amount = amount;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            Element def = ctx.state.library.element(target);
            for (int i = 0; i < amount; i++) ctx.state.addCard(def);
            return true;
        }
    }

    /**
     * "consume":从本窗口的槽里吃掉 amount 张带有 target 词条(或 id)的牌。
     * 槽里数量不够则一张都不吃并返回失败。优先吃词条总数最少的牌,尽量保住多功能牌。
     */
    public static class Consume extends Action {
        public String target;
        public int amount = 1;

        public Consume() {}

        public Consume(String target, int amount) {
            this.target = target;
            this.amount = amount;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            if (ctx.situation == null) return false;
            List<ElementInstance> found = new ArrayList<>();
            for (Slot s : ctx.situation.slots) {
                if (s.content != null && ActionContext.hasAspect(s.content, target)) found.add(s.content);
            }
            if (found.size() < amount) return false;
            found.sort(Comparator.comparingInt(c -> Aspects.total(c.aspects())));
            for (int i = 0; i < amount; i++) ctx.state.removeCard(found.get(i));
            return true;
        }
    }

    /**
     * "drain":从整个游戏里"吸走"amount 张带有 target 词条(或 id)的、未锁定的牌。
     * 数量不够则一张都不吸并返回失败。优先吸桌面上的,其次才是窗口槽里的。
     */
    public static class Drain extends Action {
        public String target;
        public int amount = 1;

        public Drain() {}

        public Drain(String target, int amount) {
            this.target = target;
            this.amount = amount;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            List<ElementInstance> found = new ArrayList<>();
            for (ElementInstance c : ctx.state.cards) {
                if (!c.locked && ActionContext.hasAspect(c, target)) found.add(c);
            }
            if (found.size() < amount) return false;
            found.sort(Comparator.comparingInt(c -> ctx.state.isInAnySlot(c) ? 1 : 0));
            for (int i = 0; i < amount; i++) ctx.state.removeCard(found.get(i));
            return true;
        }
    }

    /** "produce":发放本事件 products 里列出的所有产物。永远成功。 */
    public static class Produce extends Action {
        @Override
        public boolean execute(ActionContext ctx) {
            for (EventDef.Product p : ctx.event.products) {
                Element def = ctx.state.library.element(p.card);
                for (int i = 0; i < p.amount; i++) ctx.state.addCard(def);
            }
            return true;
        }
    }

    /**
     * "trigger":触发另一个事件。在窗口里是"接着往下做"(沿用当前槽位,只取第一个 trigger);
     * 在自动事件里则作为后台事件立刻(time 为 0)或稍后执行。永远成功。
     */
    public static class TriggerEvent extends Action {
        public String event;

        public TriggerEvent() {}

        public TriggerEvent(String event) {
            this.event = event;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            if (ctx.situation != null) {
                if (ctx.next == null) ctx.next = event;
            } else {
                ctx.state.runBackground(event);
            }
            return true;
        }
    }

    /** "end_game":结束游戏,ending 是结局 id(文字见 ending.&lt;id&gt;.name / .desc)。永远成功。 */
    public static class EndGame extends Action {
        public String ending;

        @Override
        public boolean execute(ActionContext ctx) {
            if (ctx.state.ending == null) ctx.state.ending = ending;
            return true;
        }
    }

    /** "chance":按 percent(0~100)的概率成功。 */
    public static class Chance extends Action {
        public int percent;

        @Override
        public boolean execute(ActionContext ctx) {
            return ctx.state.random.nextInt(100) < percent;
        }
    }

    /** "check":检查词条总量(有窗口看窗口槽里,没有窗口看桌面)是否满足 aspects。 */
    public static class Check extends Action {
        public HashMap<String, Integer> aspects = new HashMap<>();

        @Override
        public boolean execute(ActionContext ctx) {
            return Aspects.satisfies(ctx.aspects(), aspects);
        }
    }
}
