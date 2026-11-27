package com.six.game.core;

import com.six.game.AudioManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** 所有动作节点类型。在 events.json 里用 "type" 字段指定。 */
public final class Actions {
    private Actions() {}

    /** "create":在桌面上创建 amount 张 target 卡(可堆叠的会并进已有的那摞)。永远成功。 */
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
            ctx.state.addCards(ctx.state.library.element(target), amount);
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
     * "drain":从整个游戏里"吸走"amount 张带有 target 词条(或 id)的、未锁定的牌(堆叠的按张数算)。
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
            long available = 0;
            for (ElementInstance c : ctx.state.cards) {
                if (!c.locked && ActionContext.hasAspect(c, target)) {
                    found.add(c);
                    available += c.count;
                }
            }
            if (available < amount) return false;
            found.sort(Comparator.comparingInt(c -> ctx.state.isInAnySlot(c) ? 1 : 0));
            int left = amount;
            for (ElementInstance c : found) {
                if (left <= 0) break;
                int take = Math.min(left, c.count);
                ctx.state.removeFromStack(c, take);
                left -= take;
            }
            return true;
        }
    }

    /** "produce":发放本事件 products 里列出的所有产物。永远成功。 */
    public static class Produce extends Action {
        @Override
        public boolean execute(ActionContext ctx) {
            for (EventDef.Product p : ctx.event.products) {
                ctx.state.addCards(ctx.state.library.element(p.card), p.amount);
            }
            return true;
        }
    }

    /**
     * "trigger":触发另一个事件。目标事件的类型决定怎么跑:
     *   linked  在窗口里是"接着往下做"(沿用当前槽位,只取第一个 trigger);没有窗口时作为后台事件立刻执行
     *   manual 且 slot 为 none  开一个独立的事件窗口(自动开始,结束后自动关闭)
     * 永远成功。
     */
    public static class TriggerEvent extends Action {
        public String event;

        public TriggerEvent() {}

        public TriggerEvent(String event) {
            this.event = event;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            EventDef target = ctx.state.events.require(event);
            if ("linked".equals(target.start)) {
                if (ctx.situation != null) {
                    if (ctx.next == null) ctx.next = event;
                } else {
                    ctx.state.runBackground(event);
                }
            } else {
                float x = ctx.situation != null ? ctx.situation.x + GameState.WINDOW_SPACING : ctx.state.worldWidth / 2f;
                float y = ctx.situation != null ? ctx.situation.y : ctx.state.worldHeight / 2f - 100f;
                ctx.state.openEventWindow(target, null, x, y);
            }
            return true;
        }
    }

    /** "unlock_verb":解锁一个行动窗口(已经有了就什么都不做)。永远成功。 */
    public static class UnlockVerb extends Action {
        public String verb;

        @Override
        public boolean execute(ActionContext ctx) {
            ctx.state.unlockVerb(verb);
            return true;
        }
    }

    /** "start_auto":启动一个 auto 事件的自动循环(已经在循环就什么都不做)。永远成功。 */
    public static class StartAuto extends Action {
        public String event;

        @Override
        public boolean execute(ActionContext ctx) {
            ctx.state.startAuto(event);
            return true;
        }
    }

    /** "end_game":结束游戏,ending 是 endings.json 里的结局 id。永远成功。 */
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

    /** "modify_aspects": 修改当前槽位里特定卡牌的词条。 */
    public static class ModifyAspects extends Action {
        public String target; // 要修改的卡牌 ID 或词条
        public HashMap<String, Integer> aspects = new HashMap<>(); // 要增加的数值（可以是负数）

        @Override
        public boolean execute(ActionContext ctx) {
            if (ctx.situation == null) return false;
            boolean modified = false;

            for (Slot s : ctx.situation.slots) {
                if (s.content != null) {
                    // 如果 target 为空，或者这张牌带有 target 词条/ID，就开始改造它
                    if (target == null || target.isEmpty() || s.content.aspects().containsKey(target)) {
                        aspects.forEach((k, v) ->
                            s.content.tempAspects.merge(k, v, Integer::sum)
                        );
                        modified = true;
                    }
                }
            }
            return modified;
        }
    }

    /** "play_music": 切换背景音乐。 */
    public static class PlayMusic extends Action {
        public String track;
        @Override
        public boolean execute(ActionContext ctx) {
            AudioManager.getInstance().playBgm(track);
            return true;
        }
    }

    /** "random_create": 按权重随机创造一张卡牌（权重不需要总和为100，引擎自动计算）。 */
    public static class RandomCreate extends Action {
        public ArrayList<Option> options = new ArrayList<>();

        public static class Option {
            public String target;
            public int weight = 1;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            if (options.isEmpty()) return false;

            // 1. 计算总权重
            int totalWeight = 0;
            for (Option o : options) totalWeight += o.weight;
            if (totalWeight <= 0) return false;

            // 2. 掷骰子
            int roll = ctx.state.random.nextInt(totalWeight);

            // 3. 找到命中的目标
            int current = 0;
            for (Option o : options) {
                current += o.weight;
                if (roll < current) {
                    ctx.state.addCards(ctx.state.library.element(o.target), 1);
                    return true;
                }
            }
            return false;
        }
    }
    /** "random_do": 按权重随机执行一段动作代码。 */
    public static class RandomDo extends Action {
        public ArrayList<Option> options = new ArrayList<>();

        public static class Option {
            public int weight = 1;
            public Action action; // 👈 这里可以塞入任何动作！
        }

        @Override
        public boolean execute(ActionContext ctx) {
            if (options.isEmpty()) return false;

            int totalWeight = 0;
            for (Option o : options) totalWeight += o.weight;
            if (totalWeight <= 0) return false;

            int roll = ctx.state.random.nextInt(totalWeight);
            int current = 0;
            for (Option o : options) {
                current += o.weight;
                if (roll < current) {
                    if (o.action != null) {
                        return o.action.execute(ctx); // 执行嵌套的动作
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /** "random_trigger": 按权重随机触发一个事件。 */
    public static class RandomTrigger extends Action {
        public ArrayList<Option> options = new ArrayList<>();

        public static class Option {
            public String event;
            public int weight = 1;
        }

        @Override
        public boolean execute(ActionContext ctx) {
            if (options.isEmpty()) return false;

            int totalWeight = 0;
            for (Option o : options) totalWeight += o.weight;
            if (totalWeight <= 0) return false;

            int roll = ctx.state.random.nextInt(totalWeight);
            int current = 0;
            for (Option o : options) {
                current += o.weight;
                if (roll < current) {
                    if (ctx.situation != null) {
                        if (ctx.next == null) ctx.next = o.event;
                    } else {
                        ctx.state.runBackground(o.event);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * "transform":把最多 amount 张带 target 词条(或 id)的牌变成 to 卡牌。
     * 保留堆叠数/临时词条/位置与槽位锁定;窗口槽优先,桌面未锁定牌次之。
     * amount 填 -1 表示全部变形。一张都找不到则失败。
     */
    public static class Transform extends Action {
        public String target;
        public String to;
        public int amount = 1;

        @Override
        public boolean execute(ActionContext ctx) {
            Element toDef = ctx.state.library.element(to);
            List<ElementInstance> found = new ArrayList<>();
            if (ctx.situation != null) {
                for (Slot s : ctx.situation.slots) {
                    if (s.content != null && ActionContext.hasAspect(s.content, target) && !found.contains(s.content)) {
                        found.add(s.content);
                    }
                }
            }
            for (ElementInstance c : ctx.state.cards) {
                if (c.locked) continue;
                if (ActionContext.hasAspect(c, target) && !found.contains(c)) found.add(c);
            }
            if (found.isEmpty()) return false;
            int n = amount < 0 ? found.size() : Math.min(amount, found.size());
            for (int i = 0; i < n; i++) {
                ctx.state.transformCard(found.get(i), toDef);
            }
            return true;
        }
    }

    /** "record":往编年史里写一条记录(报幕字幕素材)。永远成功。 */
    public static class Record extends Action {
        public String text;

        @Override
        public boolean execute(ActionContext ctx) {
            ctx.state.chronicle.addRecord(ctx.state.totalTime, text == null ? "" : text);
            return true;
        }
    }

    /** "set_flag":设置全局剧情旗标(随存档保存)。永远成功。 */
    public static class SetFlag extends Action {
        public String flag;
        public int value = 1;

        @Override
        public boolean execute(ActionContext ctx) {
            ctx.state.storyFlags.put(flag, value);
            return true;
        }
    }

    /** "check_flag":检查全局剧情旗标是否 &gt;= value。 */
    public static class CheckFlag extends Action {
        public String flag;
        public int value = 1;

        @Override
        public boolean execute(ActionContext ctx) {
            return ctx.state.storyFlags.getOrDefault(flag, 0) >= value;
        }
    }

    /** "check_count":检查场上某卡牌(按 id,含槽内,按堆叠计数)总数是否 &gt;= amount。 */
    public static class CheckCount extends Action {
        public String target;
        public int amount = 1;

        @Override
        public boolean execute(ActionContext ctx) {
            return ctx.state.count(target) >= amount;
        }
    }

    /**
     * "destroy":摧毁桌面上带 target 词条(或 id)的未锁定散牌 amount 张(-1=全部,按堆叠计数)。
     * 行动窗/事件槽里的牌不受影响。一张都没摧到(或不够数)则失败。
     */
    public static class Destroy extends Action {
        public String target;
        public int amount = 1;

        @Override
        public boolean execute(ActionContext ctx) {
            int destroyed = ctx.state.destroyCards(target, amount);
            return amount < 0 ? destroyed > 0 : destroyed >= amount;
        }
    }

    /** "stop_event":停止指定事件的一切运行(后台循环、延时触发、正在跑的窗口)。停到了就算成功。 */
    public static class StopEvent extends Action {
        public String event;

        @Override
        public boolean execute(ActionContext ctx) {
            return ctx.state.stopEvent(event) > 0;
        }
    }
}
