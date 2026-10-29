package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import game.core.ElementInstance;

/** 一张牌的视图。只负责显示;状态以 ElementInstance 为准,每次重建界面时重新生成。 */
public class CardActor extends Table {
    public static final float W = 100, H = 140;
    private static final Color NORMAL = new Color(0.55f, 0.42f, 0.22f, 1f);
    private static final Color LOCKED = new Color(0.30f, 0.26f, 0.20f, 1f);

    public final ElementInstance card;

    public CardActor(Ui ui, ElementInstance card) {
        this.card = card;
        setBackground(ui.rect(card.locked ? LOCKED : NORMAL));
        setTouchable(Touchable.enabled); // Table 默认是 childrenOnly,不会自己接收触摸

        Label label = new Label(card.def.label, ui.labelStyle(Color.WHITE));
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setTouchable(Touchable.disabled); // 让触摸落在整张牌上
        add(label).width(W - 10).expand().center();
        setSize(W, H);
    }
}
