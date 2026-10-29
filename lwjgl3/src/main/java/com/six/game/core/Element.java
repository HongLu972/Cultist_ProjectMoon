package game.core;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 卡牌定义。只含逻辑数据,名称和描述在文字文件里:
 * element.&lt;id&gt;.name / element.&lt;id&gt;.desc
 */
public class Element {
    public String id;
    /** 图片路径(相对 assets),可选;不写则不显示图片。 */
    public String image;
    /** 词条。卡牌自身的 id 也隐式算作一个词条。 */
    public HashMap<String, Integer> aspects = new HashMap<>();
    /** 寿命(秒)。-1 或不写表示永久。 */
    public float lifetime = -1f;
    /** 寿命到期后变成哪张牌;不写则直接消失。 */
    public String decayTo;
    /** "放在行动槽 X 里,触发事件 Y"。 */
    public ArrayList<Trigger> triggers = new ArrayList<>();

    public static class Trigger {
        /** 行动槽 id,即 verbs.json 里某个行动的 id。 */
        public String slot;
        /** 要触发的事件 id(必须是 manual 事件)。 */
        public String event;
    }

    /** LibGDX Json 反序列化需要无参构造。 */
    public Element() {}

    public Element(String id) {
        this.id = id;
    }

    /** 这张牌放进 slotId 对应的行动槽时会触发哪个事件;没有则返回 null。 */
    public String eventFor(String slotId) {
        for (Trigger t : triggers) {
            if (t.slot.equals(slotId)) return t.event;
        }
        return null;
    }
}
