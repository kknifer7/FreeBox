package io.knifer.freebox.service;

import io.knifer.freebox.model.common.AbsXml;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.s2c.GetSearchContentDTO;
import io.knifer.freebox.net.websocket.template.impl.KebSocketTemplateImpl;
import io.knifer.freebox.util.CollectionUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 检测端口是否被占用服务
 * TODO 待改造（优化取消逻辑）
 *
 * @author Knifer
 */
@Setter
@Getter
@RequiredArgsConstructor
public class MovieSearchService extends Service<Void> {

    /**
     * 客户端信息
     */
    private final ClientInfo clientInfo;

    /**
     * 源ID列表
     */
    private Iterator<String> sourceKeyIterator;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 数据处理回调（数据为 关键字-搜索结果 键值对）
     */
    private final Consumer<Pair<String, AbsXml>> callback;

    /**
     * 结束回调
     */
    private final Runnable endCallback;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                search(keyword);

                return null;
            }

            private void search(String keyword) {
                String sourceKey;

                if (isCancelled()) {

                    return;
                }
                if (!sourceKeyIterator.hasNext()) {
                    endCallback.run();

                    return;
                }
                sourceKey = sourceKeyIterator.next();
                KebSocketTemplateImpl.getInstance().getSearchContent(
                        clientInfo,
                        GetSearchContentDTO.of(sourceKey, keyword),
                        searchContent -> {
                            List<Movie.Video> videos;

                            if (isCancelled()) {
                                // 这里的检测取消是无效的，并没有进行取消操作，同时经测试，取消Service或者Task也无法成功终止任务，待优化

                                return;
                            }
                            if (searchContent == null) {
                                search(keyword);

                                return;
                            }
                            videos = searchContent.getMovie().getVideoList();
                            if (CollectionUtil.isNotEmpty(videos)) {
                                callback.accept(Pair.of(keyword, searchContent));
                            }
                            search(keyword);
                        }
                );
            }
        };
    }
}
