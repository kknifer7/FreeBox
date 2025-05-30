package io.knifer.freebox.component.node;

import com.google.gson.JsonSyntaxException;
import io.knifer.freebox.component.validator.URLValidator;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.ValidationHelper;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.FreeBoxApiConfig;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.validation.ValidationSupport;

import java.util.function.Consumer;

/**
 * 导入API对话框
 *
 * @author Knifer
 */
@Slf4j
public class ImportApiDialog extends TextInputDialog {

    private final ValidationSupport validationSupport;
    private final BooleanProperty loadingProperty;

    public ImportApiDialog(Consumer<ClientInfo> onAction) {
        super();
        DialogPane dialogPane = getDialogPane();
        TextField editor = getEditor();
        Button okBtn;
        ProgressIndicator loadingIndicator;

        validationSupport = new ValidationSupport();
        loadingProperty = new SimpleBooleanProperty(false);
        setTitle(I18nHelper.get(I18nKeys.HOME_IMPORT_API));
        setHeaderText(I18nHelper.get(I18nKeys.HOME_IMPORT_API_INPUT_PLACEHOLDER));
        setContentText(I18nHelper.get(I18nKeys.HOME_IMPORT_API_INPUT_LABEL));
        dialogPane.setPrefWidth(500);
        setOnShowing(evt -> validationSupport.registerValidator(editor, URLValidator.getInstance()));
        editor.textProperty().addListener(
                (ob, oldVal, newVal) ->
                        ValidationHelper.validate(validationSupport, editor)
        );
        okBtn = (Button) dialogPane.lookupButton(ButtonType.OK);
        okBtn.disableProperty().bind(loadingProperty);
        okBtn.addEventFilter(ActionEvent.ACTION, evt -> {
            String url = editor.getText();
            Service<String> service;

            if (ValidationHelper.validate(validationSupport, editor) && StringUtils.isNotEmpty(url)) {
                loadingProperty.set(true);
                log.info("import source json from: {}", url);
                service = new FutureWaitingService<>(HttpUtil.getAsync(url));
                service.setOnSucceeded(ignored -> {
                    String jsonVal = service.getValue();
                    FreeBoxApiConfig apiConfig;

                    try {
                        apiConfig = GsonUtil.fromJson(jsonVal, FreeBoxApiConfig.class);
                    } catch (JsonSyntaxException e) {
                        ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_GET_CONFIG_FAILED);
                        loadingProperty.set(false);

                        return;
                    }
                    apiConfig.setUrl(url);
                    log.info("apiConfig: {}", apiConfig);
                    if (CollectionUtil.isEmpty(apiConfig.getSites())) {
                        ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_NO_AVAILABLE_SITE);
                        loadingProperty.set(false);

                        return;
                    }
                    loadingProperty.set(false);
                    close();
                    onAction.accept(ClientInfo.of(url));
                });
                service.start();
            }
            evt.consume();
        });
    }
}
