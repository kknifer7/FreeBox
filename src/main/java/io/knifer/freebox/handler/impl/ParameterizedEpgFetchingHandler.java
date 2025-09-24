package io.knifer.freebox.handler.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.knifer.freebox.handler.EpgFetchingHandler;
import io.knifer.freebox.model.common.diyp.EPG;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

/**
 * 获取电子节目单实现（参数化url）
 *
 * @author Knifer
 */
public class ParameterizedEpgFetchingHandler implements EpgFetchingHandler {

    private static final ParameterizedEpgFetchingHandler INSTANCE = new ParameterizedEpgFetchingHandler();

    @Override
    public CompletableFuture<EPG> handle(String epgServiceUrl, String channelName, LocalDate date) {
        String url = epgServiceUrl.replace("{name}", channelName)
                .replace("{date}", LocalDateTimeUtil.formatNormal(date));

        return HttpUtil.getAsync(url)
                .thenApply(jsonContent -> {
                    if (StringUtils.isBlank(jsonContent)) {

                        return null;
                    }

                    return GsonUtil.fromJson(jsonContent, EPG.class);
                });
    }

    public static ParameterizedEpgFetchingHandler getInstance() {
        return INSTANCE;
    }
}
