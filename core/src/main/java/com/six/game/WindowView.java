package com.six.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.six.game.core.Situation;
import com.six.game.core.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * 局面详情面板:停靠在屏幕右侧的共享区域(密教式)。
 * 位置由 GameScreen 每帧锚定(随缩放补偿),自身不可拖动;面板本体拦截点击,防止穿透到地图。
 */
public class WindowView extends Table {
    /** 面板内容区固定宽度(总宽 = 360 + pad 8*2 = 376,约占 1280 宽屏的 29%)。 */
    public static final float CONTENT_W = 360f;

    private static final Color WINDOW = new Color(0.20f, 0.16f, 0.26f, 1f);
    private static final Color EVENT_WINDOW = new Color(0.16f, 0.20f, 0.26f, 1f);
    private static final Color HEADER_COLOR = new Color(0.28f, 0.22f, 0.35f, 1f); // 标题栏颜色

    public final Situation situation;
    public final List<SlotActor> slotActors = new ArrayList<>();
    public final Label titleLabel;
    public final Table header; // 暴露标题栏(点击可弹出信息卡)
    private final Ui ui;
    private final Label descLabel;
    private final Label status;
    private final TextButton startButton;

    public WindowView(Ui ui, Situation situation) {
        this.ui = ui;
        this.situation = situation;
        setBackground(ui.rect(situation.eventWindow ? EVENT_WINDOW : WINDOW));
        pad(8);

        // 面板本体接收触摸并拦截事件:点在面板空隙上不会穿透到地图(误拖地图/误收面板)
        setTouchable(Touchable.enabled);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                return true;
            }
        });

        String title = situation.eventWindow
            ? (situation.selected != null
                ? ui.texts.get("event." + situation.selected.id + ".name", situation.selected.id)
                : situation.situationId)
            : ui.texts.get("slot." + situation.verbId + ".name", situation.verbId);

        // --- 横跨全宽的标题栏 ---
        header = new Table();
        header.setBackground(ui.rect(HEADER_COLOR));
        header.setTouchable(Touchable.enabled); // 标题栏接收触摸
        header.pad(6);

        titleLabel = new Label(title, ui.labelStyle(Color.GOLD));
        titleLabel.setTouchable(Touchable.disabled); // 文字不拦截,让点击落在 header 上

        header.add(titleLabel).left().expandX();
        add(header).growX().padBottom(8).row();
        // -----------------------

        // 剧情描述:选了事件显示事件自己的 desc;行动窗没选事件时退回行动(slot)的默认 desc
        descLabel = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        descLabel.setWrap(true);
        descLabel.setTouchable(Touchable.disabled);
        descLabel.setVisible(false);
        add(descLabel).width(CONTENT_W).padTop(2).padBottom(4).row();

        Table slotRow = new Table();
        for (int i = 0; i < situation.slots.size(); i++) {
            Slot slot = situation.slots.get(i);
            SlotActor sa = new SlotActor(ui, situation, i);
            slotActors.add(sa);

            Table column = new Table();
            boolean isCarrier = situation.isSeed(i);
            String slotNameKey = isCarrier ? "slot.carrier" : "slot." + slot.defId;
            column.add(new Label(ui.texts.get(slotNameKey + ".name", slot.defId), ui.labelStyle(Color.LIGHT_GRAY))).padBottom(4).row();
            column.add(sa).size(SlotActor.W, SlotActor.H);
            slotRow.add(column).pad(4);
        }

        ScrollPane scroll = new ScrollPane(slotRow);
        scroll.setScrollingDisabled(false, true);
        scroll.setFadeScrollBars(false);
        add(scroll).width(CONTENT_W).padTop(4).row();

        status = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        status.setWrap(true);
        add(status).width(CONTENT_W).minHeight(56).padTop(6).row();

        startButton = new TextButton(ui.texts.get("ui.start"), ui.buttonStyle());
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                situation.start();
            }
        });
        if (!situation.eventWindow || situation.selected == null || !situation.selected.autoStart) {
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

        // 描述跟随选中事件变化:选了事件显示事件 desc,行动窗退回默认 desc(槽位的默认描述)
        String d = "";
        if (situation.selected != null) {
            String key = "event." + situation.selected.id + ".desc";
            if (ui.texts.has(key)) d = ui.texts.get(key, "");
        }
        if (d.isEmpty() && !situation.eventWindow && situation.verbId != null
            && ui.texts.has("slot." + situation.verbId + ".desc")) {
            d = ui.texts.get("slot." + situation.verbId + ".desc", "");
        }
        if (!d.equals(descLabel.getText().toString())) descLabel.setText(d);
        descLabel.setVisible(!d.isEmpty());

        startButton.setDisabled(!situation.canStart());
    }
}
