package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import game.core.GameState;
import game.core.Recipe;
import game.core.Situation;
import java.util.ArrayList;
import java.util.List;

/** 行动窗口:标题、卡槽、状态文字、"开始"按钮。 */
public class SituationView extends Table {
    private static final Color WINDOW = new Color(0.20f, 0.16f, 0.26f, 1f);

    public final Situation situation;
    public final List<SlotActor> slotActors = new ArrayList<>();
    private final GameState state;
    private final Label status;
    private final TextButton startButton;

    public SituationView(Ui ui, GameState state, Situation situation, String title) {
        this.state = state;
        this.situation = situation;
        setBackground(ui.rect(WINDOW));
        pad(12);

        add(new Label(title, ui.labelStyle(Color.GOLD))).left().row();

        Table slotRow = new Table();
        for (int i = 0; i < situation.slots.size(); i++) {
            SlotActor sa = new SlotActor(ui, situation, i);
            slotActors.add(sa);
            slotRow.add(sa).size(SlotActor.W, SlotActor.H).pad(6);
        }
        add(slotRow).row();

        status = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        status.setWrap(true);
        add(status).width(260).minHeight(70).padTop(8).row();

        startButton = new TextButton("开始", ui.buttonStyle());
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                situation.start(state.book);
            }
        });
        add(startButton).width(120).height(40).padTop(8);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (situation.isRunning()) {
            Recipe r = situation.running;
            String desc = r.description == null ? "" : r.description;
            status.setText(desc + "\n剩余 " + (int) Math.ceil(Math.max(0f, situation.timer)) + " 秒");
            startButton.setDisabled(true);
        } else {
            status.setText("");
            boolean canStart = state.book.match(situation.verbId, situation.totalAspects()) != null;
            startButton.setDisabled(!canStart);
        }
    }
}
