package game.core;

import java.util.ArrayList;
import java.util.List;

public class Chronicle {
    public static class Record {
        public float gameTime; // 发生的时间(秒)
        public String text;    // 发生了什么
    }

    public final List<Record> history = new ArrayList<>();
    public float totalPlayTime = 0f;

    public void addRecord(float currentTime, String text) {
        history.add(new Record() {{ gameTime = currentTime; this.text = text; }});
    }
}