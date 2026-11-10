package com.six.game.core;

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

    /** 实例级临时词条（比如锻炼后增加的属性，可以写负数扣除） */
    public java.util.HashMap<String, Integer> tempAspects = new java.util.HashMap<>();

    /** 【底层逻辑用】所有词条总和（包含隐藏、临时、隐式ID） */
    public Map<String, Integer> aspects() {
        Map<String, Integer> m = new java.util.HashMap<>();
        if (def.aspects != null) m.putAll(def.aspects);
        // 把隐藏属性加进去（比如卡门暗藏的 cogito: 100）
        if (def.hiddenAspects != null) {
            def.hiddenAspects.forEach((k, v) -> m.merge(k, v, Integer::sum));
        }
        // 隐式 ID 属性
        m.merge(def.id, 1, Integer::sum);
        // 临时属性
        if (tempAspects != null) {
            tempAspects.forEach((k, v) -> m.merge(k, v, Integer::sum));
        }
        m.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return m;
    }

    /** 【UI 显示用】只显示基础词条和临时词条，隐藏词条绝不外露！ */
    public Map<String, Integer> visibleAspects() {
        Map<String, Integer> m = new java.util.HashMap<>();
        if (def.aspects != null) m.putAll(def.aspects);
        // 注意：这里不合并 def.hiddenAspects！
        if (tempAspects != null) {
            tempAspects.forEach((k, v) -> m.merge(k, v, Integer::sum));
        }
        m.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return m;
    }

    @Override
    public String toString() {
        return instanceId + (count > 1 ? " x" + count : "");
    }
}
