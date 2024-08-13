package io.knifer.freebox.helper;

import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.util.GsonUtil;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 设置
 *
 * @author Knifer
 */
@UtilityClass
public class ConfigHelper {

    private static final Path CONFIG_PATH = Path.of("config", "config.json");

    private static Config config = loadConfig();

    public void refresh() {
        config = loadConfig();
    }

    private Config loadConfig() {
        Config config;
        String configJson;

        if (Files.exists(CONFIG_PATH)) {
            try {
                configJson = Files.readString(CONFIG_PATH);

                return GsonUtil.fromJson(configJson, Config.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                config = new Config();
                config.setUuid(UUID.randomUUID().toString());
                config.setSourceLink("https://ghproxy.net/https://raw.githubusercontent.com/Greatwallcorner/CatVodSpider/master/json/config.json");
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));

                return config;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getSourceLink() {
        return config.getSourceLink();
    }

    public void updateSourceLink(String sourceLink) {
        config.setSourceLink(sourceLink);
        saveConfig();
        refresh();
    }

    private void saveConfig() {
        try {
            Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
