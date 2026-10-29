package game.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventBook {
    private final Map<String, EventDef> byId = new LinkedHashMap<>();

    public void add(EventDef e) {
        byId.put(e.id, e);
    }

    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public EventDef get(String id) {
        return byId.get(id);
    }

    public EventDef require(String id) {
        EventDef e = byId.get(id);
        if (e == null) throw new IllegalStateException("未知事件: " + id);
        return e;
    }

    public Collection<EventDef> all() {
        return byId.values();
    }
}
