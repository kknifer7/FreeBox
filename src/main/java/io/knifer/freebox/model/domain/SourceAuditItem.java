package io.knifer.freebox.model.domain;

import io.knifer.freebox.constant.SourceAuditStatus;
import io.knifer.freebox.constant.SourceAuditType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放源审计项
 *
 * @author Knifer
 */
@Data
public class SourceAuditItem {

    private String sourceKey;

    private SourceAuditType auditType;

    private String name;

    private LocalDateTime startAt;

    private Double cost;

    private SourceAuditStatus status;

    private String resultInfo;

    private String requestRawData;

    private String responseRawData;

    public static SourceAuditItem newInitializedItem(
            String sourceKey,
            SourceAuditType auditType,
            String name
    ) {
        SourceAuditItem result = new SourceAuditItem();

        result.setSourceKey(sourceKey);
        result.setAuditType(auditType);
        result.setName(name);
        result.setStatus(SourceAuditStatus.INITIALIZED);

        return result;
    }
}
