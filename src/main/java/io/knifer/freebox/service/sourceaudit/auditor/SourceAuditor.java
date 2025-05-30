package io.knifer.freebox.service.sourceaudit.auditor;

import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.service.sourceaudit.SourceAuditContext;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 源审计器
 *
 * @author Knifer
 */
@Setter
@Slf4j
@RequiredArgsConstructor
public abstract class SourceAuditor {

    protected final SpiderTemplate spiderTemplate;

    protected SourceAuditor nextAuditor;

    /**
     * 是否支持
     * @param sourceAuditType 审计类型
     * @return bool
     */
    public abstract boolean support(SourceAuditType sourceAuditType);

    /**
     * 进行审计
     * @param context 源审计上下文
     * @param skip 是否跳过
     */
    public abstract void audit(SourceAuditContext context, boolean skip);

    /**
     * 进行下一步审计
     * @param context 源审计上下文
     * @param skip 是否跳过
     */
    protected void doNext(SourceAuditContext context, boolean skip) {
        if (nextAuditor == null || context.isInterrupt()) {
            context.getAfterAll().run();
        } else {
            nextAuditor.audit(context, skip);
        }
    }
}
