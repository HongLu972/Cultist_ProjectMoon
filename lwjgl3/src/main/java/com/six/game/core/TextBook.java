package game.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 界面文字仓库:key -> 文字。游戏数据里不写任何展示文字,全部放在 data/text_&lt;语言&gt;.json。
 * 约定的 key:
 *   element.&lt;id&gt;.name / .desc       卡牌
 *   slot.&lt;id&gt;.name / .desc          槽位(起始槽的 id 就是行动 id)
 *   event.&lt;id&gt;.name / .desc / .result  事件(.result 非空时结算会写进记录)
 *   aspect.&lt;id&gt;.name                词条
 *   ending.&lt;id&gt;.name / .desc         结局
 *   ui.*                            界面固定文字
 * 可以设置备用语言:当前语言缺 key 时回退到备用语言,再缺才显示 [key]。
 */
public class TextBook {
    private final Map<String, String> map;
    private final TextBook fallback;

    public TextBook(Map<String, String> map) {
        this(map, null);
    }

    public TextBook(Map<String, String> map, TextBook fallback) {
        this.map = new HashMap<>(map);
        this.fallback = fallback;
    }

    public TextBook withFallback(TextBook fb) {
        return new TextBook(map, fb);
    }

    public Set<String> keys() {
        return map.keySet();
    }

    public boolean has(String key) {
        return map.containsKey(key) || (fallback != null && fallback.has(key));
    }

    /** 缺失时返回 "[key]",让你一眼看出哪条文字没写。 */
    public String get(String key) {
        String v = lookup(key);
        return v != null ? v : "[" + key + "]";
    }

    public String get(String key, String fallbackText) {
        String v = lookup(key);
        return v != null ? v : fallbackText;
    }

    private String lookup(String key) {
        String v = map.get(key);
        if (v != null) return v;
        return fallback == null ? null : fallback.lookup(key);
    }
}
