package com.six.game.core;

/**
 * 模组脚本接口：实现类放进 mods/&lt;模组名&gt;/scripts/（打包成 .jar，或直接放 .java 源码），
 * 游戏启动时自动加载并执行。
 */
public interface ModScript {
    /** 所有 JSON 数据加载并校验完成后调用一次。可在此用代码注册/修改卡牌、事件等数据。 */
    default void onContentLoaded(Library library, EventBook events, GameConfig config) {}
}
