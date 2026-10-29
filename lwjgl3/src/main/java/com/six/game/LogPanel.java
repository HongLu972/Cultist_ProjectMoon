package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** 记录面板:事件结算时写入一行。用 ScrollPane 实现自动向下滚动。 */
public class LogPanel extends Table {
    public static final float W = 340;
    private final Table contentTable;
    private final ScrollPane scrollPane;

    public LogPanel(Ui ui) {
        setBackground(ui.rect(new Color(0.12f, 0.10f, 0.16f, 1f)));
        pad(12);
        top().left();
        add(new Label(ui.texts.get("ui.log"), ui.labelStyle(Color.GOLD))).left().row();

        contentTable = new Table();
        contentTable.top().left();

        scrollPane = new ScrollPane(contentTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        add(scrollPane).width(W - 24).height(200).padTop(8);
    }

    public void add(String line, Ui ui) {
        Label label = new Label(line, ui.labelStyle(Color.LIGHT_GRAY));
        label.setWrap(true);
        contentTable.add(label).width(W - 40).left().row();
        contentTable.pack();
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f); // 自动滚到底部
    }
}
