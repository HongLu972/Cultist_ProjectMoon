package com.six.game.core;

/**
 * JSON 预处理器：让"沟槽的 JSON"也能写注释喵！
 *
 * 解析前剥除两种注释（字符串字面量内不生效）：
 * 1. 双斜杠行注释；2. 用 斜杠+星号 与 星号+斜杠 包起来的块注释。
 * 另外支持在任意对象内部写 "comment": "..." 或 "_comment": ... 键（值可为字符串/数字/布尔）。
 *
 * 注意：注释键只能写在对象内部，不能把 {"comment": ...} 当成数组里的独立元素。
 */
public final class JsonSanitizer {
    private JsonSanitizer() {}

    public static String clean(String raw) {
        if (raw == null || raw.isEmpty()) return raw;
        return stripCommentKeys(stripSlashedComments(raw));
    }

    /** 删除字符串字面量以外的双斜杠行注释与块注释。 */
    private static String stripSlashedComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean inStr = false;
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            if (inStr) {
                out.append(c);
                if (c == '\\') {
                    if (i + 1 < n) out.append(s.charAt(++i));
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                out.append(c);
                continue;
            }
            if (c == '/' && i + 1 < n) {
                char d = s.charAt(i + 1);
                if (d == '/') {                       // 行注释：吃到行尾
                    while (i < n && s.charAt(i) != '\n') i++;
                    out.append('\n');
                    continue;
                }
                if (d == '*') {                       // 块注释：吃到 星号+斜杠
                    i += 2;
                    while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
                    i++;                              // 越过结尾的斜杠
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /** 删除 "comment"/"_comment" 键值对（值为字符串/数字/布尔），并吃掉随之悬挂的逗号。 */
    private static String stripCommentKeys(String s) {
        String value = "\"(?:[^\"\\\\]|\\\\.)*\"|[-0-9.eE+]+|true|false";
        s = s.replaceAll("(,\\s*)\"_?comment\"\\s*:\\s*(" + value + ")\\s*", "$1");
        s = s.replaceAll("\"_?comment\"\\s*:\\s*(" + value + ")\\s*,\\s*", "");
        s = s.replaceAll("\"_?comment\"\\s*:\\s*(" + value + ")\\s*", "");
        return s;
    }
}
