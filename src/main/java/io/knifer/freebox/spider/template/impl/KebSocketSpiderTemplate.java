package io.knifer.freebox.spider.template.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.common.catvod.History;
import io.knifer.freebox.model.common.catvod.Keep;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.converter.CatVodBeanConverter;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.core.KebSocketRunner;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CollectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TVBox KebSocket 爬虫模板实现
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
@Singleton
public class KebSocketSpiderTemplate implements SpiderTemplate {

    private final KebSocketRunner runner;
    private final ClientManager clientManager;
    private final CatVodBeanConverter beanConverter;

    @Override
    public CompletableFuture<List<SourceBean>> getSourceBeanList() {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_SOURCE_BEAN_LIST,
                        null,
                        new TypeToken<List<SourceBean>>(){}
                ))
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException || ex instanceof ExecutionException) {
                        Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.COMMON_MESSAGE_TIMOUT_FAILED));
                    } else {
                        Platform.runLater(() -> ToastHelper.showException(ex));
                    }

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<AbsSortXml> getHomeContent(SourceBean sourceBean) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_HOME_CONTENT,
                        sourceBean,
                        new TypeToken<Result>(){}
                ))
                .thenApply(result ->
                        result == null ? null : beanConverter.resultToAbsSortXml(result, sourceBean.getKey())
                )
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<AbsXml> getCategoryContent(GetCategoryContentDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_CATEGORY_CONTENT,
                        dto,
                        new TypeToken<Result>(){}
                ))
                .thenApply(
                        result -> result == null ? null : beanConverter.resultToAbsXml(result, dto.getSourceKey())
                )
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<AbsXml> getDetailContent(GetDetailContentDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_DETAIL_CONTENT,
                        dto,
                        new TypeToken<Result>(){}
                ))
                .thenApply(
                        result -> result == null ? null : beanConverter.resultToAbsXml(result, dto.getSourceKey())
                )
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<JsonObject> getPlayerContent(GetPlayerContentDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_PLAYER_CONTENT,
                        dto,
                        new TypeToken<JsonObject>(){}
                ).thenApply(
                        result -> result == null ?
                                null : beanConverter.catVodPlayContentToTVBoxPlayContent(result, clientInfo)
                )).exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<List<VodInfo>> getPlayHistory(GetPlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_PLAY_HISTORY,
                        dto,
                        new TypeToken<List<History>>(){}
                ))
                .thenApply(
                        result -> CollectionUtil.isEmpty(result) ?
                                List.<VodInfo>of() : result.stream().map(VodInfo::from).toList()
                )
                .exceptionally(e -> {
                    handleException(e);

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<VodInfo> getOnePlayHistory(GetOnePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_ONE_PLAY_HISTORY,
                        dto,
                        new TypeToken<History>(){}
                ))
                .thenApply(
                        result -> result == null ? null : VodInfo.from(result)
                )
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<AbsXml> getSearchContent(GetSearchContentDTO dto) {
        CompletableFuture<ClientInfo> clientFuture = clientManager.getCurrentClient();
        CompletableFuture<AbsXml> result = new CompletableFuture<>();
        AtomicReference<CompletableFuture<Result>> topicFutureRef = new AtomicReference<>();

        clientFuture.whenComplete((clientInfo, clientEx) -> {
            CompletableFuture<Result> topicFuture;

            if (clientEx != null) {
                result.completeExceptionally(clientEx);

                return;
            }
            if (clientInfo == null) {
                result.complete(null);

                return;
            }
            topicFuture = runner.sendTopic(
                    clientInfo.getConnection(),
                    MessageCodes.GET_SEARCH_CONTENT,
                    dto,
                    new TypeToken<>() {}
            );
            topicFutureRef.set(topicFuture);
            topicFuture.thenApply(
                    msg -> msg == null ? null : beanConverter.resultToAbsXml(msg, dto.getSourceKey())
                    )
                    .exceptionally(throwable -> {
                        Throwable cause = throwable instanceof CompletionException ? throwable.getCause() : throwable;

                        if (cause instanceof TimeoutException) {
                            log.warn("get search content timeout", cause);
                        } else if (
                                cause instanceof CancellationException ||
                                cause instanceof InterruptedException
                        ) {
                            log.info("get search content cancelled", cause);
                        } else {
                            Platform.runLater(() -> ToastHelper.showException(cause));
                        }

                        return null;
                    })
                    .whenComplete((absXml, ex) -> {
                        if (ex != null) {
                            result.completeExceptionally(ex);
                        } else {
                            result.complete(absXml);
                        }
                    });
        });

        result.whenComplete((absXml, ex) -> {
            CompletableFuture<Result> topicFuture;

            if (ex instanceof CancellationException) {
                clientFuture.cancel(true);
                topicFuture = topicFutureRef.get();
                if (topicFuture != null) {
                    topicFuture.cancel(true);
                }
            }
        });

        return result;
    }

    @Override
    public CompletableFuture<Void> savePlayHistory(SavePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenAccept(clientInfo -> runner.send(
                        clientInfo.getConnection(),
                        MessageCodes.SAVE_PLAY_HISTORY,
                        dto
                ));
    }

    @Override
    public CompletableFuture<Void> deletePlayHistory(DeletePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.DELETE_PLAY_HISTORY,
                        dto,
                        new TypeToken<Void>(){}
                ))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> saveMovieCollection(SaveMovieCollectionDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.SAVE_MOVIE_COLLECTION,
                        dto,
                        new TypeToken<Void>(){}
                ))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> deleteMovieCollection(DeleteMovieCollectionDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.DELETE_MOVIE_COLLECTION,
                        dto,
                        new TypeToken<Void>(){}
                ))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<List<VodCollect>> getMovieCollection() {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_MOVIE_COLLECTION,
                        null,
                        new TypeToken<List<Keep>>(){}
                ))
                .thenApply(
                        result -> CollectionUtil.isEmpty(result) ?
                                List.<VodCollect>of() : result.stream().map(VodCollect::from).toList()
                )
                .exceptionally(e -> {
                    handleException(e);

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<Boolean> getMovieCollectedStatus(GetMovieCollectedStatusDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_MOVIE_COLLECTED_STATUS,
                        dto,
                        new TypeToken<Boolean>(){}
                ))
                .exceptionally(e -> {
                    handleException(e);

                    return false;
                });
    }

    @Override
    public CompletableFuture<List<FreeBoxLive>> getLives() {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> runner.sendTopic(
                        clientInfo.getConnection(),
                        MessageCodes.GET_LIVES,
                        null,
                        new TypeToken<List<FreeBoxLive>>(){}
                ))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    private void handleException(Throwable e) {
        if (e instanceof ExecutionException || e instanceof TimeoutException) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.COMMON_MESSAGE_TIMOUT_FAILED));
        } else if (e instanceof InterruptedException) {
            log.info("interrupted");
        } else {
            Platform.runLater(() -> ToastHelper.showException(e));
        }
    }
}
