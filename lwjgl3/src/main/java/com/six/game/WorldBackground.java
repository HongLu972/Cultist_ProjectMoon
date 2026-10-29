package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/**
 * 世界地图的背景:纯色底 + 淡淡的网格 + 边界线。网格让你拖动视角时能看出自己在移动。
 * 它同时是"拖动背景来平移视角"和"点空白处收起信息框"的触摸目标。
 */
public class WorldBackground extends Actor {
    private static final float GRID = 200f;
    private static final float BORDER = 8f;
    private final TextureRegion white;

    public WorldBackground(Ui ui, float width, float height) {
        this.white = ui.whiteRegion();
        setSize(width, height);
        setTouchable(Touchable.enabled);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX(), y = getY(), w = getWidth(), h = getHeight();

        batch.setColor(0.07f, 0.06f, 0.10f, parentAlpha);
        batch.draw(white, x, y, w, h);

        batch.setColor(1f, 1f, 1f, 0.05f * parentAlpha);
        for (float gx = 0; gx <= w; gx += GRID) batch.draw(white, x + gx - 1f, y, 2f, h);
        for (float gy = 0; gy <= h; gy += GRID) batch.draw(white, x, y + gy - 1f, w, 2f);

        batch.setColor(1f, 0.85f, 0.4f, 0.35f * parentAlpha);
        batch.draw(white, x, y, w, BORDER);
        batch.draw(white, x, y + h - BORDER, w, BORDER);
        batch.draw(white, x, y, BORDER, h);
        batch.draw(white, x + w - BORDER, y, BORDER, h);

        batch.setColor(Color.WHITE);
    }
}
