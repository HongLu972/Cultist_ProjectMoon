package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import game.core.ElementInstance;
import game.core.Situation;

/** 一个卡槽的视图,同时是拖拽的放置目标。槽里有牌时,牌的视图作为子节点放进来。 */
public class SlotActor extends Table {
    public static final float W = 120, H = 160;
    private static final Color SLOT_COLOR = new Color(0.12f, 0.10f, 0.16f, 1f);

    public final Situation situation;
    public final int index;
    /** 槽里没牌时为 null。 */
    public CardActor cardActor;

    public SlotActor(Ui ui, Situation situation, int index) {
        this.situation = situation;
        this.index = index;
        setBackground(ui.rect(SLOT_COLOR));
        setTouchable(Touchable.enabled);

        ElementInstance c = situation.slots.get(index).content;
        if (c != null) {
            cardActor = new CardActor(ui, c);
            add(cardActor).size(CardActor.W, CardActor.H).center();
        }
    }
}
