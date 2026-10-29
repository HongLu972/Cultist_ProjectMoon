package game.core;

import java.util.HashMap;

/** 卡牌定义:从 JSON 读取,全局每种卡只有一份。 */
public class Element {
    public String id;
    public String label;
    // 字段用具体类型 HashMap:LibGDX Json 对具体类型最稳妥。
    public HashMap<String, Integer> aspects = new HashMap<>();

    /** LibGDX Json 反序列化需要无参构造。 */
    public Element() {}

    public Element(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
