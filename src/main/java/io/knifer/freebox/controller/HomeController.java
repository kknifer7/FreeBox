package io.knifer.freebox.controller;

import io.knifer.freebox.FreeBoxApplication;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class HomeController {

    @FXML
    private void onSettingsBtnClick() throws IOException {
        Stage stage = new Stage();
        Scene scene = new Scene(FXMLLoader.load(
                FreeBoxApplication.class.getResource("settings-view.fxml"),
                I18nHelper.getBundle()
        ));

        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(I18nHelper.get(I18nKeys.SETTINGS));
        stage.setScene(scene);
        stage.show();
    }
}