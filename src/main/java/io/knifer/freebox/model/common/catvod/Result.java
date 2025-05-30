package io.knifer.freebox.model.common.catvod;

import cn.hutool.json.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.*;

@Data
public class Result {

    @SerializedName("class")
    private List<Class> classes;
    @SerializedName("list")
    private List<Vod> list;
    @SerializedName("filters")
    private LinkedHashMap<String, List<Filter>> filters;
    @SerializedName("header")
    private String header;
    @SerializedName("format")
    private String format;

    @SerializedName("msg")
    private String msg;
    @SerializedName("danmaku")
    private String danmaku;
    @SerializedName("url")
    private Object url;
    @SerializedName("subs")
    private List<Sub> subs;
    @SerializedName("parse")
    private int parse;
    @SerializedName("jx")
    private int jx;
    @SerializedName("page")
    private Integer page;
    @SerializedName("pagecount")
    private Integer pagecount;
    @SerializedName("limit")
    private Integer limit;
    @SerializedName("total")
    private Integer total;

    public static Result objectFrom(String str) {
        return new Gson().fromJson(str, Result.class);
    }

    public static String string(Integer page,Integer pagecount,Integer limit,Integer total,List<Vod> list){
        return Result.get().page(page,pagecount,limit,total).vod(list).string();
    }

    public static String string(List<Class> classes, List<Vod> list, LinkedHashMap<String, List<Filter>> filters) {
        return Result.get().classes(classes).vod(list).filters(filters).string();
    }

    public static String string(List<Class> classes, List<Vod> list, JSONObject filters) {
        return Result.get().classes(classes).vod(list).filters(filters).string();
    }

    public static String string(List<Class> classes, List<Vod> list, JsonElement filters) {
        return Result.get().classes(classes).vod(list).filters(filters).string();
    }

    public static String string(List<Class> classes, LinkedHashMap<String, List<Filter>> filters) {
        return Result.get().classes(classes).filters(filters).string();
    }

    public static String string(List<Class> classes, JsonElement filters) {
        return Result.get().classes(classes).filters(filters).string();
    }

    public static String string(List<Class> classes, JSONObject filters) {
        return Result.get().classes(classes).filters(filters).string();
    }

    public static String string(List<Class> classes, List<Vod> list) {
        return Result.get().classes(classes).vod(list).string();
    }

    public static String string(List<Vod> list) {
        return Result.get().vod(list).string();
    }

    public static String string(Vod item) {
        return Result.get().vod(item).string();
    }

    public static Result get() {
        return new Result();
    }

    public static String error(String msg) {
        return Result.get().vod(Collections.emptyList()).msg(msg).string();
    }

    public Result msg(String msg) {
        this.msg = msg;
        return this;
    }

    public Result classes(List<Class> classes) {
        this.classes = classes;
        return this;
    }

    public Result vod(List<Vod> list) {
        this.list = list;
        return this;
    }

    public Result vod(Vod item) {
        this.list = Collections.singletonList(item);
        return this;
    }

    public Result filters(LinkedHashMap<String, List<Filter>> filters) {
        this.filters = filters;
        return this;
    }

    public Result filters(JSONObject object) {
        if (object == null) return this;
        Type listType = new TypeToken<LinkedHashMap<String, List<Filter>>>() {
        }.getType();
        this.filters = new Gson().fromJson(object.toString(), listType);
        return this;
    }

    public Result filters(JsonElement element) {
        if (element == null) return this;
        Type listType = new TypeToken<LinkedHashMap<String, List<Filter>>>() {
        }.getType();
        this.filters = new Gson().fromJson(element.toString(), listType);
        return this;
    }

    public Result header(Map<String, String> header) {
        if (header.isEmpty()) return this;
        this.header = new Gson().toJson(header);
        return this;
    }

    public Result parse() {
        this.parse = 1;
        return this;
    }

    public Result parse(int parse) {
        this.parse = parse;
        return this;
    }

    public Result jx() {
        this.jx = 1;
        return this;
    }

    public Result url(String url) {
        this.url = url;
        return this;
    }

    public Result url(List<String> url) {
        this.url = url;
        return this;
    }

    public Result danmaku(String danmaku) {
        this.danmaku = danmaku;
        return this;
    }

    public Result format(String format) {
        this.format = format;
        return this;
    }

    public Result subs(List<Sub> subs) {
        this.subs = subs;
        return this;
    }

    public Result dash() {
        this.format = "application/dash+xml";
        return this;
    }

    public Result m3u8() {
        this.format = "application/x-mpegURL";
        return this;
    }

    public Result rtsp() {
        this.format = "application/x-rtsp";
        return this;
    }

    public Result octet() {
        this.format = "application/octet-stream";
        return this;
    }

    public Result page() {
        return page(1, 1, 0, 1);
    }

    public Result page(int page, int count, int limit, int total) {
        this.page = page > 0 ? page : Integer.MAX_VALUE;
        this.limit = limit > 0 ? limit : Integer.MAX_VALUE;
        this.total = total > 0 ? total : Integer.MAX_VALUE;
        this.pagecount = count > 0 ? count : Integer.MAX_VALUE;
        return this;
    }

    public List<Vod> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public Object getUrl() {
        return url;
    }

    public String string() {
        return toString();
    }

    public List<Class> getClasses() {
        return classes;
    }

    /**
     * 真实播放链接
     */
    public static class UrlBuilder{
        private List<String> urlList = new ArrayList<>();

        public UrlBuilder add(String name, String value){
            urlList.add(name);
            urlList.add(value);
            return this;
        }

        public List<String> build(){
            return urlList;
        }
    }
}
