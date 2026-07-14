package io.knifer.freebox.spider.template;

import com.google.gson.JsonObject;
import io.knifer.freebox.ioc.IOC;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.common.tvbox.*;
import io.knifer.freebox.model.s2c.*;
import io.knifer.freebox.spider.SpiderJarLoader;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Spider模板接口
 *
 * @author Knifer
 */
public interface SpiderTemplate {

    default CompletableFuture<Boolean> init() {
        return CompletableFuture.completedFuture(true);
    }

    default void destroy() {}

    /**
     * 获取源列表
     */
    CompletableFuture<List<SourceBean>> getSourceBeanList();

    /**
     * 获取首页内容
     * @param sourceBean 源
     */
    CompletableFuture<AbsSortXml> getHomeContent(SourceBean sourceBean);

    /**
     * 获取指定分类信息
     * @param dto 参数
     */
    CompletableFuture<AbsXml> getCategoryContent(GetCategoryContentDTO dto);

    /**
     * 获取影视详情内容
     * @param dto 参数
     */
    CompletableFuture<AbsXml> getDetailContent(GetDetailContentDTO dto);

    /**
     * 获取播放信息
     * @param dto 参数
     */
    CompletableFuture<JsonObject> getPlayerContent(GetPlayerContentDTO dto);

    /**
     * 获取历史记录
     * @param dto 参数
     */
    CompletableFuture<List<VodInfo>> getPlayHistory(GetPlayHistoryDTO dto);

    /**
     * 获取一条历史记录
     * @param dto 参数
     */
    CompletableFuture<VodInfo> getOnePlayHistory(GetOnePlayHistoryDTO dto);

    /**
     * 影视搜索
     * @param dto 参数
     */
    CompletableFuture<AbsXml> getSearchContent(GetSearchContentDTO dto);

    /**
     * 保存历史记录
     * @param dto 参数
     */
    CompletableFuture<Void> savePlayHistory(SavePlayHistoryDTO dto);

    /**
     * 删除历史记录
     * @param dto 参数
     */
    CompletableFuture<Void> deletePlayHistory(DeletePlayHistoryDTO dto);

    /**
     * 收藏影片
     * @param dto 参数
     */
    CompletableFuture<Void> saveMovieCollection(SaveMovieCollectionDTO dto);

    /**
     * 取消收藏
     * @param dto 参数
     */
    CompletableFuture<Void> deleteMovieCollection(DeleteMovieCollectionDTO dto);

    /**
     * 获取收藏列表
     */
    CompletableFuture<List<VodCollect>> getMovieCollection();

    /**
     * 获取影视收藏状态
     * @param dto 参数
     */
    CompletableFuture<Boolean> getMovieCollectedStatus(GetMovieCollectedStatusDTO dto);

    /**
     * 获取直播配置列表
     */
    CompletableFuture<List<FreeBoxLive>> getLives();

    /**
     * 代理播放请求
     * @param callback 回调
     * @param params 参数
     */
    default void proxy(Consumer<Object[]> callback, Map<String, String> params) {
        Object spider = IOC.getBean(SpiderJarLoader.class).getSpider();

        if (spider == null) {
            callback.accept(ArrayUtils.EMPTY_OBJECT_ARRAY);
        } else if (spider instanceof JSSpider jsSpider){
            try {
                callback.accept(jsSpider.proxy(params));
            } catch (Exception ignored) {
                callback.accept(ArrayUtils.EMPTY_OBJECT_ARRAY);
            }
        } else {
            callback.accept(SpiderInvokeUtil.proxyLocal(spider, params));
        }
    }
}
