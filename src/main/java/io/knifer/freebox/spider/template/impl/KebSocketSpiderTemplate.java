package io.knifer.freebox.spider.template.impl;

import cn.hutool.core.net.Ipv4Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.CastUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * TVBox KebSocket 爬虫模板实现
 *
 * @author Knifer
 */
@Slf4j
public class KebSocketSpiderTemplate implements SpiderTemplate {

    private final KebSocketRunner runner;

    private final ClientManager clientManager;

    private final Set<Class<? extends Throwable>> ignoringToastThrowableClassesInMovieSearching =
            Set.of(TimeoutException.class);

    public KebSocketSpiderTemplate(KebSocketRunner runner, ClientManager clientManager) {
        this.runner = runner;
        this.clientManager = clientManager;
    }

    @Override
    public void getSourceBeanList(Consumer<List<SourceBean>> callback) {
        execute(
                MessageCodes.GET_SOURCE_BEAN_LIST,
                null,
                new TypeToken<List<SourceBean>>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getHomeContent(SourceBean sourceBean, Consumer<AbsSortXml> callback) {
        execute(
                MessageCodes.GET_HOME_CONTENT,
                sourceBean,
                new TypeToken<AbsSortXml>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getCategoryContent(GetCategoryContentDTO dto, Consumer<AbsXml> callback) {
        execute(
                MessageCodes.GET_CATEGORY_CONTENT,
                dto,
                new TypeToken<AbsXml>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getDetailContent(GetDetailContentDTO dto, Consumer<AbsXml> callback) {
        execute(
                MessageCodes.GET_DETAIL_CONTENT,
                dto,
                new TypeToken<AbsXml>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getPlayerContent(GetPlayerContentDTO dto, Consumer<JsonObject> callback) {
        execute(
                MessageCodes.GET_PLAYER_CONTENT,
                dto,
                new TypeToken<JsonObject>(){},
                msg -> {
                    JsonElement jsonElem;
                    JsonObject jsonObject;
                    String url;
                    ClientInfo clientInfo;

                    if (msg != null && msg.has("nameValuePairs")) {
                        jsonElem = msg.getAsJsonObject("nameValuePairs");
                        if (jsonElem.isJsonObject()) {
                            jsonObject = jsonElem.getAsJsonObject();
                            if (jsonObject.has("url")) {
                                clientInfo = clientManager.getCurrentClientImmediately();
                                if (clientInfo == null) {
                                    throw new AssertionError();
                                }
                                url = jsonObject.get("url").getAsString();
                                url = RegExUtils.replaceAll(
                                        url,
                                        Ipv4Util.LOCAL_IP,
                                        clientInfo.getConnection().getRemoteSocketAddress().getHostName()
                                );
                                jsonObject.addProperty("url", url);
                            }
                        }
                    }
                    callback.accept(CastUtil.cast(msg));
                }
        );
    }

    @Override
    public void getPlayHistory(GetPlayHistoryDTO dto, Consumer<List<VodInfo>> callback) {
        execute(
                MessageCodes.GET_PLAY_HISTORY,
                dto,
                new TypeToken<List<VodInfo>>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getOnePlayHistory(GetOnePlayHistoryDTO dto, Consumer<VodInfo> callback) {
        execute(
                MessageCodes.GET_ONE_PLAY_HISTORY,
                dto,
                new TypeToken<VodInfo>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getSearchContent(GetSearchContentDTO dto, Consumer<AbsXml> callback) {
        execute(
                MessageCodes.GET_SEARCH_CONTENT,
                dto,
                new TypeToken<AbsXml>(){},
                msg -> callback.accept(CastUtil.cast(msg)),
                ignoringToastThrowableClassesInMovieSearching
        );
    }

    @Override
    public <T extends RuntimeException> void savePlayHistory(
            SavePlayHistoryDTO dto, Consumer<T> onError
    ) {
        FutureWaitingService<ClientInfo> service = new FutureWaitingService<>(clientManager.getCurrentClient());

        service.setOnSucceeded(evt -> {
            ClientInfo clientInfo = service.getValue();

            if (clientInfo == null) {
                return;
            }
            try {
                runner.send(
                        clientInfo.getConnection(),
                        MessageCodes.SAVE_PLAY_HISTORY,
                        dto
                );
            } catch (RuntimeException e) {
                onError.accept(CastUtil.cast(e));
            }
        });
        service.start();
    }

    @Override
    public void deletePlayHistory(DeletePlayHistoryDTO dto, Runnable callback) {
        execute(
                MessageCodes.DELETE_PLAY_HISTORY,
                dto,
                new TypeToken<Void>(){},
                msg -> callback.run()
        );
    }

    @Override
    public void saveMovieCollection(
            SaveMovieCollectionDTO dto, Runnable callback
    ) {
        execute(
                MessageCodes.SAVE_MOVIE_COLLECTION,
                dto,
                new TypeToken<Void>(){},
                ignored -> callback.run()
        );
    }

    @Override
    public void deleteMovieCollection(
            DeleteMovieCollectionDTO dto, Runnable callback
    ) {

        execute(
                MessageCodes.DELETE_MOVIE_COLLECTION,
                dto,
                new TypeToken<Void>(){},
                ignored -> callback.run()
        );
    }

    @Override
    public void getMovieCollection(Consumer<List<VodCollect>> callback) {
        execute(
                MessageCodes.GET_MOVIE_COLLECTION,
                null,
                new TypeToken<List<VodCollect>>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getMovieCollectedStatus(GetMovieCollectedStatusDTO dto, Consumer<Boolean> callback) {
        execute(
                MessageCodes.GET_MOVIE_COLLECTED_STATUS,
                dto,
                new TypeToken<Boolean>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    @Override
    public void getLives(Consumer<List<FreeBoxLive>> callback) {
        execute(
                MessageCodes.GET_LIVES,
                null,
                new TypeToken<List<FreeBoxLive>>(){},
                msg -> callback.accept(CastUtil.cast(msg))
        );
    }

    private <T, R> void execute(
            int messageCode,
            T data,
            TypeToken<R> typeToken,
            Consumer<R> callback
    ) {
        FutureWaitingService<ClientInfo> service = new FutureWaitingService<>(
                clientManager.getCurrentClient(), false
        );

        dealWithClientInfoFutureWaitingService(
                service,
                messageCode,
                data,
                typeToken,
                callback,
                null
        );
    }

    private <T, R> void execute(
            int messageCode,
            T data,
            TypeToken<R> typeToken,
            Consumer<R> callback,
            Set<Class<? extends Throwable>> ignoringToastThrowableClasses
    ) {
        FutureWaitingService<ClientInfo> service = new FutureWaitingService<>(
                clientManager.getCurrentClient(), false
        );

        dealWithClientInfoFutureWaitingService(
                service,
                messageCode,
                data,
                typeToken,
                callback,
                ignoringToastThrowableClasses
        );
    }

    private <T, R> void dealWithClientInfoFutureWaitingService(
            FutureWaitingService<ClientInfo> service,
            int messageCode,
            T data,
            TypeToken<R> typeToken,
            Consumer<R> callback,
            @Nullable Set<Class<? extends Throwable>> ignoringToastThrowableClasses
    ) {
        service.setOnSucceeded(evt -> {
            ClientInfo clientInfo = service.getValue();
            FutureWaitingService<R> processService;

            if (clientInfo == null) {
                return;
            }
            if (ignoringToastThrowableClasses == null) {
                processService = new FutureWaitingService<>(
                        runner.sendTopic(
                                clientInfo.getConnection(),
                                messageCode,
                                data,
                                typeToken
                        )
                );
            } else {
                processService = new FutureWaitingService<>(
                        runner.sendTopic(
                                clientInfo.getConnection(),
                                messageCode,
                                data,
                                typeToken
                        ),
                        ignoringToastThrowableClasses
                );
            }
            processService.setOnSucceeded(event -> callback.accept(processService.getValue()));
            processService.start();
        });
        service.start();
    }
}
