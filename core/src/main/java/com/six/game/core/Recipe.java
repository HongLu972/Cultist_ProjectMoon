package game.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Recipe {
    public String id;
    /** 属于哪个行动窗口。linkedOnly 的配方可以不填。 */
    public String verb;
    /** 属性总量要求:槽里所有牌的属性之和 >= 这些值。 */
    public HashMap<String, Integer> requirements = new HashMap<>();
    public int priority;
    public float duration;
    public String description;
    /**
     * true 表示"只能由其他配方的 linked 触发",玩家点"开始"时不会匹配到它。
     * 否则一个没有 requirements 的后续配方会在空窗口里被直接启动。
     */
    public boolean linkedOnly;
    public ArrayList<Effect> effects = new ArrayList<>();
    /** 分支:结算后按顺序检查,第一个"条件满足且概率命中"的成为下一个配方。 */
    public ArrayList<Link> linked = new ArrayList<>();

    public static class Link {
        public String id;
        /** 0~100 的百分比。 */
        public int chance = 100;
        /** 结算后,槽里剩余牌的属性总量必须满足这些要求。空表示无条件。 */
        public HashMap<String, Integer> requires = new HashMap<>();
    }
}
