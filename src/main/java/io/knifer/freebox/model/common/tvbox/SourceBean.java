package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

import java.util.ArrayList;

@Data
public class SourceBean {
    private String key;
    private String name;
    private String api;
    private int type;   // 0 xml 1 json 3 Spider
    private int searchable; // 是否可搜索
    private int quickSearch; // 是否可以快速搜索
    private int filterable; // 可筛选?
    private String playerUrl; // 站点解析Url
    private String ext; // 扩展数据
    private String jar; // 自定义jar
    private ArrayList<String> categories = null; // 分类&排序
    private int playerType; // 0 system 1 ikj 2 exo 10 mxplayer -1 以参数设置页面的为准
    private String clickSelector; // 需要点击播放的嗅探站点selector   ddrk.me;#id

    public boolean isSearchable() {
        return searchable == 1;
    }
}
