package game.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** 静态定义的仓库:卡牌、行动。保持文件里的顺序。 */
public class Library {
    private final Map<String, Element> elements = new LinkedHashMap<>();
    private final Map<String, VerbDef> verbs = new LinkedHashMap<>();

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

    public Collection<Element> elements() {
        return elements.values();
    }

    public void addVerb(VerbDef v) {
        verbs.put(v.id, v);
    }

    public boolean hasVerb(String id) {
        return verbs.containsKey(id);
    }

    public VerbDef verb(String id) {
        VerbDef v = verbs.get(id);
        if (v == null) throw new IllegalArgumentException("未知行动: " + id);
        return v;
    }

    public Collection<VerbDef> verbs() {
        return verbs.values();
    }
}
