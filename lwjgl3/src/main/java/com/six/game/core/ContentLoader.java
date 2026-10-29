package game.core;

import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把 JSON 文本读成游戏内容。只接收 String,不依赖 Gdx.files,所以能直接在 JUnit 里测。
 * 加载顺序:
 *   loadLibrary(卡牌) -> loadVerbs(行动) -> loadEndings(结局) -> loadEvents(事件,会校验动作树里的引用)
 *   -> validate(校验卡牌的 trigger / decayTo) -> loadScenarios(开局) -> loadTexts(文字) / loadConfig(配置)
 */
public class ContentLoader {
    private static final Set<String> START_MODES = Set.of("manual", "auto", "linked");

    private final Json json = new Json();

    public ContentLoader() {
        // 多态 Action 用 "type" 字段区分(默认字段名是 "class")。
        json.setTypeName("type");
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

        // 多数情况下 LibGDX 能从泛型读出元素类型,显式声明一遍更保险。
        json.setElementType(Element.class, "aspects", Integer.class);
        json.setElementType(Element.class, "triggers", Element.Trigger.class);
        json.setElementType(SlotDef.class, "accepts", Integer.class);
        json.setElementType(EventDef.class, "input", Integer.class);
        json.setElementType(EventDef.class, "slots", SlotDef.class);
        json.setElementType(EventDef.class, "action", Action.class);
        json.setElementType(EventDef.class, "products", EventDef.Product.class);
        json.setElementType(Action.class, "onSuccess", Action.class);
        json.setElementType(Action.class, "onFail", Action.class);
        json.setElementType(Actions.Check.class, "aspects", Integer.class);
        json.setElementType(ScenarioDef.class, "cards", EventDef.Product.class);
        json.setElementType(ScenarioDef.class, "verbs", String.class);
        json.setElementType(ScenarioDef.class, "autoEvents", String.class);
        // 注意:没有开 setIgnoreUnknownFields,字段名拼错会直接抛 SerializationException,这是好事。
    }

    // ---------------------------------------------------------------- 卡牌、行动、结局

    @SuppressWarnings("unchecked")
    public Library loadLibrary(String text) {
        ArrayList<Element> list = json.fromJson(ArrayList.class, Element.class, text);
        Library lib = new Library();
        for (Element e : list) {
            if (e.id == null || e.id.isEmpty()) {
                throw new IllegalStateException("有卡牌缺少 id");
            }
            if (lib.has(e.id)) {
                throw new IllegalStateException("重复的卡牌 id: " + e.id);
            }
            lib.add(e);
        }
        return lib;
    }

