package io.knifer.freebox.component.node;

import cn.hutool.core.io.FileUtil;
import com.google.common.base.Charsets;
import com.google.gson.JsonSyntaxException;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.FreeBoxApiConfig;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.catvod.ApiConfigUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.concurrent.Service;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

/**
 * 导入猫影视规则对话框
 *
 * @author Knifer
 */
@Slf4j
public class ImportCatVodApiDialog extends ImportUrlApiDialog {

    public ImportCatVodApiDialog(Consumer<ClientInfo> onAction) {
        super();
        DialogPane dialogPane = getDialogPane();
        Text warningText = new Text(I18nHelper.get(I18nKeys.HOME_IMPORT_API_INPUT_PLACEHOLDER_WARNING));
        VBox contentVBox;

        warningText.setFill(Paint.valueOf(Color.RED.toString()));
        contentVBox = (VBox) dialogPane.getContent();
        contentVBox.getChildren().add(new TextFlow(warningText));
        super.onImport = url -> {
            Service<String> service;

            if (url.startsWith("http")) {
                super.loadingProperty.set(true);
                service = new FutureWaitingService<>(HttpUtil.getAsync(
                        url, BaseValues.FETCH_CAT_VOD_API_CONFIG_HTTP_HEADERS
                ));
                service.setOnSucceeded(
                        ignored -> dealWithApiConfig(
                                ApiConfigUtil.parseApiConfigJson(StringUtils.trim(service.getValue())),
                                url,
                                onAction
                        )
                );
                service.start();
            } else if (url.startsWith("file:///")) {
                dealWithApiConfig(FileUtil.readString(url, Charsets.UTF_8), url, onAction);
            }
        };
    }

    private void dealWithApiConfig(String jsonVal, String url, Consumer<ClientInfo> onAction) {
        FreeBoxApiConfig apiConfig;

        try {
            apiConfig = GsonUtil.fromJson(jsonVal, FreeBoxApiConfig.class);
        } catch (JsonSyntaxException e) {
            ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_GET_CONFIG_FAILED);
            super.loadingProperty.set(false);

            return;
        }
        if (apiConfig == null) {
            ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_GET_CONFIG_FAILED);
            super.loadingProperty.set(false);

            return;
        }
        apiConfig.setUrl(url);
        log.info("apiConfig: {}", apiConfig);
        if (CollectionUtil.isEmpty(apiConfig.getSites()) && CollectionUtil.isEmpty(apiConfig.getLives())) {
            ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_NO_AVAILABLE_SITE);
            super.loadingProperty.set(false);

            return;
        }
        super.loadingProperty.set(false);
        close();
        onAction.accept(ClientInfo.of(url, ClientType.CATVOD_SPIDER));
    }
}
