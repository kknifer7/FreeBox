package io.knifer.freebox.net.websocket.template.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.model.common.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.CastUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * TV服务实现
 *
 * @author Knifer
 */
@Slf4j
public class KebSocketTemplateImpl implements KebSocketTemplate {

    private KebSocketTemplateImpl(KebSocketRunner runner) {
        this.runner = runner;
    }

    private final KebSocketRunner runner;

    private final Set<Class<? extends Throwable>> ignoringToastThrowableClassesInMovieSearching =
            Set.of(TimeoutException.class);

    private static final KebSocketTemplateImpl INSTANCE = new KebSocketTemplateImpl(KebSocketRunner.getInstance());

    public static KebSocketTemplate getInstance() {
        return INSTANCE;
    }

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

    @Override
    public void getCategoryContent(ClientInfo clientInfo, GetCategoryContentDTO dto, Consumer<AbsXml> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_CATEGORY_CONTENT,
                        dto,
                        new TypeToken<AbsXml>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getDetailContent(ClientInfo clientInfo, GetDetailContentDTO dto, Consumer<AbsXml> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_DETAIL_CONTENT,
                        dto,
                        new TypeToken<AbsXml>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getPlayerContent(ClientInfo clientInfo, GetPlayerContentDTO dto, Consumer<JsonObject> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_PLAYER_CONTENT,
                        dto,
                        new TypeToken<JsonObject>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getPlayHistory(ClientInfo clientInfo, GetPlayHistoryDTO dto, Consumer<List<VodInfo>> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_PLAY_HISTORY,
                        dto,
                        new TypeToken<List<VodInfo>>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getOnePlayHistory(ClientInfo clientInfo, GetOnePlayHistoryDTO dto, Consumer<VodInfo> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_ONE_PLAY_HISTORY,
                        dto,
                        new TypeToken<VodInfo>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getSearchContent(
            ClientInfo clientInfo, GetSearchContentDTO dto, Consumer<AbsXml> callback
    ) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_SEARCH_CONTENT,
                        dto,
                        new TypeToken<AbsXml>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg)),
                ignoringToastThrowableClassesInMovieSearching
        );
    }

    @Override
    public <T extends RuntimeException> void savePlayHistory(
            ClientInfo clientInfo, SavePlayHistoryDTO dto, Consumer<T> onError
    ) {
        try {
            runner.send(
                    clientInfo.getConnection(),
                    MessageCodes.SAVE_PLAY_HISTORY,
                    dto
            );
        } catch (RuntimeException e) {
            onError.accept(CastUtil.cast(e));
        }
    }

    @Override
    public void deletePlayHistory(ClientInfo clientInfo, DeletePlayHistoryDTO dto, Runnable callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.DELETE_PLAY_HISTORY,
                        dto,
                        new TypeToken<Void>(){}
                ),
                msg -> callback.run()
        );
    }

    @Override
    public void saveMovieCollection(
            ClientInfo clientInfo, SaveMovieCollectionDTO dto, Runnable callback
    ) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.SAVE_MOVIE_COLLECTION,
                        dto,
                        new TypeToken<Void>(){}
                ),
                ignored -> callback.run()
        );
    }

    @Override
    public void deleteMovieCollection(
            ClientInfo clientInfo, DeleteMovieCollectionDTO dto, Runnable callback
    ) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.DELETE_MOVIE_COLLECTION,
                        dto,
                        new TypeToken<Void>(){}
                ),
                ignored -> callback.run()
        );
    }

    @Override
    public void getMovieCollection(ClientInfo clientInfo, Consumer<List<VodCollect>> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_MOVIE_COLLECTION,
                        null,
                        new TypeToken<List<VodCollect>>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getMovieCollectedStatus(ClientInfo clientInfo, GetMovieCollectedStatusDTO dto, Consumer<Boolean> callback) {
        execute(
                runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_MOVIE_COLLECTED_STATUS,
                        dto,
                        new TypeToken<Boolean>(){}
                ),
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    private <T> void execute(Future<T> future, Consumer<T> callback) {
        FutureWaitingService<T> service = new FutureWaitingService<>(future);

        service.setOnSucceeded(event -> callback.accept(service.getValue()));
        service.start();
    }

    private <T> void execute(
            Future<T> future, Consumer<T> callback, Set<Class<? extends Throwable>> ignoringToastThrowableClasses
    ) {
        FutureWaitingService<T> service = new FutureWaitingService<>(future, ignoringToastThrowableClasses);

        service.setOnSucceeded(event -> callback.accept(service.getValue()));
        service.start();
    }
}
