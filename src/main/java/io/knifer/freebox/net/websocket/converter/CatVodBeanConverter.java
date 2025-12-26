package io.knifer.freebox.net.websocket.converter;

import cn.hutool.core.net.Ipv4Util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.knifer.freebox.model.common.catvod.*;
import io.knifer.freebox.model.common.catvod.Class;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.util.CollectionUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 猫影视规则 bean转换器
 * 负责将猫影视规则的json数据转换成与FreeBox兼容的TVBoxOSC数据
 *
 * @author Knifer
 */
public class CatVodBeanConverter {

    private final static CatVodBeanConverter INSTANCE = new CatVodBeanConverter();

    public static CatVodBeanConverter getInstance() {
        return INSTANCE;
    }

    public MovieSort classesToMovieSort(List<Class> classes, Map<String, List<Filter>> filters) {
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
                                                        Collectors.toMap(
                                                                Filter.Value::getN,
                                                                Filter.Value::getV,
                                                                (v1, v2) -> v2
                                                        )
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

    public AbsXml resultToAbsXml(Result result, String sourceKey) {
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

    public AbsSortXml resultToAbsSortXml(Result result, String sourceKey) {
        AbsSortXml absSortXml = new AbsSortXml();
        List<Vod> list = result.getList();
        Movie movie;

        absSortXml.setClasses(classesToMovieSort(result.getClasses(), result.getFilters()));
        if (CollectionUtil.isNotEmpty(list)) {
            movie = new Movie();
            movie.setVideoList(list.stream().map(v -> vodToVideo(v, sourceKey)).toList());
            absSortXml.setList(movie);
        }

        return absSortXml;
    }

    public Movie.Video vodToVideo(Vod vod, String sourceKey) {
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

    @Nullable
    public JsonObject catVodPlayContentToTVBoxPlayContent(JsonObject catVodPlayContent, ClientInfo clientInfo) {
        JsonElement jsonElem;
        JsonObject jsonObject;
        JsonArray jsonArray;
        JsonPrimitive jsonPrimitive;
        String url;
        JsonObject result;
        JsonObject nameValuePairsObject;

        jsonElem = catVodPlayContent.getAsJsonObject("url");
        if (jsonElem == null || !jsonElem.isJsonObject()) {

            return catVodPlayContent;
        }
        jsonObject = jsonElem.getAsJsonObject();
        jsonElem = jsonObject.get("values");
        if (jsonElem == null || !jsonElem.isJsonArray()) {

            return null;
        }
        jsonArray = jsonElem.getAsJsonArray();
        if (jsonArray.isEmpty()) {

            return null;
        }
        jsonElem = jsonObject.get("position");
        if (jsonElem == null || !jsonElem.isJsonPrimitive()) {

            return null;
        }
        jsonPrimitive = jsonElem.getAsJsonPrimitive();
        if (!jsonPrimitive.isNumber()) {

            return null;
        }
        jsonElem = jsonArray.get(jsonPrimitive.getAsInt());
        if (jsonElem == null || !jsonElem.isJsonObject()) {

            return null;
        }
        jsonObject = jsonElem.getAsJsonObject();
        jsonElem = jsonObject.get("v");
        if (jsonElem == null || !jsonElem.isJsonPrimitive()) {

            return null;
        }
        url = jsonElem.getAsString();
        if (StringUtils.isBlank(url)) {

            return null;
        }
        url = RegExUtils.replaceAll(
                url,
                Ipv4Util.LOCAL_IP,
                clientInfo.getConnection().getRemoteSocketAddress().getHostString()
        );
        result = new JsonObject();
        nameValuePairsObject = new JsonObject();
        nameValuePairsObject.addProperty("url", url);
        result.add("nameValuePairs", nameValuePairsObject);

        return result;
    }
}
