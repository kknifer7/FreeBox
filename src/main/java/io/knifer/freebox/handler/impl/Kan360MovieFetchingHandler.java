package io.knifer.freebox.handler.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.knifer.freebox.handler.MovieRankFetchingHandler;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 360kan 影视数据获取处理器
 *
 * @author Knifer
 */
@Slf4j
public class Kan360MovieFetchingHandler implements MovieRankFetchingHandler {

    private final static String URL = "https://api.web.360kan.com/v1/rank?cat=1";

    @NotNull
    @Override
    public List<String> handle() {
        String resultBody;
        JsonObject resultJsonObj;
        JsonElement resultJsonElm;

        try {
            resultBody = HttpUtil.getAsync(URL)
                    .get(6, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.warn("request error", e);

            return List.of();
        }
        resultJsonObj = GsonUtil.fromJson(resultBody, JsonObject.class);
        if (resultJsonObj == null || !resultJsonObj.isJsonObject()) {

            return List.of();
        }
        resultJsonElm = resultJsonObj.get("data");
        if (resultJsonElm == null || !resultJsonElm.isJsonArray()) {

            return List.of();
        }

        return resultJsonElm.getAsJsonArray()
                .asList()
                .stream()
                .map(elm -> {
                    JsonElement titleElm;

                    if (!elm.isJsonObject()) {

                        return null;
                    }
                    titleElm = elm.getAsJsonObject().get("title");

                    return titleElm == null ? null : titleElm.getAsString();
                })
                .filter(StringUtils::isNotBlank)
                .toList();
    }
}
