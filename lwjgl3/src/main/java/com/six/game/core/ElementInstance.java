package game.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 桌上真实存在的一摞牌(count 张同种牌;不可堆叠的牌 count 恒为 1)。id 由 GameState 分配。
 * x / y 是它在世界地图上的位置(左下角),属于要随存档保存的布局数据;NaN 表示"还没安排位置"。
 */
public class ElementInstance {
    public final String instanceId;
    public final Element def;
    public String tempSaveId; // 临时存储，用于存读档时关联槽位
    public int count = 1;
    /** 事件计时中,槽里的牌被锁定,不能拖走,也不会被"吸走"。 */
    public boolean locked;
    /** 剩余寿命(秒)。仅当 def.lifetime > 0 时有意义。 */
    public float remaining;
    public float x = Float.NaN;
    public float y = Float.NaN;

    ElementInstance(Element def, String instanceId) {
        this.def = def;
        this.instanceId = instanceId;
        this.remaining = def.lifetime > 0 ? def.lifetime : -1f;
    }

    /** 单张牌的词条表,自带"以自身 id 命名的隐式词条"。 */
    public Map<String, Integer> aspects() {
        Map<String, Integer> m = new HashMap<>(def.aspects);
        m.merge(def.id, 1, Integer::sum);
        return m;
    }

    @Override
    public String toString() {
        return instanceId + (count > 1 ? " x" + count : "");
    }
}
