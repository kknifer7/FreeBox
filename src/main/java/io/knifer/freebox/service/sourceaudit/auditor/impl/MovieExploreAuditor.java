package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.tvbox.AbsSortXml;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.s2c.GetCategoryContentDTO;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.json.GsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 影视分类信息审计
 *
 * @author Knifer
 */
public class MovieExploreAuditor extends SourceAuditor {

    public MovieExploreAuditor(SpiderTemplate spiderTemplate) {
        super(spiderTemplate);
    }

    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.MOVIE_EXPLORE;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();

        if (skip || context.isInterrupt()) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, SourceAuditStatus.PROCESSING));
        doAudit(context, 0);
    }

    private void doAudit(SourceAuditContext context, int retryCount) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        AbsSortXml homeContent = context.getHomeContent();
        MovieSort.SortData sortData = homeContent.getClasses().getSortList().get(0);
        GetCategoryContentDTO dto = GetCategoryContentDTO.of(
                context.getSourceBean().getKey(), sortData.getId(), false, "1", sortData.getFilterSelect()
        );
        Consumer<Pair<SourceAuditType, String>> onRequest = context.getOnRequest();
        Consumer<Pair<SourceAuditType, String>> onResponse = context.getOnResponse();
        int maxRetryCount = context.getMaxRetryCount();

        onRequest.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, GsonUtil.toPrettyJson(dto)));
        spiderTemplate.getCategoryContent(
                dto,
                categoryContent -> {
                    Movie movieData;
                    List<SourceAuditResult> results;
                    boolean needSkip;

                    if (categoryContent == null) {
                        if (retryCount >= maxRetryCount) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, List.of(SourceAuditResult.NO_DATA)));
                            needSkip = true;
                        } else {
                            doAudit(context, retryCount + 1);

                            return;
                        }
                    } else {
                        onResponse.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, GsonUtil.toPrettyJson(categoryContent)));
                        movieData = categoryContent.getMovie();
                        if (movieData == null || CollectionUtil.isEmpty(movieData.getVideoList())) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_EXPLORE, List.of(SourceAuditResult.NO_VIDEO_LIST)
                            ));
                            needSkip = true;
                        } else {
                            results = new ArrayList<>();
                            if (movieData.getPagecount() < 1 && movieData.getRecordcount() < 1) {
                                results.add(SourceAuditResult.PAGE_INFO_INACCURATE);
                            }
                            context.setCategoryContent(categoryContent);
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, SourceAuditStatus.SUCCESS));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_EXPLORE, results));
                            needSkip = false;
                        }
                    }
                    doNext(context, needSkip);
                }
        );
    }
}
