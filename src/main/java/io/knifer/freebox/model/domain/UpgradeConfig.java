package io.knifer.freebox.model.domain;

import io.knifer.freebox.constant.Architecture;
import io.knifer.freebox.constant.Platform;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新检查结果
 *
 * @author Knifer
 */
@Data
public class UpgradeConfig {

    private Integer minRequiredVersionCode;

    private String version;

    private Integer versionCode;

    private String changelog;

    private String releaseUrl;

    private LocalDate releaseDate;

    private List<ReleaseFileInfo> releaseFiles;

    @Data
    public static class ReleaseFileInfo {

        private Platform platform;

        private Architecture architecture;

        private Long fileSize;

        private String fileName;

        private String downloadLink;
    }
}
