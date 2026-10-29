package game.core;

/** 创建 amount 张 target 卡。 */
public class CreateEffect implements Effect {
    public String target;
    public int amount = 1;

    public CreateEffect() {}

    public CreateEffect(String target, int amount) {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(GameState state, Situation source) {
        Element def = state.library.element(target);
        for (int i = 0; i < amount; i++) {
            state.addCard(def);
        }
    }
}
