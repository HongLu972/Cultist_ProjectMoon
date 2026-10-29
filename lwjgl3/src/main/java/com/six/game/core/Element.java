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
    /** 能不能堆叠:同种牌可以叠成一摞,摞里的每张是同一种牌。 */
    public boolean stackable;
    /**
     * "放在 X 里,触发事件 Y"。X 是行动槽 id(verbs.json 里的行动),
     * 或者特殊值 "use"(在桌面上双击这张牌来"使用"它)。
     */
    public ArrayList<Trigger> triggers = new ArrayList<>();

    public static final String USE = "use";

    public static class Trigger {
        public String slot;
        public String event;
    }

    /** LibGDX Json 反序列化需要无参构造。 */
    public Element() {}

    public Element(String id) {
        this.id = id;
    }

    /** 这张牌在 slotId 处会触发哪个事件;没有则返回 null。 */
    public String eventFor(String slotId) {
        for (Trigger t : triggers) {
            if (t.slot.equals(slotId)) return t.event;
        }
        return null;
    }
}
