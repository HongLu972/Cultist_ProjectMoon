package game.core;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 事件的逻辑定义。名称/描述/记录文字在文字文件里:
 * event.&lt;id&gt;.name / .desc / .result
 */
public class EventDef {
    /** slot 的特殊值:不占用任何行动槽。 */
    public static final String NONE = "none";
    /** 如果设置了此项，事件开始时如果槽位为空，会强制从桌面吸入一张带有该词条的卡牌。 */
    public String forceDrainAspect;

    public String id;
    /**
     * 启动方式:
     *   manual  玩家把牌放进行动槽(或双击带 use 触发器的牌)触发
     *   auto    被 start_auto 动作或开局设定启动后自动循环,不占窗口(如"时光流逝");time 必须 &gt; 0
     *   linked  只能被其他事件的 trigger 触发;在窗口里继续沿用当前的槽位
     */
    public String start = "manual";
    /**
     * 必填。事件占用哪个行动槽:
     *   行动 id(如 "work")  在那个行动窗口里进行,期间占用该窗口
     *   "none"              不占用任何行动槽。manual 事件会自己开一个临时的"事件窗口",结束后自动关闭
     * auto / linked 事件恒为 "none"。
     */
    public String slot;
    /** manual 事件:放好牌、满足 input 后自动开始,不用再点"开始"。 */
    public boolean autoStart;
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
