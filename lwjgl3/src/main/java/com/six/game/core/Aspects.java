package game.core;

import java.util.Map;

/** 词条(属性)表的小工具。 */
final class Aspects {
    private Aspects() {}

    /** have 的每一项都 >= need 的对应项。need 为空则恒为 true。 */
    static boolean satisfies(Map<String, Integer> have, Map<String, Integer> need) {
        for (Map.Entry<String, Integer> req : need.entrySet()) {
            if (have.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
        }
        return true;
    }

    static int total(Map<String, Integer> m) {
        int sum = 0;
        for (int v : m.values()) sum += v;
        return sum;
    }
}
