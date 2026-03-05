package com.fongmi.quickjs.bean;


import cn.hutool.core.codec.Base64;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import io.knifer.freebox.util.json.GsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Res {

    private Integer code;
    private Integer buffer;
    private String content;
    private JsonElement headers;


    public int getCode() {
        return code == null ? 200 : code;
    }

    public int getBuffer() {
        return buffer == null ? 0 : buffer;
    }

    public String getContent() {
        return StringUtils.isEmpty(content) ? "" : content;
    }

    private JsonElement getHeaders() {
        return headers;
    }

    public Map<String, String> getHeader() {
        return GsonUtil.toStringMap(getHeaders());
    }

    public String getContentType() {
        Map<String, String> header = getHeader();
        List<String> keys = Arrays.asList("Content-Type", "content-type");
        String contentType = header.get(HttpHeaders.CONTENT_TYPE);

        if (contentType != null) {

            return contentType;
        }
        contentType = header.get(HttpHeaders.CONTENT_TYPE.toLowerCase());

        return contentType == null ? "application/octet-stream" : contentType;

    }

    public ByteArrayInputStream getStream() {
        if (getBuffer() == 2) {

            return new ByteArrayInputStream(Base64.decode(getContent()));
        }

        return new ByteArrayInputStream(getContent().getBytes());
    }
}
