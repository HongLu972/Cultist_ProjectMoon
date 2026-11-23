package com.six.game.core;

import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentLoader {
    private static final Set<String> START_MODES = Set.of("manual", "auto", "linked");

    private final Json json = new Json();

    public ContentLoader() {
        // 多态 Action 用 "type" 字段区分(默认字段名是 "class")。
        json.setTypeName("type");

        // 👇 核心修复：把所有动作统统注册上，一个不漏！
        json.addClassTag("create", Actions.Create.class);
        json.addClassTag("consume", Actions.Consume.class);
        json.addClassTag("drain", Actions.Drain.class);
        json.addClassTag("produce", Actions.Produce.class);
        json.addClassTag("trigger", Actions.TriggerEvent.class);
        json.addClassTag("unlock_verb", Actions.UnlockVerb.class);
        json.addClassTag("start_auto", Actions.StartAuto.class);
        json.addClassTag("end_game", Actions.EndGame.class);
        json.addClassTag("chance", Actions.Chance.class);
        json.addClassTag("check", Actions.Check.class);
        json.addClassTag("play_music", Actions.PlayMusic.class);

        // 👇 之前漏掉的类，现在补全！
        json.addClassTag("modify_aspects", Actions.ModifyAspects.class);
        json.addClassTag("random_create", Actions.RandomCreate.class);
        json.addClassTag("random_do", Actions.RandomDo.class);

        // 泛型类型声明
        json.setElementType(Element.class, "aspects", Integer.class);
        json.setElementType(Element.class, "hiddenAspects", Integer.class);
        json.setElementType(Element.class, "triggers", Element.Trigger.class);
        json.setElementType(SlotDef.class, "accepts", Integer.class);
        json.setElementType(EventDef.class, "input", Integer.class);
        json.setElementType(EventDef.class, "slots", SlotDef.class);
        json.setElementType(EventDef.class, "action", Action.class);
        json.setElementType(EventDef.class, "products", EventDef.Product.class);
        json.setElementType(Action.class, "onSuccess", Action.class);
        json.setElementType(Action.class, "onFail", Action.class);

        json.setElementType(Actions.Check.class, "aspects", Integer.class);
        json.setElementType(Actions.ModifyAspects.class, "aspects", Integer.class); // 补全

        json.setElementType(ScenarioDef.class, "cards", EventDef.Product.class);
        json.setElementType(ScenarioDef.class, "verbs", String.class);
        json.setElementType(ScenarioDef.class, "autoEvents", String.class);

        // 随机动作的泛型设置
        json.setElementType(Actions.RandomCreate.class, "options", Actions.RandomCreate.Option.class);
        json.setElementType(Actions.RandomDo.class, "options", Actions.RandomDo.Option.class);
        json.setElementType(Actions.RandomDo.Option.class, "action", Action.class);
    }

    // ---------------------------------------------------------------- 卡牌、行动、结局

    @SuppressWarnings("unchecked")
    public Library loadLibrary(String text) {
        ArrayList<Element> list = json.fromJson(ArrayList.class, Element.class, text);
        Library lib = new Library();
        for (Element e : list) {
            if (e.id == null || e.id.isEmpty()) throw new IllegalStateException("有卡牌缺少 id");
            if (lib.has(e.id)) System.out.println("[警告] 模组覆盖了已存在的卡牌 id: " + e.id);
            lib.add(e);
        }
        return lib;
    }

    @SuppressWarnings("unchecked")
    public void loadVerbs(String text, Library library) {
        ArrayList<VerbDef> list = json.fromJson(ArrayList.class, VerbDef.class, text);
        for (VerbDef v : list) {
            if (v.id == null || v.id.isEmpty()) throw new IllegalStateException("有行动缺少 id");
            if (library.hasVerb(v.id)) System.out.println("[警告] 模组覆盖了已存在的行动 id: " + v.id);
            if (Element.USE.equals(v.id) || EventDef.NONE.equals(v.id)) throw new IllegalStateException("行动 id 不能用保留字: " + v.id);
            library.addVerb(v);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadEndings(String text, Library library) {
        json.setTypeName(null); // 结局的 type 字段（BE/GE）会和多态冲突
        ArrayList<EndingDef> list;
        try {
            list = json.fromJson(ArrayList.class, EndingDef.class, text);
        } finally {
            json.setTypeName("type");
        }
        for (EndingDef e : list) {
            if (e.id == null || e.id.isEmpty()) throw new IllegalStateException("有结局缺少 id");
            if (library.hasEnding(e.id)) System.out.println("[警告] 模组覆盖了已存在的结局 id: " + e.id);
            library.addEnding(e);
        }
    }

    // ---------------------------------------------------------------- 事件

    @SuppressWarnings("unchecked")
    public EventBook loadEvents(String text, Library library) {
        ArrayList<EventDef> list;
        try {
            list = json.fromJson(ArrayList.class, EventDef.class, text);
        } catch (Exception e) {
            throw new IllegalStateException("解析 events.json 失败！请检查是不是有个动作节点漏写了 \"type\" 字段喵！\n" + e.getMessage(), e);
        }

        EventBook book = new EventBook();
        Set<String> slotIds = new HashSet<>();
        for (VerbDef v : library.verbs()) slotIds.add(v.id);
        slotIds.add("carrier");

        for (EventDef e : list) {
            if (e.id == null || e.id.isEmpty()) throw new IllegalStateException("有事件缺少 id");
            if (book.has(e.id)) System.out.println("[警告] 模组覆盖了已存在的事件 id: " + e.id);
            if (!START_MODES.contains(e.start)) throw new IllegalStateException("事件 " + e.id + " 的 start 必须是 manual / auto / linked");
            if (e.slot == null || e.slot.isEmpty()) throw new IllegalStateException("事件 " + e.id + " 缺少 slot");

            if ("manual".equals(e.start)) {
                if (!EventDef.NONE.equals(e.slot) && !library.hasVerb(e.slot)) throw new IllegalStateException("事件 " + e.id + " 的 slot 不是已知行动");
            } else if (!EventDef.NONE.equals(e.slot)) {
                throw new IllegalStateException("auto / linked 事件的 slot 只能是 none: " + e.id);
            }
            if ("auto".equals(e.start) && e.time <= 0) throw new IllegalStateException("自动事件 " + e.id + " 的 time 必须大于 0");
            if (!"manual".equals(e.start) && !e.slots.isEmpty()) throw new IllegalStateException("只有 manual 事件可以定义 slots: " + e.id);

            for (SlotDef s : e.slots) {
                if (s.id == null || s.id.isEmpty()) throw new IllegalStateException("事件 " + e.id + " 有槽位缺少 id");
                if (!slotIds.add(s.id)) throw new IllegalStateException("重复的槽位 id: " + s.id);
            }
            book.add(e);
        }

        for (EventDef e : list) {
            for (EventDef.Product p : e.products) {
                if (p.card == null || !library.has(p.card)) throw new IllegalStateException("事件 " + e.id + " 的产物引用了未知卡牌: " + p.card);
            }
            validateActions(e, e.action, library, book);
        }
        return book;
    }

    private void validateActions(EventDef owner, List<Action> actions, Library lib, EventBook book) {
        for (Action a : actions) {
            if (a instanceof Actions.Create c) {
                if (c.target == null || !lib.has(c.target)) throw new IllegalStateException("事件 " + owner.id + " 创建了未知卡牌: " + c.target);
            } else if (a instanceof Actions.TriggerEvent t) {
                EventDef target = t.event == null ? null : book.get(t.event);
                if (target == null) throw new IllegalStateException("事件 " + owner.id + " 触发了未知事件: " + t.event);
                boolean ok = "linked".equals(target.start) || ("manual".equals(target.start) && EventDef.NONE.equals(target.slot));
                if (!ok) throw new IllegalStateException("事件 " + owner.id + " 的 trigger 指向了无法直接触发的事件: " + target.id);
            } else if (a instanceof Actions.UnlockVerb u) {
                if (u.verb == null || !lib.hasVerb(u.verb)) throw new IllegalStateException("事件 " + owner.id + " 解锁了未知行动: " + u.verb);
            } else if (a instanceof Actions.StartAuto s) {
                EventDef target = s.event == null ? null : book.get(s.event);
                if (target == null || !"auto".equals(target.start)) throw new IllegalStateException("事件 " + owner.id + " 的 start_auto 必须指向 auto 事件: " + s.event);
            } else if (a instanceof Actions.EndGame g) {
                if (g.ending == null || !lib.hasEnding(g.ending)) throw new IllegalStateException("事件 " + owner.id + " 引用了未知结局: " + g.ending);
            } else if (a instanceof Actions.Produce && owner.products.isEmpty()) {
                throw new IllegalStateException("事件 " + owner.id + " 使用了 produce,但没有定义 products");
            }

            // 校验嵌套动作
            validateActions(owner, a.onSuccess, lib, book);
            validateActions(owner, a.onFail, lib, book);

            // 如果是 RandomDo，还要校验里面的动作
            if (a instanceof Actions.RandomDo rd) {
                for (Actions.RandomDo.Option opt : rd.options) {
                    if (opt.action != null) {
                        List<Action> single = new ArrayList<>();
                        single.add(opt.action);
                        validateActions(owner, single, lib, book);
                    }
                }
            }
        }
    }

    /** 校验卡牌对事件的引用 */
    public void validate(Library lib, EventBook events) {
        for (Element e : lib.elements()) {
            if (e.decayTo != null && !e.decayTo.isEmpty() && !lib.has(e.decayTo)) throw new IllegalStateException("卡牌 " + e.id + " 的 decayTo 引用了未知卡牌");
            for (Element.Trigger t : e.triggers) {
                boolean use = Element.USE.equals(t.slot);
                if (t.slot == null || (!use && !lib.hasVerb(t.slot))) throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 引用了未知行动槽: " + t.slot);
                EventDef ev = t.event == null ? null : events.get(t.event);
                if (ev == null) throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 引用了未知事件: " + t.event);
                if (!"manual".equals(ev.start)) throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 只能指向 manual 事件");
                if (use && !EventDef.NONE.equals(ev.slot)) throw new IllegalStateException("双击使用触发的事件必须是 slot 为 none 的事件");
                if (!use && !t.slot.equals(ev.slot)) throw new IllegalStateException("放置槽与事件占用槽不匹配");
            }
        }
    }

    // ---------------------------------------------------------------- 开局、文字、配置

    @SuppressWarnings("unchecked")
    public ArrayList<ScenarioDef> loadScenarios(String text, Library lib, EventBook events) {
        ArrayList<ScenarioDef> list = json.fromJson(ArrayList.class, ScenarioDef.class, text);

        // 👇 核心修复：使用 LinkedHashMap 去重，保持原有顺序，且后加载的覆盖先加载的
        java.util.LinkedHashMap<String, ScenarioDef> map = new java.util.LinkedHashMap<>();

        for (ScenarioDef s : list) {
            if (s.id == null || s.id.isEmpty()) {
                throw new IllegalStateException("有开局缺少 id");
            }
            if (map.containsKey(s.id)) {
                System.out.println("[警告] 模组覆盖了已存在的开局 id: " + s.id);
            }

            for (EventDef.Product p : s.cards) {
                if (p.card == null || !lib.has(p.card)) {
                    throw new IllegalStateException("开局 " + s.id + " 发了未知卡牌: " + p.card);
                }
            }
            for (String v : s.verbs) {
                if (!lib.hasVerb(v)) {
                    throw new IllegalStateException("开局 " + s.id + " 引用了未知行动: " + v);
                }
            }
            for (String e : s.autoEvents) {
                EventDef ev = events.get(e);
                if (ev == null || !"auto".equals(ev.start)) {
                    throw new IllegalStateException("开局 " + s.id + " 的 autoEvents 必须是 auto 事件: " + e);
                }
            }

            map.put(s.id, s);
        }

        // 最后把去重后的数据转成 List 返回
        return new ArrayList<>(map.values());
    }

    @SuppressWarnings("unchecked")
    public TextBook loadTexts(String text) {
        HashMap<String, String> map = json.fromJson(HashMap.class, String.class, text);
        return new TextBook(map);
    }

    public GameConfig loadConfig(String text) {
        return json.fromJson(GameConfig.class, text);
    }
}
