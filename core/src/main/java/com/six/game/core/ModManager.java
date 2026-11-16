package com.six.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.File;

public class ModManager {

    private static JsonValue parseFile(File file, JsonReader reader) {
        try {
            return reader.parse(new String(java.nio.file.Files.readAllBytes(file.toPath()), "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + file.getAbsolutePath(), e);
        }
    }

    public static String loadWithMods(String internalPath, String modName) {
        JsonReader reader = new JsonReader();
        JsonValue rootArray = null;

        // 1. 找外部的 data/xxx.json
        File externalFile = new File("data/" + modName);
        if (externalFile.exists()) {
            Gdx.app.log("ModManager", "读取外部数据: " + externalFile.getAbsolutePath());
            rootArray = parseFile(externalFile, reader);
        } else {
            // 2. 找 jar 包内部的
            FileHandle internalFile = Gdx.files.internal(internalPath);
            if (internalFile.exists()) {
                Gdx.app.log("ModManager", "读取内部打包数据: " + internalPath);
                rootArray = reader.parse(internalFile.readString("UTF-8"));
            } else {
                // 3. 【核心修复】如果内部找不到，直接去 mods/Origin 里面找！
                File originFile = new File("mods/Origin/" + modName);
                if (originFile.exists()) {
                    Gdx.app.log("ModManager", "读取 Origin 兜底数据: " + originFile.getAbsolutePath());
                    rootArray = parseFile(originFile, reader);
                } else {
                    // 4. 开发环境兜底：暴力找 assets
                    File current = new File(System.getProperty("user.dir"));
                    File foundFile = null;
                    for (int i = 0; i < 4; i++) {
                        File testFile = new File(current, "assets/" + internalPath);
                        if (testFile.exists()) {
                            foundFile = testFile;
                            break;
                        }
                        current = current.getParentFile();
                        if (current == null) break;
                    }

                    if (foundFile != null) {
                        Gdx.app.log("ModManager", "穿透读取: " + foundFile.getAbsolutePath());
                        rootArray = parseFile(foundFile, reader);
                    } else {
                        throw new RuntimeException("【绝杀失败】找遍全世界也找不到: " + internalPath);
                    }
                }
            }
        }

        // 5. 找 mods/xxx 的覆盖数据合并
        File modsDir = new File("mods");
        if (modsDir.exists() && modsDir.isDirectory()) {
            File[] modDirs = modsDir.listFiles();
            if (modDirs != null) {
                for (File mod : modDirs) {
                    if (mod.isDirectory()) {
                        File modFile = new File(mod, modName);
                        if (modFile.exists()) {
                            // Origin 的数据我们已经在上面读取过了，这里防止重复合并自己
                            if (mod.getName().equals("Origin") && rootArray != null) {
                                continue;
                            }
                            JsonValue modArray = parseFile(modFile, reader);
                            for (JsonValue item = modArray.child; item != null; item = item.next) {
                                rootArray.addChild(item);
                            }
                            Gdx.app.log("ModManager", "已合并模组: " + mod.getName() + "/" + modName);
                        }
                    }
                }
            }
        }

        return rootArray.toJson(JsonWriter.OutputType.json);
    }
}
