package game.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 动作树的节点(事件里的 "action")。每个节点执行后返回成功或失败,
 * 然后执行 onSuccess 或 onFail 里的子节点。具体类型见 Actions。
 */
public abstract class Action {
    public ArrayList<Action> onSuccess = new ArrayList<>();
    public ArrayList<Action> onFail = new ArrayList<>();

    /** 执行本节点自己的效果,返回是否成功。 */
    public abstract boolean execute(ActionContext ctx);

    public static void runAll(List<Action> actions, ActionContext ctx) {
        for (Action a : actions) {
            boolean ok = a.execute(ctx);
            runAll(ok ? a.onSuccess : a.onFail, ctx);
        }
    }
}
