package game.core;

import java.util.ArrayList;
import java.util.List;

public class SaveData {
    // 卡牌数据
    public static class CardData {
        public String defId;
        public int count;
        public float x, y;
        public boolean locked;
        public float remaining;
    }
    
    // 窗口数据
    public static class SituationData {
        public String verbId;
        public float x, y;
        public String runningEventId; // 正在跑的事件
        public float timer;
        // 槽位里的卡牌 defId
        public List<String> slotContents = new ArrayList<>(); 
    }

    public List<CardData> cards = new ArrayList<>();
    public List<SituationData> situations = new ArrayList<>();
    public Chronicle chronicle = new Chronicle();
}