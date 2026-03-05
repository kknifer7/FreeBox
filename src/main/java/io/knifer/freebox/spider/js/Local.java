package io.knifer.freebox.spider.js;

import io.knifer.freebox.helper.CacheHelper;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.HostAccess;

/**
 * JS本地缓存
 * 参考：<a href="https://github.com/FongMi/TV/blob/release/quickjs/src/main/java/com/fongmi/quickjs/method/Local.java">FongMi TV</a>
 */
public class Local {

    private String getKey(String rule, String key) {
        return "cache_" + (StringUtils.isEmpty(rule) ? "" : rule + "_") + key;
    }

    @HostAccess.Export
    public String get(String rule, String key) {
        return CacheHelper.get(getKey(rule, key));
    }

    @HostAccess.Export
    public void set(String rule, String key, String value) {
        CacheHelper.put(getKey(rule, key), value);
    }

    @HostAccess.Export
    public void delete(String rule, String key) {
        CacheHelper.del(getKey(rule, key));
    }
}
