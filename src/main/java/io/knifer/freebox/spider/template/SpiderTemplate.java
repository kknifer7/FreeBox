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
import java.util.function.Consumer;

/**
 * Spider模板接口
 *
 * @author Knifer
 */
public interface SpiderTemplate {

    default void init(Consumer<Boolean> callback) {
        callback.accept(true);
    }

    default void destroy() {}

    /**
     * 获取源列表
     * @param callback 回调
     */
    void getSourceBeanList(Consumer<List<SourceBean>> callback);

    /**
     * 获取首页内容
     * @param sourceBean 源
     * @param callback 回调
     */
    void getHomeContent(SourceBean sourceBean, Consumer<AbsSortXml> callback);

    /**
     * 获取指定分类信息
     * @param dto 参数
     * @param callback 回调
     */
    void getCategoryContent(GetCategoryContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 获取影视详情内容
     * @param dto 参数
     * @param callback 回调
     */
    void getDetailContent(GetDetailContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 获取播放信息
     * @param dto 参数
     * @param callback 回调
     */
    void getPlayerContent(GetPlayerContentDTO dto, Consumer<JsonObject> callback);

    /**
     * 获取历史记录
     * @param dto 参数
     * @param callback 回调
     */
    void getPlayHistory(GetPlayHistoryDTO dto, Consumer<List<VodInfo>> callback);

    /**
     * 获取一条历史记录
     * @param dto 参数
     */
    void getOnePlayHistory(GetOnePlayHistoryDTO dto, Consumer<VodInfo> callback);

    /**
     * 影视搜索
     * @param dto 参数
     * @param callback 回调
     */
    void getSearchContent(GetSearchContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 保存历史记录
     * @param dto 参数
     */
    <T extends RuntimeException> void savePlayHistory(SavePlayHistoryDTO dto, Consumer<T> onError);

    /**
     * 删除历史记录
     * @param dto 参数
     */
    void deletePlayHistory(DeletePlayHistoryDTO dto, Runnable callback);

    /**
     * 收藏影片
     * @param dto 参数
     * @param callback 回调
     */
    void saveMovieCollection(SaveMovieCollectionDTO dto, Runnable callback);

    /**
     * 取消收藏
     * @param dto 参数
     * @param callback 回调
     */
    void deleteMovieCollection(DeleteMovieCollectionDTO dto, Runnable callback);

    /**
     * 获取收藏列表
     * @param callback 回调
     */
    void getMovieCollection(Consumer<List<VodCollect>> callback);

    /**
     * 获取影视收藏状态
     * @param dto 参数
     * @param callback 回调
     */
    void getMovieCollectedStatus(GetMovieCollectedStatusDTO dto, Consumer<Boolean> callback);

    /**
     * 获取直播配置列表
     */
    void getLives(Consumer<List<FreeBoxLive>> callback);

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
