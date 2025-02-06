package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.MovieSort;
import io.knifer.freebox.model.common.SourceBean;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.GsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 首页信息审计
 *
 * @author Knifer
 */
public class HomeAuditor extends SourceAuditor {
    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.HOME;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        Consumer<Pair<SourceAuditType, String>> onRequest;
        Consumer<Pair<SourceAuditType, String>> onResponse;
        SourceBean sourceBean;

        if (skip) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.HOME, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.PROCESSING));
        sourceBean = context.getSourceBean();
        onRequest = context.getOnRequest();
        onResponse = context.getOnResponse();
        onRequest.accept(Pair.of(SourceAuditType.HOME, GsonUtil.toPrettyJson(sourceBean)));
        kebSocketTemplate.getHomeContent(context.getClientInfo(), sourceBean, content -> {
            List<SourceAuditResult> results;
            MovieSort movieSort;
            SourceAuditStatus status = SourceAuditStatus.SUCCESS;
            boolean needSkip;

            if (content == null) {
                onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.FAILED));
                onFinish.accept(Pair.of(SourceAuditType.HOME, List.of(SourceAuditResult.NO_DATA)));
                needSkip = true;
            } else {
                onResponse.accept(Pair.of(SourceAuditType.HOME, GsonUtil.toPrettyJson(content)));
                results = new ArrayList<>();
                movieSort = content.getClasses();
                if (movieSort == null || CollectionUtil.isEmpty(movieSort.getSortList())) {
                    status = SourceAuditStatus.FAILED;
                    results.add(SourceAuditResult.NO_MOVIE_SORT);
                }
                if (CollectionUtil.isEmpty(content.getVideoList())) {
                    results.add(SourceAuditResult.NO_VIDEO_LIST);
                }
                context.setHomeContent(content);
                onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, status));
                onFinish.accept(Pair.of(SourceAuditType.HOME, results));
                needSkip = false;
            }
            doNext(context, needSkip);
        });
    }
}
