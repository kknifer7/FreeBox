package io.knifer.freebox.util;

import io.knifer.freebox.FreeBoxApplication;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.ioc.IOC;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * FXML工具类
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class FXMLUtil {

    public <T> Pair<Stage, T> load(String path) {
        return load(path, BaseValues.DEFAULT_WINDOW_WIDTH, BaseValues.DEFAULT_WINDOW_HEIGHT);
    }

    public <T> Pair<Stage, T> loadDialog(String path) {
        return loadDialog(path, StageStyle.TRANSPARENT, Modality.APPLICATION_MODAL);
    }

    public <T> Pair<Stage, T> loadDialog(String path, StageStyle stageStyle, Modality modality) {
        Pair<Stage, T> stageAndController =
                load(path, BaseValues.DEFAULT_DIALOG_WIDTH, BaseValues.DEFAULT_DIALOG_HEIGHT);
        Stage stage = stageAndController.getLeft();

        stage.initStyle(stageStyle);
        stage.initModality(modality);

        return stageAndController;
    }

    public <T> Pair<Stage, T> load(String path, double width, double height) {
        FXMLLoader loader = createLoader(path);
        Scene scene;
        Stage stage;

        try {
            scene = new Scene(loader.load(), width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage = new Stage();
        stage.getIcons().add(BaseResources.LOGO_IMG);
        stage.setScene(scene);
        postProcess(stage);

        return Pair.of(stage, loader.getController());
    }

    public void load(String path, Stage stage) {
        load(path, stage, 960, 720);
    }

    public void load(String path, Stage stage, double width, double height) {
        FXMLLoader loader = createLoader(path);
        Scene scene;

        try {
            scene = new Scene(loader.load(), width, height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.getIcons().add(BaseResources.LOGO_IMG);
        stage.setScene(scene);
        postProcess(stage);
    }

    private FXMLLoader createLoader(String path) {
        FXMLLoader loader = new FXMLLoader(
                FreeBoxApplication.class.getResource(path),
                ResourceBundle.getBundle("i18n.chs")
        );

        loader.setControllerFactory(IOC::getBean);

        return loader;
    }

    private void postProcess(Stage stage) {
        // 字体处理
        WindowHelper.setFontFamily(stage, ConfigHelper.getUsageFontFamily());
    }
}
