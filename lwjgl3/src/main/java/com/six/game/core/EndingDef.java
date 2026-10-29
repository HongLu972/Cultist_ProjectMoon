package game.core;

/** 结局。名称/描述在文字文件里:ending.&lt;id&gt;.name / .desc */
public class EndingDef {
    public String id;
    /** 结局类型,如 BE(坏结局)/ GE / TE。界面按 ui.ending_type.&lt;type&gt; 取显示文字。 */
    public String type = "BE";
}
