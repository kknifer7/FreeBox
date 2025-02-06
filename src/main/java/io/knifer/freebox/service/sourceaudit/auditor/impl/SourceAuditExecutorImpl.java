package io.knifer.freebox.service.sourceaudit.auditor.impl;

import io.knifer.freebox.model.bo.SourceAuditExecutionBo;
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
    
    private static final SourceAuditExecutorImpl INSTANCE = new SourceAuditExecutorImpl();
    
    private static final List<SourceAuditor> AUDITORS;

    static {
        MovieSearchAuditor movieSearchAuditor = new MovieSearchAuditor();
        HomeAuditor homeAuditor = new HomeAuditor();
        MovieExploreAuditor movieExploreAuditor = new MovieExploreAuditor();
        MovieDetailAuditor movieDetailAuditor = new MovieDetailAuditor();
        MoviePlayAuditor moviePlayAuditor = new MoviePlayAuditor();

        movieSearchAuditor.setNextAuditor(homeAuditor);
        homeAuditor.setNextAuditor(movieExploreAuditor);
        movieExploreAuditor.setNextAuditor(movieDetailAuditor);
        movieDetailAuditor.setNextAuditor(moviePlayAuditor);
        AUDITORS = List.of(movieSearchAuditor, homeAuditor, movieExploreAuditor, movieDetailAuditor, moviePlayAuditor);
    }
    
    public static SourceAuditExecutorImpl getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void execute(SourceAuditExecutionBo bo) {
        AUDITORS.get(0).audit(SourceAuditContext.of(bo), false);
    }
}
