package game.core;

import java.util.HashMap;
import java.util.Map;

/** 所有卡牌定义的仓库。 */
public class Library {
    private final Map<String, Element> elements = new HashMap<>();

    public void add(Element e) {
        elements.put(e.id, e);
    }

    public boolean has(String id) {
        return elements.containsKey(id);
    }

    public Element element(String id) {
        Element e = elements.get(id);
        if (e == null) throw new IllegalArgumentException("未知卡牌: " + id);
        return e;
    }
}
