package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import game.core.GameState;

/** 这是一个漂浮在世界上的窗口，专门展示后台自动事件的倒计时（如时光流逝）。 */
public class BackgroundPanel extends Table {
    private final Table content;
    private final Label timerLabel;
    private final Ui ui;

    public BackgroundPanel(Ui ui) {
        this.ui = ui;
        setBackground(ui.rect(new Color(0.15f, 0.12f, 0.18f, 1f)));
        pad(10);
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled); // 不挡鼠标

        content = new Table();
        timerLabel = new Label("", ui.labelStyle(Color.GOLD));
        content.add(timerLabel).left().row();
        add(content);

        pack();
    }

    public void update(GameState.BackgroundRun run) {
        String name = ui.texts.get("event." + run.event.id + ".name", run.event.id);
        int secs = (int) Math.ceil(Math.max(0f, run.timer));
        timerLabel.setText(name + "\n剩余: " + secs + " 秒");
    }
}
