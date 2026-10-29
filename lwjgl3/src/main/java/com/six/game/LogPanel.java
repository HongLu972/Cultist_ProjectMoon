package game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.LinkedList;

/** 记录面板:事件结算时写入一行 event.<id>.result 文字。只保留最近几条。 */
public class LogPanel extends Table {
    public static final float W = 340;
    private static final float INNER = W - 24;
    private static final int MAX_LINES = 4;

    private final Label body;
    private final LinkedList<String> lines = new LinkedList<>();

    public LogPanel(Ui ui) {
        setBackground(ui.rect(new Color(0.12f, 0.10f, 0.16f, 1f)));
        setClip(true);
        pad(12);
        top().left();
        add(new Label(ui.texts.get("ui.log"), ui.labelStyle(Color.GOLD))).left().row();
        body = new Label("", ui.labelStyle(Color.LIGHT_GRAY));
        body.setWrap(true);
        add(body).width(INNER).left().top().padTop(8);
    }

    public void add(String line) {
        lines.add(line);
        while (lines.size() > MAX_LINES) lines.removeFirst();
        body.setText(String.join("\n\n", lines));
    }
}
