package game.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter; // 修复点：必须引入这个

/** 读取基础 JSON 和 mods 目录下的 JSON，将数组合并。 */
public class ModManager {
    public static String loadWithMods(String basePath, String modName) {
        JsonReader reader = new JsonReader();
        // 读取基础数据
        JsonValue rootArray = reader.parse(Gdx.files.internal(basePath).readString("UTF-8"));

        // 检查外部 mods 文件夹
        FileHandle modsDir = Gdx.files.local("mods");
        if (modsDir.exists() && modsDir.isDirectory()) {
            for (FileHandle mod : modsDir.list()) {
                if (!mod.isDirectory()) continue;
                FileHandle modFile = mod.child(modName);
                if (modFile.exists()) {
                    try {
                        JsonValue modArray = reader.parse(modFile.readString("UTF-8"));
                        // 将模组里的数组追加到主数组中
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
        // 修复点：传入 OutputType.json
        return rootArray.toJson(JsonWriter.OutputType.json);
    }
}
