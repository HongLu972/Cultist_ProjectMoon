package com.six.game;

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
import com.six.game.core.ElementInstance;

/** 一张牌(或一摞牌)的视图。 */
public class CardActor extends Table {
    // 卡牌尺寸：100(图) + 40(名) + 20(数) = 160
    public static final float W = 100, H = 160;

    private static final Color NORMAL = new Color(0.55f, 0.42f, 0.22f, 1f);
    private static final Color LOCKED = new Color(0.30f, 0.26f, 0.20f, 1f);
    private static final Color SHADOW = new Color(0.30f, 0.22f, 0.12f, 1f);

    public final ElementInstance card;
    private final Drawable shadow;

    public CardActor(Ui ui, ElementInstance card) {
        this.card = card;
        this.shadow = ui.rect(SHADOW);
        setBackground(ui.rect(card.locked ? LOCKED : NORMAL));
        setTouchable(Touchable.enabled);
        setSize(W, H);

        // ==========================
        // 1. 图片区域 (100 x 100)
        // ==========================
        Texture tex = ui.texture(card.def.image);
        if (tex != null) {
            Image img = new Image(tex);
            // 强制拉伸填满 100x100 的区域，绝对不留底边
            img.setScaling(Scaling.stretch);
            img.setTouchable(Touchable.disabled);
            add(img).size(W, W).row();
        } else {
            // 没图片也占住 100 像素高度，防止文字上移
            add().size(W, W).row();
        }

        // ==========================
        // 2. 名字区域 (100 x 40)
        // ==========================
        String nameText = ui.texts.get("element." + card.def.id + ".name", card.def.id);
        Label nameLabel = new Label(nameText, ui.labelStyle(Color.WHITE));
        nameLabel.setAlignment(Align.center);
        nameLabel.setWrap(true);
        nameLabel.setTouchable(Touchable.disabled);

        // 自动缩小字体逻辑：
        // 先创建一个临时的 Cell 测算高度，如果超过 40 像素，就按比例缩小
        add(nameLabel).width(W - 6).height(40).padBottom(4).row();

        if (card.count > 1) {
            Label countLabel = new Label("x" + card.count, ui.labelStyle(Color.LIGHT_GRAY));
            countLabel.setAlignment(Align.center);
            countLabel.setTouchable(Touchable.disabled);

            // 【修改点】去掉 .bottom()，改用 .padBottom 让它稍微往上抬一点
            add(countLabel).width(W).height(20).padBottom(4).row();
        } else {
            // 单张牌也占住 20 像素，保证所有卡牌高度一致
            add().width(W).height(20).row();
        }

        // 【核心修复】在 Table 完成一次排版后，检查名字有没有溢出，有的话自动缩字体
        pack();
        float preferredHeight = nameLabel.getPrefHeight();
        if (preferredHeight > 40) {
            // 将字体缩放到刚好塞进 40 像素的高度
            nameLabel.setFontScale(40f / preferredHeight);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (card.count > 1) {
            batch.setColor(1f, 1f, 1f, parentAlpha);
            shadow.draw(batch, getX() + 8f, getY() - 8f, getWidth(), getHeight());
            shadow.draw(batch, getX() + 4f, getY() - 4f, getWidth(), getHeight());
        }
        super.draw(batch, parentAlpha);
    }
}
