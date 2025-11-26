package io.knifer.freebox.handler.impl;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.handler.MovieSuggestionHandler;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.textfield.AutoCompletionBinding;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * IQiYi影视建议处理器
 *
 * @author Knifer
 */
@Slf4j
public class IQiYiMovieSuggestionHandler implements MovieSuggestionHandler {

    private static final String SEARCH_SUGGESTION_REQUEST_URL = "https://suggest.video.iqiyi.com/?if=mobile&key=";
    private static final String[] SEARCH_SUGGESTION_REQUEST_HEADERS = {
            HttpHeaders.USER_AGENT, BaseValues.USER_AGENT,
            HttpHeaders.REFERER, "https://so.iqiyi.com/",
            HttpHeaders.ORIGIN, "https://so.iqiyi.com/"
    };

    @Override
    public Collection<String> handle(AutoCompletionBinding.ISuggestionRequest suggestionRequest) {
        String userText = suggestionRequest.getUserText();
        String resultBody;
        JsonObject resultJsonObj;
        JsonElement resultJsonElm;

        if (StringUtils.isBlank(userText)) {

            return List.of();
        }
        try {
            resultBody = HttpUtil.getAsync(SEARCH_SUGGESTION_REQUEST_URL + userText, SEARCH_SUGGESTION_REQUEST_HEADERS)
                    .get(6, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | InterruptedException e) {
            log.warn("request error", e);

            return List.of();
        }
        log.info("userText: {}, resultBody: {}", userText, resultBody);
        resultJsonObj = GsonUtil.fromJson(resultBody, JsonObject.class);
        if (resultJsonObj == null) {

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
                    JsonElement nameElm;

                    if (!elm.isJsonObject()) {

                        return null;
                    }
                    nameElm = elm.getAsJsonObject().get("name");

                    return nameElm == null ? null : nameElm.getAsString();
                })
                .filter(StringUtils::isNotBlank)
                .toList();
    }
}
