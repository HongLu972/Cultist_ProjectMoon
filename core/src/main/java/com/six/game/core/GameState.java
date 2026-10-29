package game.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameState {
    public final Library library;
    public final RecipeBook book;
    public final List<ElementInstance> cards = new ArrayList<>();
    public final List<Situation> situations = new ArrayList<>();
    /** 所有随机判定都走这一个 Random。测试或回放时可以换成带种子的。 */
    public Random random = new Random();

    // id 计数器属于存档的一部分,放在状态里而不是 static,测试互不污染。
    private int nextCardId = 0;
    private int nextSituationId = 0;

    public GameState(Library library, RecipeBook book) {
        this.library = library;
        this.book = book;
    }

    public ElementInstance addCard(Element def) {
        ElementInstance c = new ElementInstance(def, def.id + "#" + (nextCardId++));
        cards.add(c);
        return c;
    }

    /** 删除一张牌,同时把它从所有槽里清掉。 */
    public void removeCard(ElementInstance c) {
        cards.remove(c);
        for (Situation s : situations) {
            for (Slot slot : s.slots) {
                if (slot.content == c) slot.content = null;
            }
        }
    }

    public Situation addSituation(String verbId, int slotCount) {
        Situation s = new Situation(verbId + "_" + (nextSituationId++), verbId, slotCount);
        situations.add(s);
        return s;
    }

    public long count(String elementId) {
        return cards.stream().filter(c -> c.def.id.equals(elementId)).count();
    }
}
