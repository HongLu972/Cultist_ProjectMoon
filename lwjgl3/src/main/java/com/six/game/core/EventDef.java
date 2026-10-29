package game.core;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 事件的逻辑定义。名称/描述/记录文字在文字文件里:
 * event.&lt;id&gt;.name / .desc / .result
 */
public class EventDef {
    public String id;
    /**
     * 启动方式:
     *   manual  玩家把牌放进行动槽触发,在窗口里点"开始"
     *   auto    游戏开始后自动循环,不占窗口(如"时光流逝");time 必须 &gt; 0
     *   linked  只能被其他事件的 trigger 触发;在窗口里继续沿用当前的槽位
     */
    public String start = "manual";
    /** 持续时间(秒)。linked 事件为 0 表示立刻结算。 */
    public float time;
    /** 开始所需的词条总量(槽里所有牌的词条之和)。用词条而不是指定卡牌。 */
    public HashMap<String, Integer> input = new HashMap<>();
    /** 事件带来的额外槽位(仅 manual 事件可有)。 */
    public ArrayList<SlotDef> slots = new ArrayList<>();
    /** 结算时执行的动作树。 */
    public ArrayList<Action> action = new ArrayList<>();
    /** 产物(JSON 里的 "products")。动作树里用 { "type": "produce" } 发放。 */
    public ArrayList<Product> products = new ArrayList<>();

    public static class Product {
        public String card;
        public int amount = 1;
    }
}
