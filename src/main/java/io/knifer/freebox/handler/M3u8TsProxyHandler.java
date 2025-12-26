package io.knifer.freebox.handler;

import org.apache.commons.lang3.tuple.Pair;

/**
 * m3u8 ts代理处理器
 *
 * @author Knifer
 */
public interface M3u8TsProxyHandler {

    /**
     * 处理m3u8 ts代理
     * @param m3u8Url m3u8源地址（用于拼接绝对路径）
     * @param content m3u8内容
     * @param proxyUrlPrefix 代理地址前缀
     * @return 是否成功处理, 成功处理后的m3u8内容
     */
    Pair<Boolean, String> handle(String m3u8Url, String content, String proxyUrlPrefix);
}
