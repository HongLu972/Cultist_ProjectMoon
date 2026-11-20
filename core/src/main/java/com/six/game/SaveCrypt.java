package com.six.game;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SaveCrypt {
    // 这是一个简单的异或密钥，你可以随便改
    private static final byte[] KEY = "Cultist_Project_Moon_Secret".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String json) {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return Base64.getEncoder().encodeToString(data);
    }

    public static String decrypt(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64.trim());
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
            }
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 如果解密失败（比如改了密钥或者根本不是加密文本），返回原文本，防止直接崩溃
            return base64;
        }
    }
}
