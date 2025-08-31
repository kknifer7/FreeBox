package io.knifer.freebox.net.aria2.impl;

import io.knifer.freebox.net.aria2.Aria2Template;

/**
 * Aria2下载器操作模板实现
 *
 * @author Knifer
 */
public class Aria2TemplateImpl implements Aria2Template {

    private final static String ARIA2_URL = "http://127.0.0.1:6800/jsonrpc";
    private final static String JSON_RPC_VERSION = "2.0";

    @Override
    public String addUri(String id, String uri) {
        /*return HttpUtil.post(
                ARIA2_URL,
                GsonUtil.toJson(Map.of(
                        "jsonrpc", JSON_RPC_VERSION,
                        "id", id,
                        "method", "aria2.addUri",
                        "params", new Object[]{new String[]{uri}}
                ))
        );*/

        return null;
    }
}
