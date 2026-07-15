package io.knifer.freebox.spider.template.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.github.catvod.crawler.spider.Spider;
import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.exception.GlobalExceptionHandler;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.*;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.converter.CatVodBeanConverter;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.spider.SpiderJarLoader;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.ValidationUtil;
import io.knifer.freebox.util.catvod.ApiConfigUtil;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import io.knifer.freebox.util.json.GsonUtil;
import jakarta.inject.Inject;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * FreeBox爬虫模板实现
 *
 * @author Knifer
 */
@Slf4j
public class FreeBoxSpiderTemplate implements SpiderTemplate {

    private final ClientManager clientManager;
    private final SpiderJarLoader spiderJarLoader;
    private final CatVodBeanConverter beanConverter;

    private FreeBoxApiConfig apiConfig;
    private List<SourceBean> sourceBeans;

    private final static ThreadPoolExecutor EXECUTOR =  new ThreadPoolExecutor(
            2,
            Runtime.getRuntime().availableProcessors(),
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);

                t.setName("FreeBoxSpiderTemplate-Thread");
                t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                return t;
            }
    );

    @Inject
    public FreeBoxSpiderTemplate(
            ClientManager clientManager, SpiderJarLoader spiderJarLoader, CatVodBeanConverter catVodBeanConverter
    ) {
        this.clientManager = clientManager;
        this.spiderJarLoader = spiderJarLoader;
        this.beanConverter = catVodBeanConverter;
        this.sourceBeans = List.of();
    }

    @Override
    public CompletableFuture<Boolean> init() {
        ClientInfo clientInfo = getClientInfo();

        return CompletableFuture.supplyAsync(() -> {
            String configUrl = clientInfo.getConfigUrl();
            String jsonVal;

            if (configUrl.startsWith("http")) {
                try {
                    jsonVal = HttpUtil.getAsync(configUrl, BaseValues.FETCH_CAT_VOD_API_CONFIG_HTTP_HEADERS)
                            .get(BaseValues.KEB_SOCKET_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                    if (StringUtils.isBlank(jsonVal)) {
                        Platform.runLater(
                                () -> ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_GET_CONFIG_FAILED)
                        );
                    } else {
                        jsonVal = ApiConfigUtil.parseApiConfigJson(jsonVal.trim());
                    }

                    return doInit(configUrl, jsonVal);
                } catch (Exception e) {
                    handleException(e);

                    return false;
                }
            } else if (configUrl.startsWith("file:///")) {

                return doInit(
                        configUrl,
                        ApiConfigUtil.parseApiConfigJson(FileUtil.readString(configUrl, Charsets.UTF_8).trim())
                );
            } else {
                Platform.runLater(
                        () -> ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_INVALID_CONFIG_URL)
                );

                return doInit(configUrl, null);
            }
        }, EXECUTOR).exceptionally(e -> {
            handleException(e);

            return false;
        });
    }

    private boolean doInit(String configUrl, String jsonVal) {
        String spiderUrl;

        try {
            apiConfig = GsonUtil.fromJson(jsonVal, FreeBoxApiConfig.class);
        } catch (JsonSyntaxException e) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("load api config error", e);

            return false;
        }
        if (apiConfig == null) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("api config is null");

            return false;
        }
        apiConfig.setUrl(configUrl);
        spiderUrl = apiConfig.getSpider();
        if (
                !StringUtils.startsWith(spiderUrl, "./") &&
                        !StringUtils.startsWith(spiderUrl, "../") &&
                        !ValidationUtil.isURL(spiderUrl)
        ) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("load api config error, spider url invalid");

            return false;
        }
        spiderJarLoader.setApiConfig(apiConfig);
        sourceBeans = apiConfig.getSites()
                .stream()
                .map(FreeBoxSourceBean::toSourceBean)
                .toList();
        log.info("load api config success");

        return true;
    }

    @Override
    public void destroy() {
        EXECUTOR.shutdownNow();
        spiderJarLoader.destroy();
    }

    @Override
    public CompletableFuture<List<SourceBean>> getSourceBeanList() {
        return CompletableFuture.completedFuture(sourceBeans)
                .exceptionally(e -> {
                    handleException(e);

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<AbsSortXml> getHomeContent(SourceBean sourceBean) {
        return CompletableFuture.supplyAsync(() -> {
            Object spider = getSpider(sourceBean);
            Result result = GsonUtil.fromJson(SpiderInvokeUtil.homeContent(spider, false), Result.class);

            log.info("getHomeContent: {}", result);

            return result == null ? null : beanConverter.resultToAbsSortXml(result, sourceBean.getKey());
        }, EXECUTOR).exceptionally(e -> {
            handleException(e);

            return null;
        });
    }

    @Override
    public CompletableFuture<AbsXml> getCategoryContent(GetCategoryContentDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            String sourceKey = dto.getSourceKey();
            Object spider = getSpider(sourceKey);
            HashMap<String, String> filterSelect = dto.getExtend();
            boolean filter = !filterSelect.isEmpty();
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.categoryContent(
                            spider, dto.getTid(), dto.getPage(), filter, filterSelect
                    ),
                    Result.class
            );

            log.info("getCategoryContent: {}", result);

            return result == null ? null : beanConverter.resultToAbsXml(result, sourceKey);
        }, EXECUTOR).exceptionally(e -> {
            handleException(e);

            return null;
        });
    }

    @Override
    public CompletableFuture<AbsXml> getDetailContent(GetDetailContentDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            Object spider = getSpider(dto.getSourceKey());
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.detailContent(spider, List.of(dto.getVodId())), Result.class
            );

            log.info("getDetailContent: {}", result);

            return result == null ? null : beanConverter.resultToAbsXml(result, dto.getSourceKey());
        }, EXECUTOR).exceptionally(e -> {
            handleException(e);

            return null;
        });
    }

    @Override
    public CompletableFuture<JsonObject> getPlayerContent(GetPlayerContentDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            Object spider = getSpider(dto.getSourceKey());
            JsonObject sourceResult = GsonUtil.fromJson(
                    SpiderInvokeUtil.playerContent(spider, dto.getPlayFlag(), dto.getVodId(), List.of()),
                    JsonObject.class
            );
            JsonObject result;

            log.info("getPlayerContent: {}", sourceResult);
            if (sourceResult == null) {

                return null;
            }
            result = new JsonObject();
            result.add("nameValuePairs", sourceResult);

            return result;
        }, EXECUTOR).exceptionally(e -> {
            handleException(e);

            return null;
        });
    }

    @Override
    public CompletableFuture<List<VodInfo>> getPlayHistory(GetPlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.supplyAsync(() -> {
                    MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                            .orElse(null);
                    Collection<VodInfo> result;

                    if (movieHistory == null) {
                        log.info("getPlayHistory: null");

                        return List.<VodInfo>of();
                    } else {
                        result = movieHistory.getData().values();
                        log.info("getPlayHistory: {}", result);

                        return result.isEmpty() ? List.<VodInfo>of() : new ArrayList<>(result);
                    }
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<VodInfo> getOnePlayHistory(GetOnePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.supplyAsync(() -> {
                    MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                            .orElse(null);
                    VodInfo result;

                    if (movieHistory == null) {
                        log.info("getOnePlayHistory: null");

                        return null;
                    } else {
                        result = movieHistory.getData().get(DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId()));
                        log.info("getOnePlayHistory: {}", result);

                        return result;
                    }
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<AbsXml> getSearchContent(GetSearchContentDTO dto) {
        return CompletableFuture.supplyAsync(() -> {
            Object spider = getSpider(dto.getSourceKey());
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.searchContent(spider, dto.getKeyword(), false),
                    Result.class
            );

            log.info("getSearchContent: {}", result);

            return result == null ? null : beanConverter.resultToAbsXml(result, dto.getSourceKey());
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> cancelAllSearching() {
        /* *
         * 本方法主要目的在于取消WebSocket连接的远端app的搜索任务
         * 至于非WebSocket环境下的影视搜索逻辑取消，每次搜索前调用的CommonMovieBatchSearchingHandler已经有所实现，无需在这里实现
         * */
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> savePlayHistory(SavePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.runAsync(() -> {
                    MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                            .orElse(null);
                    Map<String, VodInfo> data;
                    VodInfo vodInfo;

                    if (movieHistory == null) {
                        data = new HashMap<>();
                        movieHistory = MovieHistory.of(clientInfo.getId(), data);
                    } else {
                        data = movieHistory.getData();
                    }
                    vodInfo = VodInfo.from(dto);
                    data.put(
                            DigestUtil.md5Hex(vodInfo.getSourceKey() + vodInfo.getId()),
                            vodInfo
                    );
                    StorageHelper.save(movieHistory);
                    log.info("savePlayHistory: {}", movieHistory);
                }, EXECUTOR));
    }

    @Override
    public CompletableFuture<Void> deletePlayHistory(DeletePlayHistoryDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.runAsync(() -> {
                    MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                            .orElse(null);
                    VodInfo removed;

                    if (movieHistory == null) {

                        return;
                    }
                    removed = movieHistory.getData().remove(
                            DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId())
                    );
                    if (removed != null) {
                        StorageHelper.save(movieHistory);
                    }
                    log.info("deletePlayHistory: {}", movieHistory);
                }, EXECUTOR));
    }

    @Override
    public CompletableFuture<Void> saveMovieCollection(SaveMovieCollectionDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.runAsync(() -> {
                    MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                            .orElse(null);
                    Map<String, VodCollect> data;

                    if (movieCollection == null) {
                        data = new HashMap<>();
                        movieCollection = MovieCollection.of(clientInfo.getId(), data);
                    } else {
                        data = movieCollection.getData();
                    }
                    data.put(
                            DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId()),
                            VodCollect.from(dto)
                    );
                    StorageHelper.save(movieCollection);
                    log.info("saveMovieCollection: {}", movieCollection);
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<Void> deleteMovieCollection(DeleteMovieCollectionDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.runAsync(() -> {
                    MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                            .orElse(null);
                    VodCollect removed;

                    if (movieCollection == null) {

                        return;
                    }
                    removed = movieCollection.getData().remove(
                            DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId())
                    );
                    if (removed != null) {
                        StorageHelper.save(movieCollection);
                    }
                    log.info("deleteMovieCollection: {}", movieCollection);
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return null;
                });
    }

    @Override
    public CompletableFuture<List<VodCollect>> getMovieCollection() {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.supplyAsync(() -> {
                    MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                            .orElse(null);
                    Collection<VodCollect> result;

                    if (movieCollection == null) {
                        log.info("getMovieCollection: null");

                        return List.<VodCollect>of();
                    } else {
                        result = movieCollection.getData().values();
                        log.info("getMovieCollection: {}", movieCollection);

                        return result.isEmpty() ? List.<VodCollect>of() : new ArrayList<>(result);
                    }
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return List.of();
                });
    }

    @Override
    public CompletableFuture<Boolean> getMovieCollectedStatus(GetMovieCollectedStatusDTO dto) {
        return clientManager.getCurrentClient()
                .thenCompose(clientInfo -> CompletableFuture.supplyAsync(() -> {
                    MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                            .orElse(null);
                    boolean flag;

                    if (movieCollection == null) {
                        log.info("getMovieCollectedStatus: false");

                        return false;
                    } else {
                        flag = movieCollection.getData().containsKey(
                                DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId())
                        );
                        log.info("getMovieCollectedStatus: {}", flag);

                        return flag;
                    }
                }, EXECUTOR))
                .exceptionally(e -> {
                    handleException(e);

                    return false;
                });
    }

    private ClientInfo getClientInfo() {
        ClientInfo clientInfo = clientManager.getCurrentClientImmediately();

        if (clientInfo == null) {
            throw new AssertionError();
        }

        return clientInfo;
    }

    private Object getSpider(String sourceKey) {
        SourceBean sourceBean = CollectionUtil.findFirst(
                sourceBeans,
                bean -> bean.getKey().equals(sourceKey)
        ).orElseThrow(
                () -> new FBException("No source bean found for key: " + sourceKey)
        );

        return getSpider(sourceBean);
    }

    private Object getSpider(SourceBean sourceBean) {
        String customJar = sourceBean.getJar();
        boolean hasCustomJar = StringUtils.isNotBlank(customJar);

        if (
                hasCustomJar &&
                !StringUtils.startsWith(customJar, "./") &&
                !StringUtils.startsWith(customJar, "../") &&
                !ValidationUtil.isURL(customJar)
        ) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("load site custom spider error, spider url invalid");

            return Spider.getEmpty();
        }

        return spiderJarLoader.getSpider(
                sourceBean.getKey(),
                sourceBean.getApi(),
                sourceBean.getExt(),
                hasCustomJar ? customJar : apiConfig.getSpider()
        );
    }

    @Override
    public CompletableFuture<List<FreeBoxLive>> getLives() {
        return CompletableFuture.completedFuture(ObjectUtils.defaultIfNull(apiConfig.getLives(), List.of()));
    }

    @Override
    public void proxy(Consumer<Object[]> callback, Map<String, String> params) {
        EXECUTOR.execute(() -> SpiderTemplate.super.proxy(callback, params));
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
