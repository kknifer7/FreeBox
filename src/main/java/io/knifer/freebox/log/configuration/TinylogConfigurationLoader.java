package io.knifer.freebox.log.configuration;

import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.StorageHelper;
import org.tinylog.configuration.PropertiesConfigurationLoader;

import java.io.File;
import java.util.Properties;

/**
 * 自定义日志配置加载器
 *
 * @author Knifer
 */
public class TinylogConfigurationLoader extends PropertiesConfigurationLoader {

    private final static String WRITER_FILE_KEY = "writerFile.file";
    private final static String WRITER_LATEST_FILE_KEY = "writerFile.latest";
    private final static String CONSOLE_LOG_LEVEL_KEY = "writerConsole.level";
    private final static String FILE_LOG_LEVEL_KEY = "writerFile.level";

    @Override
    public Properties load() {
        Properties properties = super.load();
        String logPath = StorageHelper.getLogStoragePath().toString() + File.separator;
        String logLevel;
        Object oldLogLevelObj;

        // 初始化日志配置
        if (!properties.contains(WRITER_FILE_KEY)) {
            properties.put(WRITER_FILE_KEY, logPath + "{date}.log");
        }
        if (!properties.contains(WRITER_LATEST_FILE_KEY)) {
            properties.put(WRITER_LATEST_FILE_KEY, logPath + "latest.log");
        }
        if (ConfigHelper.isLoaded()) {
            logLevel = ConfigHelper.getLogLevel().name();
            oldLogLevelObj = properties.get(CONSOLE_LOG_LEVEL_KEY);
            if (oldLogLevelObj == null || !logLevel.equalsIgnoreCase((String) oldLogLevelObj)) {
                properties.put(CONSOLE_LOG_LEVEL_KEY, logLevel);
            }
            oldLogLevelObj = properties.get(FILE_LOG_LEVEL_KEY);
            if (oldLogLevelObj == null || !logLevel.equalsIgnoreCase((String) oldLogLevelObj)) {
                properties.put(FILE_LOG_LEVEL_KEY, logLevel);
            }
        }

        return properties;
    }
}
