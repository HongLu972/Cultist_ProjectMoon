package game.core;

import java.util.ArrayList;
import java.util.List;

public class Chronicle {
    public static class Record {
        public float gameTime;
        public String text;
        // 必须有无参构造
        public Record() {}
        public Record(float time, String text) {
            this.gameTime = time;
            this.text = text;
        }
    }

    public List<Record> history = new ArrayList<>();
    public float totalPlayTime = 0f;

    public void addRecord(float currentTime, String text) {
        history.add(new Record(currentTime, text));
    }
}
