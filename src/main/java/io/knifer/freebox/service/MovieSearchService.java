package io.knifer.freebox.service;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.s2c.GetSearchContentDTO;
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
 * 影视搜索服务
 *
 * @author Knifer
 */
@Setter
@Getter
@RequiredArgsConstructor
public class MovieSearchService extends Service<Void> {

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
                Context.INSTANCE.getSpiderTemplate().getSearchContent(
                        GetSearchContentDTO.of(sourceKey, keyword),
                        searchContent -> {
                            List<Movie.Video> videos;

                            if (isCancelled()) {

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
