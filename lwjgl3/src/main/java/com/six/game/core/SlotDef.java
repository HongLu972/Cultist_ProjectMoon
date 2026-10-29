package game.core;

import java.util.HashMap;

/** 事件带来的额外槽位定义。槽位 id 全局唯一。 */
public class SlotDef {
    public String id;
    /** 放进来的牌,词条必须满足这些要求。空表示什么牌都能放。 */
    public HashMap<String, Integer> accepts = new HashMap<>();
    /** true 表示这个槽必须放了牌才能开始。 */
    public boolean required;
}
