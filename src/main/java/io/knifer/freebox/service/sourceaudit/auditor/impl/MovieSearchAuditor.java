package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.Movie;
import io.knifer.freebox.model.s2c.GetSearchContentDTO;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.GsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Consumer;

/**
 * 影视搜索审计
 *
 * @author Knifer
 */
public class MovieSearchAuditor extends SourceAuditor {
    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.MOVIE_SEARCH;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        GetSearchContentDTO dto;
        Consumer<Pair<SourceAuditType, String>> onRequest;
        Consumer<Pair<SourceAuditType, String>> onResponse;

        if (skip) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, List.of()));
            doNext(context, false);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, SourceAuditStatus.PROCESSING));
        dto = GetSearchContentDTO.of(context.getSourceBean().getKey(), "战");
        onRequest = context.getOnRequest();
        onResponse = context.getOnResponse();
        onRequest.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, GsonUtil.toPrettyJson(dto)));
        kebSocketTemplate.getSearchContent(
                context.getClientInfo(),
                dto,
                content -> {
                    Movie movieData;

                    if (content == null) {
                        onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, SourceAuditStatus.FAILED));
                        onFinish.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, List.of(SourceAuditResult.NO_DATA)));
                    } else {
                        onResponse.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, GsonUtil.toPrettyJson(content)));
                        movieData = content.getMovie();
                        if (movieData == null) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(
                                    SourceAuditType.MOVIE_SEARCH, List.of(SourceAuditResult.NO_VIDEO_LIST)
                            ));
                        } else {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, SourceAuditStatus.SUCCESS));
                            onFinish.accept(Pair.of(SourceAuditType.MOVIE_SEARCH, List.of()));
                        }
                    }
                    doNext(context, false);
                }
        );
    }
}
