package io.knifer.freebox.constant;

/**
 * 播放源审计项状态
 *
 * @author Knifer
 */
public enum SourceAuditStatus {

    // 初始化
    INITIALIZED,
    // 跳过审计
    SKIPPED,
    // 正在审计
    PROCESSING,
    // 审计通过
    SUCCESS,
    // 审计不通过
    FAILED
}