    @SuppressWarnings("unchecked")
    public void loadVerbs(String text, Library library) {
        ArrayList<VerbDef> list = json.fromJson(ArrayList.class, VerbDef.class, text);
        for (VerbDef v : list) {
            if (v.id == null || v.id.isEmpty()) {
                throw new IllegalStateException("有行动缺少 id");
            }
            if (library.hasVerb(v.id)) {
                throw new IllegalStateException("重复的行动 id: " + v.id);
            }
            if (Element.USE.equals(v.id) || EventDef.NONE.equals(v.id)) {
                throw new IllegalStateException("行动 id 不能用保留字: " + v.id);
            }
            library.addVerb(v);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadEndings(String text, Library library) {
        // EndingDef 有一个业务字段就叫 "type"（BE/GE…），会和 LibGDX 的多态类型标签冲突。
        // 反序列化完再恢复，因为 loadEvents 还要用它。
        json.setTypeName(null);
        ArrayList<EndingDef> list;
        try {
            list = json.fromJson(ArrayList.class, EndingDef.class, text);
        } finally {
            json.setTypeName("type");
        }
        for (EndingDef e : list) {
            if (e.id == null || e.id.isEmpty()) {
                throw new IllegalStateException("有结局缺少 id");
            }
            if (library.hasEnding(e.id)) {
                throw new IllegalStateException("重复的结局 id: " + e.id);
            }
            library.addEnding(e);
        }
    }

    // ---------------------------------------------------------------- 事件

    @SuppressWarnings("unchecked")
    public EventBook loadEvents(String text, Library library) {
        ArrayList<EventDef> list = json.fromJson(ArrayList.class, EventDef.class, text);
        EventBook book = new EventBook();

        // 槽位 id 全局唯一,且不能和行动 id(起始槽的 id)或保留字重复
        Set<String> slotIds = new HashSet<>();
        for (VerbDef v : library.verbs()) slotIds.add(v.id);
        slotIds.add("carrier");

        for (EventDef e : list) {
            if (e.id == null || e.id.isEmpty()) {
                throw new IllegalStateException("有事件缺少 id");
            }
            if (book.has(e.id)) {
                throw new IllegalStateException("重复的事件 id: " + e.id);
            }
            if (!START_MODES.contains(e.start)) {
                throw new IllegalStateException("事件 " + e.id + " 的 start 必须是 manual / auto / linked,而不是: " + e.start);
            }
            if (e.slot == null || e.slot.isEmpty()) {
                throw new IllegalStateException("事件 " + e.id + " 缺少 slot(行动 id,或 none 表示不占用行动槽)");
            }
            if ("manual".equals(e.start)) {
                if (!EventDef.NONE.equals(e.slot) && !library.hasVerb(e.slot)) {
                    throw new IllegalStateException("事件 " + e.id + " 的 slot 不是已知行动,也不是 none: " + e.slot);
                }
            } else if (!EventDef.NONE.equals(e.slot)) {
                throw new IllegalStateException("auto / linked 事件的 slot 只能是 none: " + e.id);
            }
            if ("auto".equals(e.start) && e.time <= 0) {
                throw new IllegalStateException("自动事件 " + e.id + " 的 time 必须大于 0,否则会无限循环");
            }
            if (!"manual".equals(e.start) && !e.slots.isEmpty()) {
                throw new IllegalStateException("只有 manual 事件可以定义 slots: " + e.id);
            }
            for (SlotDef s : e.slots) {
                if (s.id == null || s.id.isEmpty()) {
                    throw new IllegalStateException("事件 " + e.id + " 有槽位缺少 id");
                }
                if (!slotIds.add(s.id)) {
                    throw new IllegalStateException("重复的槽位 id: " + s.id + "(槽位 id 必须全局唯一,且不能与行动 id 相同)");
                }
            }
            book.add(e);
        }

        // 所有事件都进了 book 之后再校验引用,这样事件的先后顺序不影响结果。
        for (EventDef e : list) {
            for (EventDef.Product p : e.products) {
                if (p.card == null || !library.has(p.card)) {
                    throw new IllegalStateException("事件 " + e.id + " 的产物引用了未知卡牌: " + p.card);
                }
            }
            validateActions(e, e.action, library, book);
        }
        return book;
    }

    private void validateActions(EventDef owner, List<Action> actions, Library lib, EventBook book) {
        for (Action a : actions) {
            if (a instanceof Actions.Create c) {
                if (c.target == null || !lib.has(c.target)) {
                    throw new IllegalStateException("事件 " + owner.id + " 创建了未知卡牌: " + c.target);
                }
            } else if (a instanceof Actions.TriggerEvent t) {
                EventDef target = t.event == null ? null : book.get(t.event);
                if (target == null) {
                    throw new IllegalStateException("事件 " + owner.id + " 触发了未知事件: " + t.event);
                }
                boolean ok = "linked".equals(target.start)
                        || ("manual".equals(target.start) && EventDef.NONE.equals(target.slot));
                if (!ok) {
                    throw new IllegalStateException("事件 " + owner.id + " 的 trigger 只能指向 linked 事件,或 slot 为 none 的 manual 事件: " + target.id);
                }
            } else if (a instanceof Actions.UnlockVerb u) {
                if (u.verb == null || !lib.hasVerb(u.verb)) {
                    throw new IllegalStateException("事件 " + owner.id + " 解锁了未知行动: " + u.verb);
                }
            } else if (a instanceof Actions.StartAuto s) {
                EventDef target = s.event == null ? null : book.get(s.event);
                if (target == null || !"auto".equals(target.start)) {
                    throw new IllegalStateException("事件 " + owner.id + " 的 start_auto 必须指向 auto 事件: " + s.event);
                }
            } else if (a instanceof Actions.EndGame g) {
                if (g.ending == null || !lib.hasEnding(g.ending)) {
                    throw new IllegalStateException("事件 " + owner.id + " 引用了未知结局: " + g.ending);
                }
            } else if (a instanceof Actions.Produce && owner.products.isEmpty()) {
                throw new IllegalStateException("事件 " + owner.id + " 使用了 produce,但没有定义 products");
            }
            validateActions(owner, a.onSuccess, lib, book);
            validateActions(owner, a.onFail, lib, book);
        }
    }

    /** 校验卡牌对事件的引用:trigger 的行动槽/事件、decayTo。 */
    public void validate(Library lib, EventBook events) {
        for (Element e : lib.elements()) {
            if (e.decayTo != null && !e.decayTo.isEmpty() && !lib.has(e.decayTo)) {
                throw new IllegalStateException("卡牌 " + e.id + " 的 decayTo 引用了未知卡牌: " + e.decayTo);
            }
            for (Element.Trigger t : e.triggers) {
                boolean use = Element.USE.equals(t.slot);
                if (t.slot == null || (!use && !lib.hasVerb(t.slot))) {
                    throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 引用了未知的行动槽: " + t.slot + "(可以是行动 id 或 use)");
                }
                EventDef ev = t.event == null ? null : events.get(t.event);
                if (ev == null) {
                    throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 引用了未知事件: " + t.event);
                }
                if (!"manual".equals(ev.start)) {
                    throw new IllegalStateException("卡牌 " + e.id + " 的 trigger 只能指向 manual 事件,但 " + ev.id + " 是 " + ev.start);
                }
                if (use && !EventDef.NONE.equals(ev.slot)) {
                    throw new IllegalStateException("卡牌 " + e.id + " 双击使用触发的事件 " + ev.id + " 必须是 slot 为 none 的事件");
                }
                if (!use && !t.slot.equals(ev.slot)) {
                    throw new IllegalStateException("卡牌 " + e.id + " 放在行动槽 " + t.slot + " 触发的事件 " + ev.id
                            + " 必须占用同一个行动槽(它的 slot 是 " + ev.slot + ")");
                }
            }
        }
    }

    // ---------------------------------------------------------------- 开局、文字、配置

    @SuppressWarnings("unchecked")
    public ArrayList<ScenarioDef> loadScenarios(String text, Library lib, EventBook events) {
        ArrayList<ScenarioDef> list = json.fromJson(ArrayList.class, ScenarioDef.class, text);
        Set<String> ids = new HashSet<>();
        for (ScenarioDef s : list) {
            if (s.id == null || s.id.isEmpty()) {
                throw new IllegalStateException("有开局缺少 id");
            }
            if (!ids.add(s.id)) {
                throw new IllegalStateException("重复的开局 id: " + s.id);
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
        }
        return list;
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
