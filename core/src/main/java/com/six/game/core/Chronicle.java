package com.six.game.core;

import java.util.ArrayList;
import java.util.List;

public class Chronicle {
    public static class Record {
        public float gameTime;
        public String text;

        // 👇 必须要有无参构造函数，不然反射会炸！
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
