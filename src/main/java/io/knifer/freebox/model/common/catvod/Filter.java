package io.knifer.freebox.model.common.catvod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Data
public class Filter {

    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("value")
    private List<Value> value;

    public Filter(String key, String name, List<Value> value) {
        this.key = key;
        this.name = name;
        this.value = value;
    }

    @NotNull
    public static LinkedHashMap<String, List<Filter>> fromJson(@NotNull String jsonStr) {
        LinkedHashMap<String, List<Filter>> map = new LinkedHashMap<>();
        JsonElement parse = GsonUtil.toJsonTree(jsonStr);
        if(parse == null) return map;
        JsonObject filters = parse.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : filters.entrySet()) {
            ArrayList<Filter> list = new ArrayList<>();
            JsonElement jsonElement = filters.get(entry.getKey());
            if(jsonElement.isJsonObject()) list.add(Filter.fromJson(jsonElement));
            else for(JsonElement item : jsonElement.getAsJsonArray()) list.add(Filter.fromJson(item));
            map.put(entry.getKey(), list);
        }
        return map;
    }

    private static Filter fromJson(JsonElement jsonElement) {
        return GsonUtil.fromJson(jsonElement, Filter.class);
    }
    @Data
    public static class Value {

        @SerializedName("n")
        private String n;
        @SerializedName("v")
        private String v;

        public Value(String value) {
            this.n = value;
            this.v = value;
        }

        public Value(String n, String v) {
            this.n = n;
            this.v = v;
        }
    }
}
