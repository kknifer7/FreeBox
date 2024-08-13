package io.knifer.freebox.util;

import com.google.gson.*;
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

    public String toJson(Object object){
        return gson.toJson(object);
    }

    public <T> T fromJson(String objectStr, Class<T> clazz){
        return gson.fromJson(objectStr, clazz);
    }

    public JsonElement toJsonTree(Object object) {
        return gson.toJsonTree(object);
    }

}
