package game.core;

/** data/config.json。 */
public class GameConfig {
    /** 语言代码,对应 data/text_&lt;language&gt;.json。 */
    public String language = "zh";
    /** 右上角可选的倍速。 */
    public float[] speeds = { 0.75f, 1f, 1.5f, 2f };
}
