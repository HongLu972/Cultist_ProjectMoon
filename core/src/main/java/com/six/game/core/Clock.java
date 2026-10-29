package game.core;

import java.util.ArrayList;
import java.util.List;

public class Clock {
    /** 0 = 暂停, 1 = 正常, 2 = 加速。 */
    public float timeScale = 1f;

    public void update(float rawDelta, GameState state) {
        float d = rawDelta * timeScale;
        if (d <= 0) return;
        // 拷贝一份再遍历:结算时 Effect 可能新增/删除窗口。
        List<Situation> snapshot = new ArrayList<>(state.situations);
        for (Situation s : snapshot) s.update(d, state);
    }
}
