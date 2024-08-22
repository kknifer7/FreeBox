package io.knifer.freebox.net.websocket.template.impl;

import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.model.common.AbsSortXml;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.template.TVTemplate;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.CastUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * TV服务实现
 *
 * @author Knifer
 */
@Slf4j
@AllArgsConstructor
public class TVTemplateImpl implements TVTemplate {

    private final KebSocketRunner runner;

    @Override
    public void getSourceBeanList(ClientInfo clientInfo, Consumer<List<SourceBean>> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_SOURCE_BEAN_LIST,
                        null,
                        new TypeToken<List<SourceBean>>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getHomeContent(ClientInfo clientInfo, SourceBean sourceBean, Consumer<AbsSortXml> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_HOME_CONTENT,
                        sourceBean,
                        new TypeToken<AbsSortXml>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    private <T> void execute(Future<T> future, Consumer<T> callback) {
        FutureWaitingService<T> service = new FutureWaitingService<>(future);

        service.setOnSucceeded(event -> callback.accept(service.getValue()));
        service.start();
    }
}
