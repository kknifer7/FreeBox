package io.knifer.freebox.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 源审计结果
 *
 * @author Knifer
 */
@Getter
@AllArgsConstructor
public enum SourceAuditResult {

    NO_DATA(Level.ERROR, "source-audit.audit-result.no-data"),
    NO_MOVIE_SORT(Level.ERROR, "source-audit.audit-result.no-movie-sort"),
    NO_VIDEO_LIST(Level.WARN, "source-audit.audit-result.no-video-list"),
    PAGE_INFO_INACCURATE(Level.WARN, "source-audit.audit-result.page-info-inaccurate"),
    NO_VIDEO_LIST_ERROR(Level.ERROR, "source-audit.audit-result.no-video-list"),
    NO_VIDEO(Level.ERROR, "source-audit.audit-result.no-video"),
    INVALID_VIDEO(Level.ERROR, "source-audit.audit-result.invalid-video"),
    INVALID_VIDEO_URLS(Level.ERROR, "source-audit.audit-result.invalid-video-urls"),
    NO_VIDEO_URL(Level.ERROR, "source-audit.audit-result.no-url"),
    INVALID_VIDEO_URL(Level.ERROR, "source-audit.audit-result.invalid-video-url"),
    UNSEARCHABLE(Level.INFO, "source-audit.audit-result.unsearchable")
    ;

    private final Level level;

    private final String i18nKey;

    public SourceAuditStatus toSourceAuditStatus() {
        return switch (this.getLevel()) {
            case INFO, WARN -> SourceAuditStatus.SUCCESS;
            default -> SourceAuditStatus.FAILED;
        };
    }

    public enum Level {
        INFO,
        WARN,
        ERROR
    }
}
