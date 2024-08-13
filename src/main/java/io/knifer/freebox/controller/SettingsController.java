package io.knifer.freebox.controller;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.controlsfx.control.Notifications;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * 设置
 *
 * @author Knifer
 */
public class SettingsController {

    @FXML
    private BorderPane root;

    @FXML
    private TextField sourceLinkTextField;

    @FXML
    private ChoiceBox<String> ruleChoiceBox;

    @FXML
    private void initialize() {
        sourceLinkTextField.setText(ConfigHelper.getSourceLink());
        ruleChoiceBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void onSaveBtnClick() {
        String sourceLink = sourceLinkTextField.getText();

        try {
            new URL(sourceLink).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            Notifications.create()
                    .position(Pos.TOP_CENTER)
                    .text(I18nHelper.get(I18nKeys.SOURCE_LINK_ILLEGAL))
                    .showError();

            return;
        }
        ConfigHelper.updateSourceLink(sourceLink);
        WindowHelper.close(root);
        Notifications.create()
                .position(Pos.TOP_CENTER)
                .text(I18nHelper.get(I18nKeys.SETTINGS_SAVED))
                .graphic(FontIcon.of(FontAwesome.CHECK_CIRCLE, 32, Color.GREEN))
                .show();
    }

    @FXML
    private void onCancelBtnClick() {
        WindowHelper.close(root);
    }
}
