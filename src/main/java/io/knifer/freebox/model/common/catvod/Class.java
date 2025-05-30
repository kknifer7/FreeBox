package io.knifer.freebox.model.common.catvod;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Data
public class Class {

    @SerializedName("type_id")
    private String typeId;
    @SerializedName("type_name")
    private String typeName;
    @SerializedName("type_flag")
    private String typeFlag;

    public static List<Class> arrayFrom(String str) {
        Type listType = new TypeToken<List<Class>>() {
        }.getType();
        return new Gson().fromJson(str, listType);
    }

    public Class(String typeId) {
        this(typeId, typeId);
    }

    public Class(String typeId, String typeName) {
        this(typeId, typeName, null);
    }

    public Class(String typeId, String typeName, String typeFlag) {
        this.typeId = typeId;
        this.typeName = typeName;
        this.typeFlag = typeFlag;
    }

    /**
     * name=value&name1=value1&name2=value2
     * @param formatStr
     * @return
     */
    @NotNull
    public static List<Class> parseFromFormatStr(@NotNull String formatStr) {
        ArrayList<Class> list = new ArrayList<>();
        String[] split = formatStr.split("&");
        for (String s : split) {
            String[] strings = s.split("=");
            if(strings.length==2){
                list.add(new Class(strings[1], strings[0]));
            }
        }
        return list;
    }

    public static String listToFormatStr(List<Class> list){
        List<String> li = new ArrayList<>();
        for (Class aClass : list) {
            li.add(aClass.typeName+"="+aClass.typeId);
        }
        return String.join("&", li);
    }
}
