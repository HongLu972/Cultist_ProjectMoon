package game.core;

/** 配方结算时执行的效果。 */
public interface Effect {
    void apply(GameState state, Situation source);
}
