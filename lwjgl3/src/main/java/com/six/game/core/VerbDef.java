package game.core;

/**
 * 行动(如"作业""研究""出门")。每个行动是一个窗口,只有一个起始槽,槽的 id 就是行动 id。
 * 放进起始槽的第一张牌决定触发哪个事件,事件再决定窗口里出现几个槽。
 */
public class VerbDef {
    public String id;
}
