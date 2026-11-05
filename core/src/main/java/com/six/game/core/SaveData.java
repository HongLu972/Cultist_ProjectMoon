package com.six.game.core;

import java.util.ArrayList;
import java.util.List;

public class SaveData {
    public String saveName = "";
    public static class CardData {
        public String saveId; // 存档专用的临时ID，用于关联槽位
        public String defId;
        public int count;
        public float x, y;
        public boolean locked;
        public float remaining;
    }

    public static class SituationData {
        public String situationId;
        public String verbId;
        public String eventId;
        public float x, y;
        public float timer;
        public String runningEventId;
        public String selectedEventId;
        public List<String> slotSaveIds = new ArrayList<>();
        public List<String> slotDefIds = new ArrayList<>(); // 👈 新增：用来保存槽位原本的名字（如 work, time_slot）
    }

    public List<CardData> cards = new ArrayList<>();
    public List<SituationData> situations = new ArrayList<>();
    public Chronicle chronicle = new Chronicle();
    public String ending; // 记录结局状态
}
