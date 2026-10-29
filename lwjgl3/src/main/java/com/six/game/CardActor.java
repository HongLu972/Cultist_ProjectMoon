package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import game.core.ElementInstance;

/** 一张牌(或一摞牌)的视图。只负责显示;状态以 ElementInstance 为准,每次重建界面时重新生成。 */
public class CardActor extends Table {
    public static final float W = 100, H = 140;
    private static final Color NORMAL = new Color(0.55f, 0.42f, 0.22f, 1f);
    private static final Color LOCKED = new Color(0.30f, 0.26f, 0.20f, 1f);
    private static final Color SHADOW = new Color(0.30f, 0.22f, 0.12f, 1f);

    public final ElementInstance card;
    private final Drawable shadow;

    public CardActor(Ui ui, ElementInstance card) {
        this.card = card;
        this.shadow = ui.rect(SHADOW);
        setBackground(ui.rect(card.locked ? LOCKED : NORMAL));
        setTouchable(Touchable.enabled); // Table 默认是 childrenOnly,不会自己接收触摸

        // 图片是可选的:没写 image、或文件不存在,就不显示
        Texture tex = ui.texture(card.def.image);
        if (tex != null) {
            Image img = new Image(tex);
            img.setScaling(Scaling.fit);
            img.setTouchable(Touchable.disabled);
            add(img).size(64, 64).padTop(8).row();
        }

        String name = ui.texts.get("element." + card.def.id + ".name", card.def.id);
        if (card.count > 1) name += "\nx" + card.count;   // 一摞牌显示张数
        Label label = new Label(name, ui.labelStyle(Color.WHITE));
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setTouchable(Touchable.disabled); // 让触摸落在整张牌上
        add(label).width(W - 10).expand().center();
        setSize(W, H);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (card.count > 1) {
            // 摞的"厚度":在牌后面错开画两层
            batch.setColor(1f, 1f, 1f, parentAlpha);
            shadow.draw(batch, getX() + 8f, getY() - 8f, getWidth(), getHeight());
            shadow.draw(batch, getX() + 4f, getY() - 4f, getWidth(), getHeight());
        }
        super.draw(batch, parentAlpha);
    }
}
