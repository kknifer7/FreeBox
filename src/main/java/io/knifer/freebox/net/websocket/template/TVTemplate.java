package io.knifer.freebox.net.websocket.template;

import io.knifer.freebox.model.common.AbsSortXml;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;

import java.util.List;
import java.util.function.Consumer;

/**
 * TV服务
 *
 * @author Knifer
 */
public interface TVTemplate {

    /**
     * 获取源列表
     * @param clientInfo 客户端信息
     * @param callback 回调
     */
    void getSourceBeanList(ClientInfo clientInfo, Consumer<List<SourceBean>> callback);

    /**
     * 获取首页内容
     * @param clientInfo 客户端信息
     * @param sourceBean 源
     * @param callback 回调
     */
    void getHomeContent(ClientInfo clientInfo, SourceBean sourceBean, Consumer<AbsSortXml> callback);
}
