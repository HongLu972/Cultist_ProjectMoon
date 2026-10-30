package game.core;

import java.util.ArrayList;
import java.util.List;

public class SaveData {
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
        public String verbId;       // 如果是行动窗口，这里存行动 ID
        public String eventId;      // 如果是事件窗口，这里存事件 ID
        public float x, y;
        public float timer;
        public String runningEventId; // 正在跑的事件
        public String selectedEventId; // 选中但没开始的事件
        public List<String> slotSaveIds = new ArrayList<>(); // 槽位里卡的 saveId，空格存 "null"
    }

    public List<CardData> cards = new ArrayList<>();
    public List<SituationData> situations = new ArrayList<>();
    public Chronicle chronicle = new Chronicle();
    public String ending; // 记录结局状态
}
