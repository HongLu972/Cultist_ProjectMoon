package com.six.game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

public class ModManager {

    /**
     * 加载 JSON：优先读 exe 同级的 data 文件夹，如果没有，再读 exe 内部打包的 assets。
     * @param internalPath 比如 "data/elements.json"
     * @param modName 比如 "elements.json"
     */
    public static String loadWithMods(String internalPath, String modName) {
        JsonReader reader = new JsonReader();
        JsonValue rootArray = null;

        // 1. 尝试读取外部（exe 同级的 data 文件夹）里的文件
        // 注意：这里路径去掉了 "data/"，因为我们希望玩家看到的是一个干净的 data 文件夹
        FileHandle externalFile = Gdx.files.local("data/" + modName);

        if (externalFile.exists()) {
            if (externalFile.exists()) {
                try {
                    rootArray = reader.parse(externalFile.readString("UTF-8"));
                    Gdx.app.log("ModManager", "读取外部数据成功: " + externalFile.path());
                } catch (Exception e) {
                    Gdx.app.error("ModManager", "外部 JSON 格式错误！已回退到内部默认数据", e);
                    rootArray = reader.parse(Gdx.files.internal(internalPath).readString("UTF-8"));
                }
            }
        } else {
            // 2. 如果外部没有，读取 exe 内部打包的兜底数据
            Gdx.app.log("ModManager", "读取内部数据: " + internalPath);
            rootArray = reader.parse(Gdx.files.internal(internalPath).readString("UTF-8"));
        }

        // 3. 检查 mods 文件夹，如果有同名文件，进行覆盖合并
        FileHandle modsDir = Gdx.files.local("mods");
        if (modsDir.exists() && modsDir.isDirectory()) {
            for (FileHandle mod : modsDir.list()) {
                if (!mod.isDirectory()) continue;
                FileHandle modFile = mod.child(modName);
                if (modFile.exists()) {
                    try {
                        JsonValue modArray = reader.parse(modFile.readString("UTF-8"));
                        for (JsonValue item = modArray.child; item != null; item = item.next) {
                            rootArray.addChild(item);
                        }
                        Gdx.app.log("ModManager", "已加载模组: " + mod.name() + "/" + modName);
                    } catch (Exception e) {
                        Gdx.app.error("ModManager", "模组加载失败: " + mod.name(), e);
                    }
                }
            }
        }
        return rootArray.toJson(JsonWriter.OutputType.json);
    }
}
