package game.core;

import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 把 JSON 文本读成 Library / RecipeBook。只接收 String,不依赖 Gdx.files,
 * 所以能直接在 JUnit 里测。接到 LibGDX 后这样用:
 *   loader.loadLibrary(Gdx.files.internal("data/elements.json").readString("UTF-8"));
 */
public class ContentLoader {
    private final Json json = new Json();

    public ContentLoader() {
        // 多态 Effect 用 "type" 字段区分(默认字段名是 "class")。
        json.setTypeName("type");
        json.addClassTag("create", CreateEffect.class);
        json.addClassTag("consume_card", ConsumeCardEffect.class);
        json.addClassTag("consume_aspect", ConsumeAspectEffect.class);

        // 多数情况下 LibGDX 能从泛型读出元素类型,显式声明一遍更保险。
        json.setElementType(Element.class, "aspects", Integer.class);
        json.setElementType(Recipe.class, "requirements", Integer.class);
        json.setElementType(Recipe.class, "effects", Effect.class);
        json.setElementType(Recipe.class, "linked", Recipe.Link.class);
        json.setElementType(Recipe.Link.class, "requires", Integer.class);
        // 注意:没有开 setIgnoreUnknownFields,字段名拼错会直接抛 SerializationException,这是好事。
    }

    @SuppressWarnings("unchecked")
    public Library loadLibrary(String text) {
        ArrayList<Element> list = json.fromJson(ArrayList.class, Element.class, text);
        Library lib = new Library();
        for (Element e : list) {
            if (e.id == null || e.id.isEmpty()) {
                throw new IllegalStateException("有卡牌缺少 id");
            }
            if (lib.has(e.id)) {
                throw new IllegalStateException("重复的卡牌 id: " + e.id);
            }
            lib.add(e);
        }
        return lib;
    }

    @SuppressWarnings("unchecked")
    public RecipeBook loadRecipes(String text, Library library) {
        ArrayList<Recipe> list = json.fromJson(ArrayList.class, Recipe.class, text);
        RecipeBook book = new RecipeBook();
        Set<String> ids = new HashSet<>();
        for (Recipe r : list) {
            if (r.id == null || r.id.isEmpty()) {
                throw new IllegalStateException("有配方缺少 id");
            }
            if (!ids.add(r.id)) {
                throw new IllegalStateException("重复的配方 id: " + r.id);
            }
            if (!r.linkedOnly && r.verb == null) {
                throw new IllegalStateException("配方 " + r.id + " 缺少 verb(只能被链接触发的配方请设 linkedOnly)");
            }
            book.add(r);
        }
        // 所有配方都进了 book 之后再校验引用,这样配方的先后顺序不影响结果。
        for (Recipe r : list) {
            validate(r, book, library);
        }
        return book;
    }

    private void validate(Recipe r, RecipeBook book, Library library) {
        for (Recipe.Link link : r.linked) {
            if (book.get(link.id) == null) {
                throw new IllegalStateException("配方 " + r.id + " 链接到不存在的配方: " + link.id);
            }
        }
        for (Effect e : r.effects) {
            if (e instanceof CreateEffect c && !library.has(c.target)) {
                throw new IllegalStateException("配方 " + r.id + " 创建了未知卡牌: " + c.target);
            }
            if (e instanceof ConsumeCardEffect c && !library.has(c.target)) {
                throw new IllegalStateException("配方 " + r.id + " 消耗了未知卡牌: " + c.target);
            }
        }
    }
}
