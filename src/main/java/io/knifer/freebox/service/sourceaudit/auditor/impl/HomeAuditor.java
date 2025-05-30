package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.tvbox.MovieSort;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.json.GsonUtil;
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

    public HomeAuditor(SpiderTemplate spiderTemplate) {
        super(spiderTemplate);
    }

    @Override
    public boolean support(SourceAuditType sourceAuditType) {
        return sourceAuditType == SourceAuditType.HOME;
    }

    @Override
    public void audit(SourceAuditContext context, boolean skip) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();

        if (skip || context.isInterrupt()) {
            onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.SKIPPED));
            onFinish.accept(Pair.of(SourceAuditType.HOME, List.of()));
            doNext(context, true);

            return;
        }
        onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.PROCESSING));
        doAudit(context, 0);
    }

    private void doAudit(SourceAuditContext context, int retryCount) {
        Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate = context.getOnStatusUpdate();
        Consumer<Pair<SourceAuditType, String>> onRequest = context.getOnRequest();
        Consumer<Pair<SourceAuditType, String>> onResponse = context.getOnResponse();
        Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish = context.getOnFinish();
        SourceBean sourceBean = context.getSourceBean();
        int maxRetryCount = context.getMaxRetryCount();

        onRequest.accept(Pair.of(SourceAuditType.HOME, GsonUtil.toPrettyJson(sourceBean)));
        spiderTemplate.getHomeContent(
                sourceBean,
                content -> {
                    List<SourceAuditResult> results;
                    MovieSort movieSort;
                    SourceAuditStatus status = SourceAuditStatus.SUCCESS;
                    boolean needSkip;

                    if (content == null) {
                        if (retryCount >= maxRetryCount) {
                            onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, SourceAuditStatus.FAILED));
                            onFinish.accept(Pair.of(SourceAuditType.HOME, List.of(SourceAuditResult.NO_DATA)));
                            needSkip = true;
                        } else {
                            doAudit(context, retryCount + 1);

                            return;
                        }
                    } else {
                        onResponse.accept(Pair.of(SourceAuditType.HOME, GsonUtil.toPrettyJson(content)));
                        results = new ArrayList<>();
                        movieSort = content.getClasses();
                        if (movieSort == null || CollectionUtil.isEmpty(movieSort.getSortList())) {
                            status = SourceAuditStatus.FAILED;
                            results.add(SourceAuditResult.NO_MOVIE_SORT);
                            needSkip = true;
                        } else {
                            needSkip = false;
                        }
                        if (CollectionUtil.isEmpty(content.getVideoList())) {
                            results.add(SourceAuditResult.NO_VIDEO_LIST);
                        }
                        context.setHomeContent(content);
                        onStatusUpdate.accept(Pair.of(SourceAuditType.HOME, status));
                        onFinish.accept(Pair.of(SourceAuditType.HOME, results));
                    }
                    doNext(context, needSkip);
                }
        );
    }
}
