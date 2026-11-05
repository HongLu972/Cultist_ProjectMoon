package com.six.game.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    public interface Listener {
        void eventCompleted(Situation situation, EventDef event);
    }

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

    public static final float WINDOW_SPACING = 560f;
    private static final float CARD_STEP_X = 112f, CARD_STEP_Y = 150f;

    public final Library library;
    public final EventBook events;
    public final List<ElementInstance> cards = new ArrayList<>();
    public final List<Situation> situations = new ArrayList<>();
    public final List<BackgroundRun> backgrounds = new ArrayList<>();
    public Random random = new Random();
    public String ending;
    public float worldWidth = 3200f, worldHeight = 1800f;

    // 编年史与时间
    public Chronicle chronicle = new Chronicle();
    public float totalTime = 0f;

    private final List<Listener> listeners = new ArrayList<>();
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

    public void begin(ScenarioDef sc) {
        for (String v : sc.verbs) unlockVerb(v);
        for (EventDef.Product p : sc.cards) addCards(library.element(p.card), p.amount);
        for (String e : sc.autoEvents) startAuto(e);
        placeNewCards(worldWidth / 2f - 200f, worldHeight / 2f - 100f);
    }

    // ---------------------------------------------------------------- 存档与读档
    public SaveData createSaveData() {
        SaveData data = new SaveData();
        data.chronicle = this.chronicle;
        data.chronicle.totalPlayTime = this.totalTime;
        data.ending = this.ending;

        // 1. 给所有卡牌分配存档 ID，并保存
        int cardIdx = 0;
        for (ElementInstance c : cards) {
            SaveData.CardData cd = new SaveData.CardData();
            cd.saveId = "card_" + (cardIdx++);
            c.tempSaveId = cd.saveId; // 临时挂载到实例上，方便槽位查找
            cd.defId = c.def.id;
            cd.count = c.count;
            cd.x = c.x;
            cd.y = c.y;
            cd.locked = c.locked;
            cd.remaining = c.remaining;
            data.cards.add(cd);
        }

        // 2. 保存所有窗口及其槽位状态
        for (Situation s : situations) {
            SaveData.SituationData sd = new SaveData.SituationData();
            sd.situationId = s.situationId;
            sd.verbId = s.verbId;
            sd.eventId = s.eventWindow && s.selected != null ? s.selected.id : null;
            sd.x = s.x;
            sd.y = s.y;
            sd.timer = s.timer;
            sd.runningEventId = s.running != null ? s.running.id : null;
            sd.selectedEventId = s.selected != null ? s.selected.id : null;

            for (Slot slot : s.slots) {
                if (slot.content != null) {
                    sd.slotSaveIds.add(slot.content.tempSaveId);
                } else {
                    sd.slotSaveIds.add("null");
                }
                sd.slotDefIds.add(slot.defId); // 👈 顺便保存真实槽位 ID
            }
            data.situations.add(sd);
        }
        return data;
    }

    public void loadFromSave(SaveData data) {
        this.cards.clear();
        this.situations.clear();
        this.backgrounds.clear();
        this.totalTime = data.chronicle.totalPlayTime;
        this.chronicle = data.chronicle;
        this.ending = data.ending;

        java.util.Map<String, ElementInstance> saveIdMap = new java.util.HashMap<>();

        // 1. 先恢复所有卡牌
        for (SaveData.CardData cd : data.cards) {
            ElementInstance c = createCardDirect(library.element(cd.defId));
            c.count = cd.count;
            c.x = cd.x;
            c.y = cd.y;
            c.locked = cd.locked;
            c.remaining = cd.remaining;
            c.tempSaveId = cd.saveId;
            saveIdMap.put(cd.saveId, c);
        }

        // 2. 恢复窗口（彻底剥离自动启动逻辑）
        for (SaveData.SituationData sd : data.situations) {
            Situation s = null;
            if (sd.eventId != null && events.has(sd.eventId)) {
                // 事件窗口：直接用构造函数，避免触发 autoStart
                s = new Situation(this, sd.situationId, events.get(sd.eventId), null);
                s.x = sd.x;
                s.y = sd.y;
            } else if (sd.verbId != null && library.hasVerb(sd.verbId)) {
                // 行动窗口：直接用构造函数
                s = new Situation(this, sd.situationId, library.verb(sd.verbId));
                s.x = sd.x;
                s.y = sd.y;
            }
            if (s == null) continue;

            // 【核心修复】清空默认槽位，用存下来的真实 ID 重建！
            s.slots.clear();
            for (int i = 0; i < sd.slotSaveIds.size(); i++) {
                // 从存档里读出真实的 defId（兼容没有这个字段的旧存档）
                String defId = (sd.slotDefIds != null && i < sd.slotDefIds.size()) ? sd.slotDefIds.get(i) : "restored_" + i;
                Slot newSlot = new Slot(s.situationId + ":" + defId, defId, new java.util.HashMap<>(), false);
                s.slots.add(newSlot);
            }

            // 【核心修复 2】把卡牌安全地塞进去，并抹除坐标
            for (int i = 0; i < sd.slotSaveIds.size(); i++) {
                String saveId = sd.slotSaveIds.get(i);
                if (!saveId.equals("null") && saveIdMap.containsKey(saveId)) {
                    ElementInstance c = saveIdMap.get(saveId);
                    s.slots.get(i).content = c;
                    c.locked = true; // 槽里的牌必定是锁定状态
                    c.x = Float.NaN; // 👈 抹除坐标，防止“飞雷神”到桌面
                    c.y = Float.NaN;
                }
            }

            // 恢复运行状态
            if (sd.selectedEventId != null && events.has(sd.selectedEventId)) {
                s.selected = events.get(sd.selectedEventId);
            }
            if (sd.runningEventId != null && events.has(sd.runningEventId)) {
                s.running = events.get(sd.runningEventId);
            }
            s.timer = sd.timer;

            situations.add(s);
        }
    }

    // ---------------------------------------------------------------- 窗口与卡牌（保持你原有逻辑）
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

    public Situation openEventWindow(EventDef e, ElementInstance carrier, float x, float y) {
        // 智能防重叠：如果这个位置已经有窗口了，就围着它转圈找空位
        float finalX = x;
        float finalY = y;
        int attempts = 0;
        while (attempts < 12) {
            boolean occupied = false;
            for (Situation existing : situations) {
                if (Math.abs(existing.x - finalX) < 200 && Math.abs(existing.y - finalY) < 200) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) break;

            // 如果被占了，就沿着圆周找下一个位置
            double angle = Math.toRadians(attempts * 30);
            finalX = x + (float) (Math.cos(angle) * 300);
            finalY = y + (float) (Math.sin(angle) * 300);
            attempts++;
        }

        Situation s = new Situation(this, "ev_" + e.id + "_" + (nextSituationId++), e, carrier);
        // 限制在世界地图边界内
        s.x = clamp(finalX, 10f, worldWidth - 520f);
        s.y = clamp(finalY, 10f, worldHeight - 360f);
        situations.add(s);
        boolean nothingToWaitFor = e.input.isEmpty() && e.slots.isEmpty();
        if ((e.autoStart || nothingToWaitFor) && s.canStart()) s.start();
        return s;
    }

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

    public ElementInstance createCardDirect(Element def) {
        ElementInstance c = new ElementInstance(def, def.id + "#" + (nextCardId++));
        cards.add(c);
        return c;
    }

    public ElementInstance addCard(Element def) {
        return addCards(def, 1);
    }

    private ElementInstance newCard(Element def) {
        ElementInstance c = new ElementInstance(def, def.id + "#" + (nextCardId++));
        cards.add(c);
        return c;
    }

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

    public void removeFromStack(ElementInstance c, int n) {
        c.count -= n;
        if (c.count <= 0) removeCard(c);
    }

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

    public void mergeInto(ElementInstance src, ElementInstance dst) {
        if (!canMerge(src, dst)) return;
        dst.count += src.count;
        if (dst.def.lifetime > 0) dst.remaining = Math.min(dst.remaining, src.remaining);
        removeCard(src);
    }

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

    public long count(String elementId) {
        long total = 0;
        for (ElementInstance c : cards) {
            if (c.def.id.equals(elementId)) total += c.count;
        }
        return total;
    }

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

    public void startAuto(String eventId) {
        EventDef e = events.require(eventId);
        for (BackgroundRun r : backgrounds) {
            if (r.event == e && r.repeat) return;
        }
        backgrounds.add(new BackgroundRun(e, e.time, true));
    }

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

    public void tick(float dt) {
        if (ending != null) return;
        totalTime += dt;

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
