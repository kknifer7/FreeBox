package io.knifer.freebox.model.bo;

import io.knifer.freebox.constant.SourceAuditResult;
import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import io.knifer.freebox.model.common.tvbox.SourceBean;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Consumer;

/**
 * 源审计执行参数
 *
 * @author Knifer
 */
@Data
public class SourceAuditExecutionBo {

    /**
     * 待审计源
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

    public static SourceAuditExecutionBo of(
            SourceBean sourceBean,
            Consumer<Pair<SourceAuditType, String>> onRequest,
            Consumer<Pair<SourceAuditType, String>> onResponse,
            Consumer<Pair<SourceAuditType, SourceAuditStatus>> onStatusUpdate,
            Consumer<Pair<SourceAuditType, List<SourceAuditResult>>> onFinish,
            Runnable afterAll,
            int maxRetryCount
    ) {
        SourceAuditExecutionBo result = new SourceAuditExecutionBo();

        result.setSourceBean(sourceBean);
        result.setOnRequest(onRequest);
        result.setOnResponse(onResponse);
        result.setOnStatusUpdate(onStatusUpdate);
        result.setOnFinish(onFinish);
        result.setAfterAll(afterAll);
        result.setMaxRetryCount(maxRetryCount);

        return result;
    }
}
