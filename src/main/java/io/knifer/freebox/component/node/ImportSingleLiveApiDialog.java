package io.knifer.freebox.component.node;

import cn.hutool.core.io.FileUtil;
import com.google.common.base.Charsets;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.HttpUtil;
import javafx.concurrent.Service;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

/**
 * 导入独立直播源对话框
 *
 * @author Knifer
 */
public class ImportSingleLiveApiDialog extends ImportUrlApiDialog {

    public ImportSingleLiveApiDialog(Consumer<ClientInfo> onAction) {
        super();
        super.onImport = url -> {
            Service<String> service;
            String filePath;

            if (url.startsWith("http")) {
                super.loadingProperty.set(true);
                service = new FutureWaitingService<>(HttpUtil.getAsync(
                        url, BaseValues.FETCH_CAT_VOD_API_CONFIG_HTTP_HEADERS
                ));
                service.setOnSucceeded(
                        ignored -> dealWithLiveContent(
                                service.getValue(),
                                url,
                                onAction
                        )
                );
                service.start();
            } else if (url.startsWith("file:///")) {
                filePath = url.substring(7);
                if (!FileUtil.exist(filePath)) {
                    ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_INVALID_LIVE_URL);

                    return;
                }
                dealWithLiveContent(FileUtil.readString(filePath, Charsets.UTF_8), url, onAction);
            }
        };
    }

    private void dealWithLiveContent(String liveContent, String url, Consumer<ClientInfo> onAction) {
        super.loadingProperty.set(false);
        if (StringUtils.isBlank(liveContent)) {
            ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_INVALID_LIVE);
        } else {
            onAction.accept(ClientInfo.of(url, ClientType.SINGLE_LIVE));
        }
        setResult(BaseValues.SUCCESS_STR);
        close();
    }
}
