package game.core;

import java.util.ArrayList;

/** 结局。名称/描述在文字文件里:ending.<id>.name / .desc */
public class EndingDef {
    public String id;
    public String type = "BE";

    // 冰汽时代式结局回顾数据
    public Review review;

    public static class Review {
        public float duration; // 动画/字幕持续时间（秒）
        public ArrayList<Subtitle> subtitles = new ArrayList<>();
    }

    public static class Subtitle {
        public float time;   // 在第几秒出现
        public String text;  // 显示的文字（支持变量替换，如 "牺牲了 {casualty} 人"）
    }
}
