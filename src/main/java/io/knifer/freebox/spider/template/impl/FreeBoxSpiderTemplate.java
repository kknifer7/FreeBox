package io.knifer.freebox.spider.template.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.github.catvod.spider.Spider;
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
import io.knifer.freebox.model.common.catvod.Class;
import io.knifer.freebox.model.common.catvod.Filter;
import io.knifer.freebox.model.common.catvod.Result;
import io.knifer.freebox.model.common.catvod.Vod;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.*;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.spider.SpiderJarLoader;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.ValidationUtil;
import io.knifer.freebox.util.catvod.ApiConfigUtil;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * FreeBox爬虫模板实现
 *
 * @author Knifer
 */
@Slf4j
public class FreeBoxSpiderTemplate implements SpiderTemplate {

    private final ClientManager clientManager;
    private final SpiderJarLoader spiderJarLoader;

    private FreeBoxApiConfig apiConfig;
    private List<SourceBean> sourceBeans;

    private final static ThreadPoolExecutor EXECUTOR =  new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            r -> {
                Thread t = new Thread(r);

                t.setName("FreeBoxSpiderTemplate-Thread");
                t.setUncaughtExceptionHandler(GlobalExceptionHandler.getInstance());

                return t;
            }
    );

    public FreeBoxSpiderTemplate(ClientManager clientManager) {
        this.clientManager = clientManager;
        this.spiderJarLoader = SpiderJarLoader.getInstance();
        this.sourceBeans = List.of();
    }

    @Override
    public void init(Consumer<Boolean> callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            String configUrl = clientInfo.getConfigUrl();
            FutureWaitingService<String> service;

            if (configUrl.startsWith("http")) {
                service = new FutureWaitingService<>(HttpUtil.getAsync(
                        configUrl,
                        BaseValues.FETCH_CAT_VOD_API_CONFIG_HTTP_HEADERS
                ));
                service.setOnSucceeded(evt -> {
                    String jsonVal = service.getValue();

                    if (StringUtils.isBlank(jsonVal)) {
                        Platform.runLater(
                                () -> ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_GET_CONFIG_FAILED)
                        );
                    } else {
                        jsonVal = ApiConfigUtil.parseApiConfigJson(jsonVal.trim());
                    }
                    doInit(configUrl, jsonVal, callback);
                });
                service.start();
            } else if (configUrl.startsWith("file:///")) {
                doInit(
                        configUrl,
                        ApiConfigUtil.parseApiConfigJson(FileUtil.readString(configUrl, Charsets.UTF_8).trim()),
                        callback
                );
            } else {
                Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_INVALID_CONFIG_URL));
                doInit(configUrl, null, callback);
            }
        });
    }

    private void doInit(String configUrl, String jsonVal, Consumer<Boolean> callback) {
        String spiderUrl;

        try {
            apiConfig = GsonUtil.fromJson(jsonVal, FreeBoxApiConfig.class);
        } catch (JsonSyntaxException e) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("load api config error", e);
            callback.accept(false);

            return;
        }
        if (apiConfig == null) {
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_CONFIG_FAILED));
            log.error("api config is null");
            callback.accept(false);

            return;
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
            callback.accept(false);

            return;
        }
        spiderJarLoader.setApiConfig(apiConfig);
        sourceBeans = apiConfig.getSites()
                .stream()
                .map(FreeBoxSourceBean::toSourceBean)
                .toList();
        log.info("load api config success");
        callback.accept(true);
    }

    @Override
    public void destroy() {
        EXECUTOR.shutdownNow();
        spiderJarLoader.destroy();
    }

    @Override
    public void getSourceBeanList(Consumer<List<SourceBean>> callback) {
        callback.accept(sourceBeans);
    }

    @Override
    public void getHomeContent(SourceBean sourceBean, Consumer<AbsSortXml> callback) {
        EXECUTOR.execute(() -> {
            Object spider = getSpider(sourceBean);
            Result result = GsonUtil.fromJson(SpiderInvokeUtil.homeContent(spider, false), Result.class);

            log.info("getHomeContent: {}", result);
            callback.accept(result == null ? null : resultToAbsSortXml(result, sourceBean.getKey()));
        });
    }

    private AbsSortXml resultToAbsSortXml(Result result, String sourceKey) {
        AbsSortXml absSortXml = new AbsSortXml();
        List<Vod> list = result.getList();
        Movie movie;

        absSortXml.setClasses(resultClassToMovieSort(result.getClasses(), result.getFilters()));
        if (CollectionUtil.isNotEmpty(list)) {
            movie = new Movie();
            movie.setVideoList(list.stream().map(v -> vodToVideo(v, sourceKey)).toList());
            absSortXml.setList(movie);
        }

        return absSortXml;
    }

    private MovieSort resultClassToMovieSort(List<Class> classes, Map<String, List<Filter>> filters) {
        MovieSort movieSort = new MovieSort();
        List<MovieSort.SortData> sortList;

        if (CollectionUtil.isEmpty(classes)) {
            sortList = List.of();
        } else {
            sortList = classes.stream()
                    .map(c -> {
                        MovieSort.SortData sortData = new MovieSort.SortData();
                        String typeId = c.getTypeId();
                        List<Filter> filterList;
                        ArrayList<MovieSort.SortFilter> sortDataFilterList;

                        sortData.setId(typeId);
                        sortData.setName(c.getTypeName());
                        sortData.setFlag(c.getTypeFlag());
                        if (filters != null && !CollectionUtil.isEmpty(filterList = filters.get(typeId))) {
                            sortDataFilterList = new ArrayList<>(
                                    filterList.stream().map(f -> {
                                        MovieSort.SortFilter filter = new MovieSort.SortFilter();

                                        filter.key = f.getKey();
                                        filter.name = f.getName();
                                        filter.values = new LinkedHashMap<>(
                                                f.getValue().stream().collect(
                                                        Collectors.toMap(Filter.Value::getN, Filter.Value::getV)
                                                )
                                        );

                                        return filter;
                                    }).toList()
                            );
                            sortData.setFilters(sortDataFilterList);
                        }

                        return sortData;
                    })
                    .toList();
        }
        movieSort.setSortList(sortList);

        return movieSort;
    }

    private Movie.Video vodToVideo(Vod vod, String sourceKey) {
        Movie.Video video = new Movie.Video();
        String[] playFroms;
        String vodPlayUrl;
        String[] playUrls;
        Movie.Video.UrlBean.UrlInfo urlInfo;
        String[] eps;
        List<Movie.Video.UrlBean.UrlInfo> urlInfos = List.of();
        List<Movie.Video.UrlBean.UrlInfo.InfoBean> infoBeans;
        Movie.Video.UrlBean urlBean;

        video.setId(vod.getVodId());
        video.setName(vod.getVodName());
        video.setNote(vod.getVodRemarks());
        video.setType(vod.getTypeName());
        video.setPic(vod.getVodPic());
        video.setArea(vod.getVodArea());
        video.setYear(NumberUtils.toInt(vod.getVodYear()));
        video.setActor(vod.getVodActor());
        video.setDirector(vod.getVodDirector());
        video.setDes(vod.getVodContent());
        video.setTag(vod.getVodTag());
        video.setSourceKey(sourceKey);
        playFroms = StringUtils.split(vod.getVodPlayFrom(), "$$$");
        vodPlayUrl = vod.getVodPlayUrl();
        if (!ArrayUtils.isEmpty(playFroms) || StringUtils.isNotBlank(vodPlayUrl)) {
            playUrls = vodPlayUrl.split("\\$\\$\\$");
            if (!ArrayUtils.isEmpty(playUrls)) {
                urlInfos = new ArrayList<>(playFroms.length);
                for (int i = 0; i < playFroms.length; i++) {
                    String playUrl = ArrayUtils.get(playUrls, i);
                    String playFrom;

                    if (playUrl == null) {
                        continue;
                    }
                    playFrom = playFroms[i];
                    urlInfo = new Movie.Video.UrlBean.UrlInfo();
                    urlInfo.setFlag(playFrom);
                    urlInfo.setUrls(playUrl);
                    eps = playUrl.split("#");
                    if (ArrayUtils.isEmpty(eps)) {
                        continue;
                    }
                    infoBeans = Arrays.stream(eps)
                            .map(epStr -> {
                                String[] ep = epStr.split("\\$");

                                if (ArrayUtils.getLength(ep) != 2) {
                                    return null;
                                }

                                return new Movie.Video.UrlBean.UrlInfo.InfoBean(ep[0], ep[1]);
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    urlInfo.setBeanList(infoBeans);
                    urlInfos.add(urlInfo);
                }
            }
        }
        urlBean = new Movie.Video.UrlBean();
        urlBean.setInfoList(urlInfos);
        video.setUrlBean(urlBean);

        return video;
    }

    @Override
    public void getCategoryContent(GetCategoryContentDTO dto, Consumer<AbsXml> callback) {
        EXECUTOR.execute(() -> {
            Object spider = getSpider(dto.getSource());
            MovieSort.SortData sortData = dto.getSortData();
            HashMap<String, String> filterSelect = sortData.getFilterSelect();
            boolean filter = !filterSelect.isEmpty();
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.categoryContent(
                            spider, sortData.getId(),String.valueOf(dto.getPage()), filter, filterSelect
                    ),
                    Result.class
            );

            log.info("getCategoryContent: {}", result);
            callback.accept(result == null ? null : resultToAbsXml(result, dto.getSource().getKey()));
        });
    }

    @Override
    public void getDetailContent(GetDetailContentDTO dto, Consumer<AbsXml> callback) {
        EXECUTOR.execute(() -> {
            Object spider = getSpider(dto.getSourceKey());
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.detailContent(spider, List.of(dto.getVideoId())), Result.class
            );

            log.info("getDetailContent: {}", result);
            callback.accept(result == null ? null : resultToAbsXml(result, dto.getSourceKey()));
        });
    }

    private AbsXml resultToAbsXml(Result result, String sourceKey) {
        AbsXml absXml = new AbsXml();
        List<Vod> list = result.getList();
        Movie movie = new Movie();

        if (CollectionUtil.isEmpty(list)) {
            movie.setVideoList(List.of());
        } else {
            movie.setVideoList(list.stream().map(v -> vodToVideo(v, sourceKey)).toList());
        }
        movie.setPagecount(ObjectUtils.defaultIfNull(result.getPagecount(), Integer.MAX_VALUE));
        movie.setPagesize(ObjectUtils.defaultIfNull(result.getLimit(), movie.getVideoList().size()));
        movie.setPage(ObjectUtils.defaultIfNull(result.getPage(), 1));
        movie.setRecordcount(ObjectUtils.defaultIfNull(result.getTotal(), Integer.MAX_VALUE));
        absXml.setMovie(movie);

        return absXml;
    }

    @Override
    public void getPlayerContent(GetPlayerContentDTO dto, Consumer<JsonObject> callback) {
        EXECUTOR.execute(() -> {
            Object spider = getSpider(dto.getSourceKey());
            JsonObject sourceResult = GsonUtil.fromJson(
                    SpiderInvokeUtil.playerContent(spider, dto.getPlayFlag(), dto.getId(), List.of()),
                    JsonObject.class
            );
            JsonObject result;

            log.info("getPlayerContent: {}", sourceResult);
            if (sourceResult == null) {
                callback.accept(null);

                return;
            }
            result = new JsonObject();
            result.add("nameValuePairs", sourceResult);
            callback.accept(result);
        });
    }

    @Override
    public void getPlayHistory(GetPlayHistoryDTO dto, Consumer<List<VodInfo>> callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                    .orElse(null);
            Collection<VodInfo> result;

            if (movieHistory == null) {
                log.info("getPlayHistory: null");
                callback.accept(List.of());
            } else {
                result = movieHistory.getData().values();
                log.info("getPlayHistory: {}", result);
                callback.accept(result.isEmpty() ? List.of() : new ArrayList<>(result));
            }
        });
    }

    @Override
    public void getOnePlayHistory(GetOnePlayHistoryDTO dto, Consumer<VodInfo> callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                    .orElse(null);
            VodInfo result;

            if (movieHistory == null) {
                callback.accept(null);
                log.info("getOnePlayHistory: null");
            } else {
                result = movieHistory.getData().get(DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId()));
                log.info("getOnePlayHistory: {}", result);
                callback.accept(result);
            }
        });
    }

    @Override
    public void getSearchContent(GetSearchContentDTO dto, Consumer<AbsXml> callback) {
        EXECUTOR.execute(() -> {
            Object spider = getSpider(dto.getSourceKey());
            Result result = GsonUtil.fromJson(
                    SpiderInvokeUtil.searchContent(spider, dto.getKeyword(), false),
                    Result.class
            );

            log.info("getSearchContent: {}", result);
            callback.accept(result == null ? null : resultToAbsXml(result, dto.getSourceKey()));
        });
    }

    @Override
    public <T extends RuntimeException> void savePlayHistory(SavePlayHistoryDTO dto, Consumer<T> onError) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
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
            vodInfo = dto.getVodInfo();
            data.put(
                    DigestUtil.md5Hex(vodInfo.getSourceKey() + vodInfo.getId()),
                    vodInfo
            );
            StorageHelper.save(movieHistory);
            log.info("savePlayHistory: {}", movieHistory);
        });
    }

    @Override
    public void deletePlayHistory(DeletePlayHistoryDTO dto, Runnable callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieHistory movieHistory = StorageHelper.find(clientInfo.getId(), MovieHistory.class)
                    .orElse(null);
            VodInfo vodInfo;
            VodInfo removed;

            if (movieHistory == null) {
                callback.run();
            } else {
                vodInfo = dto.getVodInfo();
                removed = movieHistory.getData().remove(
                        DigestUtil.md5Hex(vodInfo.getSourceKey() + vodInfo.getId())
                );
                if (removed != null) {
                    StorageHelper.save(movieHistory);
                }
                callback.run();
            }
            log.info("deletePlayHistory: {}", movieHistory);
        });
    }

    @Override
    public void saveMovieCollection(SaveMovieCollectionDTO dto, Runnable callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                    .orElse(null);
            Map<String, VodCollect> data;
            VodInfo vodInfo;

            if (movieCollection == null) {
                data = new HashMap<>();
                movieCollection = MovieCollection.of(clientInfo.getId(), data);
            } else {
                data = movieCollection.getData();
            }
            vodInfo = dto.getVodInfo();
            data.put(
                    DigestUtil.md5Hex(vodInfo.getSourceKey() + vodInfo.getId()),
                    VodCollect.from(vodInfo)
            );
            StorageHelper.save(movieCollection);
            callback.run();
            log.info("saveMovieCollection: {}", movieCollection);
        });
    }

    @Override
    public void deleteMovieCollection(DeleteMovieCollectionDTO dto, Runnable callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                    .orElse(null);
            VodInfo vodInfo;
            VodCollect removed;

            if (movieCollection == null) {
                callback.run();
            } else {
                vodInfo = dto.getVodInfo();
                removed = movieCollection.getData().remove(
                        DigestUtil.md5Hex(vodInfo.getSourceKey() + vodInfo.getId())
                );
                if (removed != null) {
                    StorageHelper.save(movieCollection);
                }
                callback.run();
            }
            log.info("deleteMovieCollection: {}", movieCollection);
        });
    }

    @Override
    public void getMovieCollection(Consumer<List<VodCollect>> callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                    .orElse(null);
            Collection<VodCollect> result;

            if (movieCollection == null) {
                log.info("getMovieCollection: null");
                callback.accept(List.of());
            } else {
                result = movieCollection.getData().values();
                log.info("getMovieCollection: {}", movieCollection);
                callback.accept(result.isEmpty() ? List.of() : new ArrayList<>(result));
            }
        });
    }

    @Override
    public void getMovieCollectedStatus(GetMovieCollectedStatusDTO dto, Consumer<Boolean> callback) {
        ClientInfo clientInfo = getClientInfo();

        EXECUTOR.execute(() -> {
            MovieCollection movieCollection = StorageHelper.find(clientInfo.getId(), MovieCollection.class)
                    .orElse(null);
            boolean flag;

            if (movieCollection == null) {
                log.info("getMovieCollectedStatus: false");
                callback.accept(false);
            } else {
                flag = movieCollection.getData().containsKey(
                        DigestUtil.md5Hex(dto.getSourceKey() + dto.getVodId())
                );
                log.info("getMovieCollectedStatus: {}", flag);
                callback.accept(flag);
            }
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

            return new Spider();
        }

        return spiderJarLoader.getSpider(
                sourceBean.getKey(),
                sourceBean.getApi(),
                sourceBean.getExt(),
                hasCustomJar ? customJar : apiConfig.getSpider()
        );
    }

    @Override
    public void getLives(Consumer<List<FreeBoxLive>> callback) {
        callback.accept(ObjectUtils.defaultIfNull(apiConfig.getLives(), List.of()));
    }
}
