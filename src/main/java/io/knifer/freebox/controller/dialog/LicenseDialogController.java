package io.knifer.freebox.controller.dialog;

import cn.hutool.core.io.IoUtil;
import io.knifer.freebox.FreeBoxApplication;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.LoadingHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.InputStream;

/**
 * 许可协议对话框
 *
 * @author Knifer
 */
public class LicenseDialogController {

    @FXML
    private TextArea licenseText;
    @FXML
    private CheckBox agreeCheckBox;
    @FXML
    private Button agreeButton;
    @FXML
    private Button disagreeButton;
    @FXML
    private Label agreementLabel;

    @FXML
    private void initialize() {
        loadLicenseContent();

        if (!ConfigHelper.getShowLicense()) {
            disagreeButton.setVisible(false);
            disagreeButton.setManaged(false);
            agreeCheckBox.setSelected(true);
            agreementLabel.setDisable(true);
            agreeButton.setText(I18nHelper.get(I18nKeys.COMMON_CLOSE));
        } else {
            agreeButton.setText(I18nHelper.get(I18nKeys.LICENSE_AGREE));
            agreeButton.disableProperty().bind(agreeCheckBox.selectedProperty().not());
        }
    }

    private void loadLicenseContent() {
        InputStream resource = FreeBoxApplication.class.getResourceAsStream("doc/license.txt");
        String license;

        if (resource == null) {
            throw new FBException("no license");
        }
        license = IoUtil.readUtf8(resource);
        licenseText.setText(license);
    }

    @FXML
    private void onAgreeButtonAction() {
        if (ConfigHelper.getShowLicense()) {
            ConfigHelper.setShowLicense(false);
            ConfigHelper.saveAnyWay();
        }
        WindowHelper.close(agreeButton);
    }

    @FXML
    private void onDisagreeButtonAction() {
        LoadingHelper.showLoading(WindowHelper.getStage(disagreeButton), I18nKeys.MESSAGE_QUIT_LOADING);
        Context.INSTANCE.destroy();
    }

    @FXML
    private void onAgreementLabelMouseClicked() {
        agreeCheckBox.setSelected(!agreeCheckBox.selectedProperty().get());
    }
}
