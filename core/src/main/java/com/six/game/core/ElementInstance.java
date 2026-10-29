package game.core;

import java.util.HashMap;
import java.util.Map;

/** 卡牌实例:玩家桌上真实存在的每一张牌。id 由 GameState 分配。 */
public class ElementInstance {
    public final String instanceId;
    public final Element def;
    /** 配方计时中,槽里的牌被锁定,不能拖走。 */
    public boolean locked;

    ElementInstance(Element def, String instanceId) {
        this.def = def;
        this.instanceId = instanceId;
    }

    /** 属性表,自带"以自身 id 命名的隐式属性",这样"需要一张灯"和"需要 2 点激情"可用同一套匹配逻辑。 */
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
