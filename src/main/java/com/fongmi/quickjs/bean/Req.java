package com.fongmi.quickjs.bean;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Data
public class Req {

    private Integer buffer;
    private Integer redirect;
    private Integer timeout;
    private String postType;
    private String method;
    private String body;
    private JsonElement data;
    private JsonElement headers;

    public int getBuffer() {
        return buffer == null ? 0 : buffer;
    }

    public Integer getRedirect() {
        return redirect == null ? 1 : redirect;
    }

    public boolean isRedirect() {
        return getRedirect() > 0;
    }

    public Integer getTimeout() {
        return timeout == null ? 10000 : timeout;
    }

    public String getPostType() {
        return StringUtils.isEmpty(postType) ? "json" : postType;
    }

    public String getMethod() {
        return StringUtils.isEmpty(method) ? "get" : method;
    }

    public Map<String, String> getHeader() {
        return GsonUtil.toStringMap(getHeaders());
    }

    public String getCharset() {
        Map<String, String> header = getHeader();
        String contentType = header.get(HttpHeaders.CONTENT_TYPE);

        if (contentType != null) {

            return getCharset(contentType);
        }
        contentType = header.get(HttpHeaders.CONTENT_TYPE.toLowerCase());
        if (contentType != null) {

            return getCharset(contentType);
        }

        return "UTF-8";
    }

    private String getCharset(String value) {
        for (String text : value.split(";")) {
            if (text.contains("charset=")) {

                return text.split("=")[1];
            }
        }

        return "UTF-8";
    }
}
