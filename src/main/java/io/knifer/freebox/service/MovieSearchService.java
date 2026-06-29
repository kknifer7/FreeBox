package io.knifer.freebox.service;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.ioc.IOC;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 影视搜索服务
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class MovieSearchService extends Service<Void> {

    /**
     * 源ID列表
     */
    @Setter
    @Getter
    private Iterator<String> sourceKeyIterator;

    /**
     * 关键字
     */
    @Setter
    @Getter
    private String keyword;

    /**
     * 数据处理回调（数据为 关键字-搜索结果 键值对）
     */
    private final Consumer<Pair<String, AbsXml>> callback;

    /**
     * 结束回调
     */
    private final Runnable endCallback;

    /**
     * 搜索任务线程池
     */
    private ExecutorService executor;

    /**
     * 任务列表
     */
    private final List<Future<?>> jobs = new CopyOnWriteArrayList<>();

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                search(keyword);

                return null;
            }

            private void search(String keyword) {
                List<String> sourceKeys = new ArrayList<>();
                int totalSources;
                AtomicInteger completedCount;
                Future<?> job;

                sourceKeyIterator.forEachRemaining(sourceKeys::add);
                if (sourceKeys.isEmpty()) {
                    runEndCallback();

                    return;
                }
                totalSources = sourceKeys.size();
                completedCount= new AtomicInteger(0);
                executor = Executors.newFixedThreadPool(
                        Math.min(totalSources, Runtime.getRuntime().availableProcessors())
                );
                try {
                    for (String sourceKey : sourceKeys) {
                        job = executor.submit(() -> {
                            if (isCancelled()) {

                                return;
                            }
                            IOC.getBean(Context.class).getSpiderTemplate().getSearchContent(
                                    GetSearchContentDTO.of(sourceKey, keyword),
                                    searchContent -> {
                                        Movie movie;

                                        if (isCancelled()) {

                                            return;
                                        }
                                        if (
                                                searchContent != null &&
                                                (movie = searchContent.getMovie()) != null &&
                                                CollectionUtil.isNotEmpty(movie.getVideoList())
                                        ) {
                                            callback.accept(Pair.of(keyword, searchContent));
                                        }
                                        if (completedCount.incrementAndGet() == totalSources) {
                                            runEndCallback();
                                        }
                                    }
                            );
                        });
                        jobs.add(job);
                    }
                } finally {
                    executor.shutdown();
                }
            }

            private void runEndCallback() {
                if (!isCancelled()) {
                    endCallback.run();
                }
            }

            @Override
            public boolean cancel(boolean b) {
                boolean result = super.cancel(b);

                if (executor != null) {
                    for (Future<?> job : jobs) {
                        job.cancel(b);
                    }
                    executor.shutdownNow();
                }
                jobs.clear();

                return result;
            }
        };
    }
}
