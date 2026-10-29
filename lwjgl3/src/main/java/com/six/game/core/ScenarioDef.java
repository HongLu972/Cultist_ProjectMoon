package game.core;

import java.util.ArrayList;

/** 开局。名称/描述在文字文件里:scenario.&lt;id&gt;.name / .desc */
public class ScenarioDef {
    public String id;
    /** 开局给的牌。 */
    public ArrayList<EventDef.Product> cards = new ArrayList<>();
    /** 开局就有的行动窗口(verbs.json 里的行动 id)。其他行动要靠事件解锁。 */
    public ArrayList<String> verbs = new ArrayList<>();
    /** 开局就启动的自动事件(start 为 auto 的事件 id)。 */
    public ArrayList<String> autoEvents = new ArrayList<>();
}
