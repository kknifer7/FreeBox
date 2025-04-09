package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.model.bo.SourceAuditExecutionBo;
import io.knifer.freebox.net.websocket.template.KebSocketTemplate;
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

    public SourceAuditExecutorImpl(KebSocketTemplate kebSocketTemplate) {
        MovieSearchAuditor movieSearchAuditor = new MovieSearchAuditor(kebSocketTemplate);
        HomeAuditor homeAuditor = new HomeAuditor(kebSocketTemplate);
        MovieExploreAuditor movieExploreAuditor = new MovieExploreAuditor(kebSocketTemplate);
        MovieDetailAuditor movieDetailAuditor = new MovieDetailAuditor(kebSocketTemplate);
        MoviePlayAuditor moviePlayAuditor = new MoviePlayAuditor(kebSocketTemplate);

        movieSearchAuditor.setNextAuditor(homeAuditor);
        homeAuditor.setNextAuditor(movieExploreAuditor);
        movieExploreAuditor.setNextAuditor(movieDetailAuditor);
        movieDetailAuditor.setNextAuditor(moviePlayAuditor);
        AUDITORS = List.of(movieSearchAuditor, homeAuditor, movieExploreAuditor, movieDetailAuditor, moviePlayAuditor);
    }

    @Override
    public void execute(SourceAuditExecutionBo bo) {
        AUDITORS.get(0).audit(SourceAuditContext.of(bo), false);
    }
}
