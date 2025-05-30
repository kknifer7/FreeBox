package io.knifer.freebox.service.sourceaudit.auditor;

import io.knifer.freebox.model.bo.SourceAuditExecutionBo;

/**
 * 源审计执行器
 *
 * @author Knifer
 */
public interface SourceAuditExecutor {

    /**
     * 执行审计
     * @param bo 审计参数
     */
    void execute(SourceAuditExecutionBo bo);

    /**
     * 停止审计
     */
    void stop();
}
