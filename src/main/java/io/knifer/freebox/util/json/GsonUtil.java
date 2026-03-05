package io.knifer.freebox.util.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON工具类
 *
 * @author Knifer
 * @version 1.0.0
 */
@UtilityClass
public class GsonUtil {

    private final static Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
            .registerTypeAdapterFactory(new ValueEnumTypeAdapterFactory())
            .create();

    private final static Gson prettyGson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
            .registerTypeAdapterFactory(new ValueEnumTypeAdapterFactory())
            .create();

    public String toJson(Object object){
        return gson.toJson(object);
    }

    public <T> T fromJson(String objectStr, Class<T> clazz){
        return gson.fromJson(objectStr, clazz);
    }

    public <T> T fromJson(JsonElement jsonElement, Class<T> clazz) {
        return gson.fromJson(jsonElement, clazz);
    }

    public <T> T fromJson(String objectStr, TypeToken<T> typeToken) {
        return gson.fromJson(objectStr, typeToken);
    }

    public <T> T fromJson(JsonElement jsonElement, TypeToken<T> typeToken) {
        return gson.fromJson(jsonElement, typeToken);
    }

    public JsonElement toJsonTree(Object object) {
        return gson.toJsonTree(object);
    }

    public String toPrettyJson(Object object){
        return prettyGson.toJson(object);
    }

    /**
     * 将 JsonElement 转换为 Map<String, String>
     */
    public static Map<String, String> toStringMap(JsonElement element) {
        Map<String, String> map = new HashMap<>();
        JsonObject jsonObject;
        String key;
        JsonElement value;

        if (element == null || !element.isJsonObject()) {

            return map;
        }
        jsonObject = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            map.put(key, getStringValue(value));
        }

        return map;
    }

    /**
     * 将 JSON 字符串转换为 Map<String, String>
     */
    public static Map<String, String> toStringMap(String json) {
        JsonElement element;

        if (json == null || json.trim().isEmpty()) {

            return new HashMap<>();
        }

        try {
            element = JsonParser.parseString(json);

            return toStringMap(element);
        } catch (Exception e) {

            return new HashMap<>();
        }
    }

    /**
     * 获取 JsonElement 的字符串值
     */
    private static String getStringValue(JsonElement element) {
        JsonPrimitive primitive;

        if (element == null || element.isJsonNull()) {
            return "";
        }

        if (element.isJsonPrimitive()) {
            primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {

                return primitive.getAsString().trim();
            }

            return primitive.getAsString();
        }

        return element.toString();
    }

    public boolean isJson(String text) {
        try {
            if (StringUtils.isBlank(text)) {

                return false;
            }

            new JSONObject(text);

            return true;
        } catch (Exception e) {

            return false;
        }
    }
}
