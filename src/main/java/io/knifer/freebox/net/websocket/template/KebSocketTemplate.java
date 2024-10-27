package io.knifer.freebox.net.websocket.template;

import com.google.gson.JsonObject;
import io.knifer.freebox.model.common.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * TV服务
 *
 * @author Knifer
 */
public interface KebSocketTemplate {

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

    /**
     * 获取指定分类信息
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getCategoryContent(ClientInfo clientInfo, GetCategoryContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 获取影视详情内容
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getDetailContent(ClientInfo clientInfo, GetDetailContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 获取播放信息
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getPlayerContent(ClientInfo clientInfo, GetPlayerContentDTO dto, Consumer<JsonObject> callback);

    /**
     * 获取历史记录
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getPlayHistory(ClientInfo clientInfo, GetPlayHistoryDTO dto, Consumer<List<VodInfo>> callback);

    /**
     * 获取一条历史记录
     * @param clientInfo 客户端信息
     * @param dto 参数
     */
    void getOnePlayHistory(ClientInfo clientInfo, GetOnePlayHistoryDTO dto, Consumer<VodInfo> callback);

    /**
     * 影视搜索
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getSearchContent(ClientInfo clientInfo, GetSearchContentDTO dto, Consumer<AbsXml> callback);

    /**
     * 保存历史记录
     * @param clientInfo 客户端信息
     * @param dto 参数
     */
    void savePlayHistory(ClientInfo clientInfo, SavePlayHistoryDTO dto);

    /**
     * 删除历史记录
     * @param clientInfo 客户端信息
     * @param dto 参数
     */
    void deletePlayHistory(ClientInfo clientInfo, DeletePlayHistoryDTO dto, Runnable callback);

    /**
     * 收藏影片
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void saveMovieCollection(ClientInfo clientInfo, SaveMovieCollectionDTO dto, Runnable callback);

    /**
     * 取消收藏
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void deleteMovieCollection(ClientInfo clientInfo, DeleteMovieCollectionDTO dto, Runnable callback);

    /**
     * 获取收藏列表
     * @param clientInfo 客户端信息
     * @param callback 回调
     */
    void getMovieCollection(ClientInfo clientInfo, Consumer<List<VodCollect>> callback);

    /**
     * 获取影视收藏状态
     * @param clientInfo 客户端信息
     * @param dto 参数
     * @param callback 回调
     */
    void getMovieCollectedStatus(ClientInfo clientInfo, GetMovieCollectedStatusDTO dto, Consumer<Boolean> callback);
}
