package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import game.core.Situation;
import game.core.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * 行动窗口:标题(行动名)、若干槽位(起始槽 + 事件带来的额外槽)、状态、"开始"按钮。
 * 槽位数量由引擎决定,这里只是把 situation.slots 画出来。描述文字默认不显示,点击才弹出。
 */
public class SituationView extends Table {
    private static final Color WINDOW = new Color(0.20f, 0.16f, 0.26f, 1f);

    public final Situation situation;
    public final List<SlotActor> slotActors = new ArrayList<>();
    /** 点标题可以查看这个窗口(进行中的事件)的信息。 */
    public final Label titleLabel;
    private final Ui ui;
    private final Label status;
    private final TextButton startButton;

    public SituationView(Ui ui, Situation situation) {
        this.ui = ui;
        this.situation = situation;
        setBackground(ui.rect(WINDOW));
        pad(8);

        titleLabel = new Label(ui.texts.get("slot." + situation.verbId + ".name", situation.verbId), ui.labelStyle(Color.GOLD));
        titleLabel.setTouchable(Touchable.enabled);
        add(titleLabel).left().row();

        Table slotRow = new Table();
        for (int i = 0; i < situation.slots.size(); i++) {
            Slot slot = situation.slots.get(i);
            SlotActor sa = new SlotActor(ui, situation, i);
            slotActors.add(sa);

            Table column = new Table();
            String slotName = ui.texts.get("slot." + slot.defId + ".name", slot.defId);
            column.add(new Label(slotName, ui.labelStyle(Color.LIGHT_GRAY))).padBottom(4).row();
            column.add(sa).size(SlotActor.W, SlotActor.H);
            slotRow.add(column).pad(4);
        }
        add(slotRow).row();

        status = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        status.setWrap(true);
        add(status).width(280).minHeight(56).padTop(6).row();

        startButton = new TextButton(ui.texts.get("ui.start"), ui.buttonStyle());
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                situation.start();
            }
        });
        add(startButton).width(120).height(40).padTop(6);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        String text = "";
        if (situation.isRunning()) {
            int secs = (int) Math.ceil(Math.max(0f, situation.timer));
            text = ui.texts.get("event." + situation.running.id + ".name", situation.running.id)
                    + "\n" + ui.texts.get("ui.remaining") + " " + secs + " " + ui.texts.get("ui.seconds");
        } else if (situation.selected != null) {
            text = ui.texts.get("event." + situation.selected.id + ".name", situation.selected.id);
        }
        status.setText(text);
        startButton.setDisabled(!situation.canStart());
    }
}
