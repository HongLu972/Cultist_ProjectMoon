package game.core;

/** data/config.json。 */
public class GameConfig {
    /** 语言代码,对应 data/text_&lt;language&gt;.json。 */
    public String language = "zh";
    /** 右上角可选的倍速。 */
    public float[] speeds = { 0.75f, 1f, 1.5f, 2f };
    /** 世界地图大小(窗口只显示其中一部分,拖动背景来移动视角)。 */
    public float worldWidth = 3200f;
    public float worldHeight = 1800f;
}
