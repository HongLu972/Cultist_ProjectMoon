package game.core;

import java.util.HashMap;
import java.util.Map;

/** 卡牌实例:玩家桌上真实存在的每一张牌。id 由 GameState 分配。 */
public class ElementInstance {
    public final String instanceId;
    public final Element def;
    /** 事件计时中,槽里的牌被锁定,不能拖走,也不会被"吸走"。 */
    public boolean locked;
    /** 剩余寿命(秒)。仅当 def.lifetime > 0 时有意义。 */
    public float remaining;

    ElementInstance(Element def, String instanceId) {
        this.def = def;
        this.instanceId = instanceId;
        this.remaining = def.lifetime > 0 ? def.lifetime : -1f;
    }

    /** 词条表,自带"以自身 id 命名的隐式词条"。 */
    public Map<String, Integer> aspects() {
        Map<String, Integer> m = new HashMap<>(def.aspects);
        m.merge(def.id, 1, Integer::sum);
        return m;
    }

    @Override
    public String toString() {
        return instanceId;
    }
}
