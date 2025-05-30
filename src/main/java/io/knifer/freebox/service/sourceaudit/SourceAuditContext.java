package io.knifer.freebox.service.sourceaudit;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.bo.SourceAuditExecutionBo;
import io.knifer.freebox.model.common.tvbox.AbsSortXml;
import io.knifer.freebox.model.common.tvbox.AbsXml;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Consumer;

/**
 * 源审计上下文
 *
 * @author Knifer
 */
@Data
public class SourceAuditContext {

    /**
     * 源信息
     */
    private SourceBean sourceBean;

    /**
     * 发送请求时的回调
     */
    private Consumer<Pair<SourceAuditType, String>> onRequest;

    /**
     * 接收响应时的回调
     */
    private Consumer<Pair<SourceAuditType, String>> onResponse;

    /**
     * 状态更新回调
     */
    private Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate;

    /**
     * 完成回调（每个审计器完成后都会调用）
     */
    private Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish;

    /**
     * 最终回调（结束时调用，只会调用一次）
     */
    private Runnable afterAll;

    /**
     * 最大重试次数
     */
    private int maxRetryCount;

    /**
     * 中断标志
     */
    private boolean interrupt;

    /**
     * 首页信息
     */
    private AbsSortXml homeContent;

    /**
     * 影片分类信息
     */
    private AbsXml categoryContent;

    /**
     * 影片详情信息
     */
    private AbsXml detailContent;

    public static SourceAuditContext of(SourceAuditExecutionBo executionBo) {
        SourceAuditContext context = new SourceAuditContext();

        context.setSourceBean(executionBo.getSourceBean());
        context.setOnRequest(executionBo.getOnRequest());
        context.setOnResponse(executionBo.getOnResponse());
        context.setOnStatusUpdate(executionBo.getOnStatusUpdate());
        context.setOnFinish(executionBo.getOnFinish());
        context.setAfterAll(executionBo.getAfterAll());
        context.setMaxRetryCount(executionBo.getMaxRetryCount());
        context.setInterrupt(false);

        return context;
    }
}
