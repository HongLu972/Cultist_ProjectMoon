package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import game.core.Situation;
import game.core.Slot;
import java.util.ArrayList;
import java.util.List;

public class WindowView extends Table {
    private static final Color WINDOW = new Color(0.20f, 0.16f, 0.26f, 1f);
    private static final Color EVENT_WINDOW = new Color(0.16f, 0.20f, 0.26f, 1f);

    public final Situation situation;
    public final List<SlotActor> slotActors = new ArrayList<>();
    public final Label titleLabel;
    private final Ui ui;
    private final Label status;
    private final TextButton startButton;

    public WindowView(Ui ui, Situation situation) {
        this.ui = ui;
        this.situation = situation;
        setBackground(ui.rect(situation.eventWindow ? EVENT_WINDOW : WINDOW));
        setTouchable(Touchable.enabled); // 整个窗口可接受拖拽
        pad(8);

        String title = situation.eventWindow
            ? ui.texts.get("event." + situation.selected.id + ".name", situation.selected.id)
            : ui.texts.get("slot." + situation.verbId + ".name", situation.verbId);
        titleLabel = new Label(title, ui.labelStyle(Color.GOLD));
        titleLabel.setTouchable(Touchable.enabled);
        add(titleLabel).left().row();

        Table slotRow = new Table();
        for (int i = 0; i < situation.slots.size(); i++) {
            Slot slot = situation.slots.get(i);
            SlotActor sa = new SlotActor(ui, situation, i);
            slotActors.add(sa);

            Table column = new Table();
            // 判断是否为起始槽的 Carrier
            boolean isCarrier = situation.isSeed(i);
            String slotNameKey = isCarrier ? "slot.carrier" : "slot." + slot.defId;
            column.add(new Label(ui.texts.get(slotNameKey + ".name", slot.defId), ui.labelStyle(Color.LIGHT_GRAY))).padBottom(4).row();
            column.add(sa).size(SlotActor.W, SlotActor.H);
            slotRow.add(column).pad(4);
        }

        ScrollPane scroll = new ScrollPane(slotRow);
        scroll.setScrollingDisabled(false, true);
        scroll.setFadeScrollBars(false);
        add(scroll).width(Math.min(situation.slots.size() * (SlotActor.W + 8), 380)).padTop(4).row();

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
        if (!situation.eventWindow || !situation.selected.autoStart) {
            add(startButton).width(120).height(40).padTop(6);
        }

        pack();
        pack();
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
