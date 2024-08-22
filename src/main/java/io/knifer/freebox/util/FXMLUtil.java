package io.knifer.freebox.util;

import io.knifer.freebox.FreeBoxApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * FXML工具类
 *
 * @author Knifer
 */
@UtilityClass
public class FXMLUtil {

    public <T> Pair<Stage, T> load(String path) {
        FXMLLoader loader = new FXMLLoader(
                FreeBoxApplication.class.getResource(path),
                ResourceBundle.getBundle("i18n.chs")
        );
        Scene scene;
        Stage stage;

        try {
            scene = new Scene(loader.load(), 960, 720);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage = new Stage();
        stage.setScene(scene);

        return Pair.of(stage, loader.getController());
    }

    public void load(String path, Stage stage) {
        FXMLLoader loader = new FXMLLoader(
                FreeBoxApplication.class.getResource(path),
                ResourceBundle.getBundle("i18n.chs")
        );
        Scene scene;

        try {
            scene = new Scene(loader.load(), 960, 720);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        stage.setScene(scene);
    }
}
