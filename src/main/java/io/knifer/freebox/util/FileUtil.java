package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 文件操作工具类
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class FileUtil {

    /**
     * 在指定目录下寻找符合正则表达式的子文件
     * @param directoryPath 目录路径
     * @param fileNamePattern 文件名正则
     * @return 是否存在
     */
    public boolean existsSubFile(String directoryPath, Pattern fileNamePattern) {
        Path path;

        if (StringUtils.isBlank(directoryPath)) {

            return false;
        }
        path = Path.of(directoryPath);
        if (!Files.isDirectory(path)) {

            return false;
        }
        try (Stream<Path> paths = Files.list(path)) {

            return paths.anyMatch(p -> fileNamePattern.matcher(p.getFileName().toString()).matches());
        } catch (IOException e) {
            log.warn("match file failed", e);

            return false;
        }
    }
}
