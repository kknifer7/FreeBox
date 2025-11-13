package com.github.catvod.spider;

import okhttp3.Dns;
import okhttp3.OkHttpClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Spider {
    public void init() throws Exception {

    }

    public void init(String extend) throws Exception {
        init();
    }

    public String homeContent(boolean filter) throws Exception {
        return "{}";
    }

    public String homeVideoContent() throws Exception {
        return "{}";
    }

    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        return "{}";
    }

    public String detailContent(List<String> ids) throws Exception {
        return "{}";
    }

    public String searchContent(String key, boolean quick) throws Exception {
        return "{}";
    }

    public String searchContent(String key, boolean quick, String pg) throws Exception {
        return "{}";
    }

    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return "{}";
    }

    public boolean manualVideoCheck() throws Exception {
        return false;
    }

    public boolean isVideoFormat(String url) throws Exception {
        return false;
    }

    public Object[] proxyLocal(Map<String, String> params) throws Exception {
        return new Object[0];
    }

    public void destroy() {

    }

    public static Dns safeDns() {
        return Dns.SYSTEM;
    }

    public OkHttpClient client() {
        return null;
    }
}