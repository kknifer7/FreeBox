package io.knifer.freebox.handler.impl;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.handler.MovieBatchSearchingHandler;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.s2c.GetSearchContentDTO;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CollectionUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 批量调用Spider影视搜索实现
 * 调用时，如果前一次调用的搜索任务还未完成，则未完成的任务会被批量取消
 *
 * @author Knifer
 */
@Singleton
@ThreadSafe
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class CommonMovieBatchSearchingHandler implements MovieBatchSearchingHandler {

    /**
     * 任务列表
     */
    private volatile List<CompletableFuture<?>> jobs;

    private final Lock lock = new ReentrantLock();

    private final Context context;

    @Override
    public void handle(
            Collection<String> sourceKeys,
            String keyword,
            Consumer<Pair<String, AbsXml>> callback,
            Runnable finishCallback
    ) {
        int totalSources;
        List<CompletableFuture<?>> jobList;
        CompletableFuture<AbsXml> searchFuture;
        SpiderTemplate template = context.getSpiderTemplate();

        lock.lock();
        try {
            cancelAllJobs();
            totalSources = sourceKeys.size();
            jobList = new ArrayList<>(totalSources);
            for (String sourceKey : sourceKeys) {
                searchFuture = template.getSearchContent(GetSearchContentDTO.of(sourceKey, keyword));
                searchFuture.thenAccept(searchContent -> {
                    Movie movie;

                    if (jobs != jobList) {

                        return;
                    }
                    if (
                            searchContent != null &&
                            (movie = searchContent.getMovie()) != null &&
                            CollectionUtil.isNotEmpty(movie.getVideoList())
                    ) {
                        Platform.runLater(() ->
                                callback.accept(Pair.of(keyword, searchContent))
                        );
                    }
                });
                jobList.add(searchFuture);
            }
            jobs = jobList;
        } finally {
            lock.unlock();
        }
        CompletableFuture.allOf(jobList.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, exception) -> {
                    if (jobs != jobList) {
                        // 任务已被新任务覆盖，直接忽略
                        return;
                    }
                    Platform.runLater(finishCallback);
                });
    }

    @Override
    public void cancelSearching(Runnable callback) {
        lock.lock();
        try {
            cancelAllJobs().thenRun(callback);
        } finally {
            lock.unlock();
        }
    }

    private CompletableFuture<Void> cancelAllJobs() {
        List<CompletableFuture<?>> oldJobs = jobs;
        SpiderTemplate template = context.getSpiderTemplate();

        jobs = null;
        if (oldJobs != null) {
            for (CompletableFuture<?> job : oldJobs) {
                if (!job.isDone()) {
                    job.cancel(true);
                }
            }
        }

        return template.cancelAllSearching().exceptionally(e -> {
            Platform.runLater(() -> ToastHelper.showException(e));

            return null;
        });
    }
}
