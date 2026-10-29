package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * 浮动信息框:默认隐藏,点卡牌/槽位/窗口时在点击处附近弹出。
 * 不接收触摸,所以不会挡住下面的操作。
 */
public class InfoPanel extends Table {
    public static final float W = 340;
    private static final float INNER = W - 24;

    private final Label title;
    private final Label body;

    public InfoPanel(Ui ui) {
        setBackground(ui.rect(new Color(0.16f, 0.13f, 0.21f, 0.96f)));
        setTouchable(Touchable.disabled);
        pad(12);
        title = new Label("", ui.labelStyle(Color.GOLD));
        title.setWrap(true);
        body = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        body.setWrap(true);
        add(title).width(INNER).left().row();
        add(body).width(INNER).left().top().padTop(8);
    }

    public void show(String titleText, String bodyText) {
        title.setText(titleText);
        body.setText(bodyText);
        // 自动换行的 Label 要先拿到宽度才能算出高度,所以 pack 两遍
        pack();
        pack();
    }

    /** 放在 (x, y) 的右下方;放不下时自动挪回舞台内。 */
    public void place(float x, float y, float stageW, float stageH) {
        float px = Math.min(x + 16, stageW - getWidth());
        float py = y - getHeight() - 16;
        if (py < 0) py = Math.min(y + 16, stageH - getHeight());
        setPosition(Math.max(0, px), Math.max(0, py));
    }
}
