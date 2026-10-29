package game.core;

/** 按卡牌 id 精确消耗:吃掉来源窗口槽里最多 amount 张 id 为 target 的牌。 */
public class ConsumeCardEffect implements Effect {
    public String target;
    public int amount = 1;

    public ConsumeCardEffect() {}

    public ConsumeCardEffect(String target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(GameState state, Situation source) {
        int left = amount;
        for (Slot slot : source.slots) {
            ElementInstance c = slot.content;
            if (left > 0 && c != null && c.def.id.equals(target)) {
                state.removeCard(c); // 同时会清空槽
                left--;
            }
        }
    }
}
