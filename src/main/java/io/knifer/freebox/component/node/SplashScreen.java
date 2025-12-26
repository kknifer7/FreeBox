package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.exception.FBException;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplashScreen {

    private StackPane root;
    private Stage stage;
    private ImageView imageView;
    private FadeTransition fadeIn;
    private FadeTransition fadeOut;

    private final Image image;
    private final Duration fadeDuration;

    public SplashScreen(double fadeDurationSeconds) {
        this.image = BaseResources.LOGO_IMG;
        this.fadeDuration = Duration.seconds(fadeDurationSeconds);
    }

    public Stage show() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::showInternal);

            return stage;
        }

        return showInternal();
    }

    private Stage showInternal() {
        Scene scene;
        Rectangle2D screenBounds;

        try {
            stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setAlwaysOnTop(true);
            imageView = new ImageView(image);
            root = new StackPane(imageView);
            root.setStyle("-fx-background-color: transparent;");
            scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            scene.setCursor(Cursor.WAIT);

            stage.setScene(scene);
            stage.setTitle("FreeBox");
            stage.sizeToScene();
            stage.getIcons().add(image);
            screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - image.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - image.getHeight()) / 2);
            stage.show();

            startFadeIn();

            return stage;
        } catch (Exception e) {
            throw new FBException("splash screen load failed", e);
        }
    }

    private void startFadeIn() {
        fadeIn = new FadeTransition(fadeDuration, imageView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    public void close() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::closeInternal);

            return;
        }
        closeInternal();
    }

    private void closeInternal() {
        if (stage == null || !stage.isShowing()) return;
        fadeOut = new FadeTransition(fadeDuration, root.getChildren().get(0));
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            stage.close();
            cleanup();
        });
        fadeOut.play();
    }

    private void cleanup() {
        if (fadeIn != null && fadeIn.getStatus() == Animation.Status.RUNNING) {
            fadeIn.stop();
        }
        if (fadeOut != null && fadeOut.getStatus() == Animation.Status.RUNNING) {
            fadeOut.stop();
        }
        stage = null;
        root = null;
    }
}