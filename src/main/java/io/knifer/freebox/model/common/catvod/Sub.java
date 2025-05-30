package io.knifer.freebox.model.common.catvod;

import com.google.gson.annotations.SerializedName;

public class Sub {

    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;
    @SerializedName("lang")
    private String lang;
    @SerializedName("format")
    private String format;

    public static Sub create() {
        return new Sub();
    }

    public Sub name(String name) {
        this.name = name;
        return this;
    }

    public Sub url(String url) {
        this.url = url;
        return this;
    }

    public Sub lang(String lang) {
        this.lang = lang;
        return this;
    }

    public Sub format(String format) {
        this.format = format;
        return this;
    }

    public Sub ext(String ext) {
        switch (ext) {
            case "vtt":
                return format("text/vtt");
            case "ass":
            case "ssa":
                return format("text/x-ssa");
            default:
                return format("application/x-subrip");
        }
    }
}
