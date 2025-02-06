package io.knifer.freebox.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.experimental.UtilityClass;

/**
 * JSON工具类
 *
 * @author Knifer
 * @version 1.0.0
 */
@UtilityClass
public class GsonUtil {

    private final static Gson gson = new GsonBuilder().create();

    private final static Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

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

}
