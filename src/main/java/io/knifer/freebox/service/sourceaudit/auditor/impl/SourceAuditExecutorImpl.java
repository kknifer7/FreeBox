package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.model.bo.SourceAuditExecutionBo;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditExecutor;
import io.knifer.freebox.service.sourceaudit.auditor.SourceAuditor;

import java.util.List;

/**
 * 源审计执行器
 *
 * @author Knifer
 */
public class SourceAuditExecutorImpl implements SourceAuditExecutor {

    private final List<SourceAuditor> AUDITORS;

    private SourceAuditContext context;

    public SourceAuditExecutorImpl(SpiderTemplate spiderTemplate) {
        MovieSearchAuditor movieSearchAuditor = new MovieSearchAuditor(spiderTemplate);
        HomeAuditor homeAuditor = new HomeAuditor(spiderTemplate);
        MovieExploreAuditor movieExploreAuditor = new MovieExploreAuditor(spiderTemplate);
        MovieDetailAuditor movieDetailAuditor = new MovieDetailAuditor(spiderTemplate);
        MoviePlayAuditor moviePlayAuditor = new MoviePlayAuditor(spiderTemplate);

        movieSearchAuditor.setNextAuditor(homeAuditor);
        homeAuditor.setNextAuditor(movieExploreAuditor);
        movieExploreAuditor.setNextAuditor(movieDetailAuditor);
        movieDetailAuditor.setNextAuditor(moviePlayAuditor);
        AUDITORS = List.of(movieSearchAuditor, homeAuditor, movieExploreAuditor, movieDetailAuditor, moviePlayAuditor);
    }

    @Override
    public void execute(SourceAuditExecutionBo bo) {
        context = SourceAuditContext.of(bo);
        AUDITORS.get(0).audit(context, false);
    }

    @Override
    public void stop() {
        if (context == null) {
            return;
        }
        context.setInterrupt(true);
    }
}
