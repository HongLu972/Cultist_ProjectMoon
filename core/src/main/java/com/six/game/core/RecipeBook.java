package game.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeBook {
    private final List<Recipe> recipes = new ArrayList<>();
    private final Map<String, Recipe> byId = new HashMap<>();

    public void add(Recipe r) {
        recipes.add(r);
        byId.put(r.id, r);
    }

    public Recipe get(String id) {
        return byId.get(id);
    }

    /** have 的每一项都 >= need 的对应项。need 为空则恒为 true。 */
    static boolean satisfies(Map<String, Integer> have, Map<String, Integer> need) {
        for (Map.Entry<String, Integer> req : need.entrySet()) {
            if (have.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
        }
        return true;
    }

    /** 在指定行动下,找出满足条件且优先级最高的"可手动启动"配方;没有则返回 null。 */
    public Recipe match(String verb, Map<String, Integer> have) {
        Recipe best = null;
        for (Recipe r : recipes) {
            if (r.linkedOnly || !verb.equals(r.verb)) continue;
            if (!satisfies(have, r.requirements)) continue;
            if (best == null || r.priority > best.priority) best = r;
        }
        return best;
    }
}
