package io.knifer.freebox.model.domain;

import cn.hutool.crypto.digest.DigestUtil;
import io.github.filelize.Filelize;
import io.github.filelize.FilelizeType;
import io.github.filelize.Id;
import io.knifer.freebox.constant.SpiderType;
import lombok.Data;

import java.io.File;

/**
 * 调试爬虫
 *
 * @author Knifer
 */
@Data
@Filelize(name = "spider_debugging", type = FilelizeType.MULTIPLE_FILES, directory = "spider_debugging")
public class SpiderDebugging implements Savable {

    @Id
    private String id;

    /**
     * 源文件路径
     */
    private String sourceFilePath;

    /**
     * 爬虫类型
     */
    private SpiderType spiderType;

    public static SpiderDebugging from(File spiderFile) {
        SpiderDebugging spiderDebugging = new SpiderDebugging();
        String sourceFilePath = spiderFile.getAbsolutePath();
        SpiderType spiderType = SpiderType.from(spiderFile);

        spiderDebugging.setId(DigestUtil.md5Hex(sourceFilePath));
        spiderDebugging.setSourceFilePath(sourceFilePath);
        spiderDebugging.setSpiderType(spiderType);

        return spiderDebugging;
    }
}
