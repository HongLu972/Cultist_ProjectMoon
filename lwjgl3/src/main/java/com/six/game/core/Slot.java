package game.core;

import java.util.Map;

public class Slot {
    /** 实例 id:situationId + ":" + 槽位定义 id,全局唯一。 */
    public final String id;
    /** 槽位定义 id。起始槽的定义 id 就是行动 id。 */
    public final String defId;
    public final Map<String, Integer> accepts;
    public final boolean required;
    public ElementInstance content;

    public Slot(String id, String defId, Map<String, Integer> accepts, boolean required) {
        this.id = id;
        this.defId = defId;
        this.accepts = accepts;
        this.required = required;
    }
}
