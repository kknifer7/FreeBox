package io.knifer.freebox.component.node;

import io.knifer.freebox.component.validator.URLValidator;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.validation.ValidationSupport;

import java.util.function.Consumer;

/**
 * 导入猫影视规则对话框
 *
 * @author Knifer
 */
@Slf4j
public abstract class ImportUrlApiDialog extends TextInputDialog {

    protected final BooleanProperty loadingProperty;
    protected Consumer<String> onImport;

    private final ValidationSupport validationSupport;

    public ImportUrlApiDialog() {
        super();
        DialogPane dialogPane = getDialogPane();
        TextField editor = getEditor();
        GridPane dialogContentGridPane;
        VBox contentVBox;
        Button okBtn;

        validationSupport = new ValidationSupport();
        loadingProperty = new SimpleBooleanProperty(false);
        setTitle(I18nHelper.get(I18nKeys.HOME_IMPORT_API));
        setHeaderText(I18nHelper.get(I18nKeys.HOME_IMPORT_API_INPUT_PLACEHOLDER));
        setContentText(I18nHelper.get(I18nKeys.HOME_IMPORT_API_INPUT_LABEL));
        dialogContentGridPane = ((GridPane) dialogPane.getContent());
        contentVBox = new VBox(dialogContentGridPane);
        contentVBox.setSpacing(10d);
        dialogPane.setContent(contentVBox);
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

            if (ValidationHelper.validate(validationSupport, editor) && StringUtils.isNotEmpty(url)) {
                log.info("import source from: {}", url);
                if (!url.startsWith("http") && !url.startsWith("file:///")) {
                    ToastHelper.showErrorI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_INVALID_CONFIG_URL);
                } else if (onImport != null) {
                    onImport.accept(url);
                }
            }
            evt.consume();
        });
        WindowHelper.setFontFamily(dialogPane, ConfigHelper.getUsageFontFamily());
    }
}
