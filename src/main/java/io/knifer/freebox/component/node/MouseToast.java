package io.knifer.freebox.component.node;

import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 鼠标位置提示弹窗
 *
 * @author Knifer
 */
public class MouseToast {

    private static final int LIMIT = 3;
    private static final List<Stage> ACTIVE_STAGES = new ArrayList<>();
    private static final double INITIAL_TRANSLATE_Y = 10;
    private static final double EXIT_TRANSLATE_Y = -25;
    private static final Duration FADE_IN_DURATION = Duration.millis(300);
    private static final Duration STAY_DURATION = Duration.seconds(0.8);
    private static final Duration EXIT_DURATION = Duration.millis(400);
    private static final List<Color> COLORS = List.of(
            Color.web("#FF1493"), // 深粉
            Color.web("#0066FF"), // 亮蓝
            Color.web("#39FF14"), // 亮绿
            Color.web("#FF4500"), // 橙红
            Color.web("#9400D3"), // 深紫
            Color.web("#32CD32")  // 酸橙绿
    );

    public static void show(String text) {
        Color textColor;

        textColor = COLORS.get(ThreadLocalRandom.current().nextInt(COLORS.size()));
        show(text, textColor);
    }

    public static void show(String text, Color textColor) {
        double mouseX;
        double mouseY;
        Stage stage;
        StackPane root;
        Label label;
        Scene scene;
        double sceneX;
        double sceneY;
        double stageWidth;
        double stageHeight;
        SequentialTransition sequentialTransition;
        ParallelTransition fadeInParallel;
        TranslateTransition fadeInTranslate;
        FadeTransition fadeIn;
        PauseTransition pause;
        ParallelTransition exitParallel;
        TranslateTransition exitTranslate;
        FadeTransition exitFade;
        DropShadow glow;

        if (StringUtils.isBlank(text)) {

            return;
        }
        mouseX = java.awt.MouseInfo.getPointerInfo().getLocation().getX();
        mouseY = java.awt.MouseInfo.getPointerInfo().getLocation().getY();
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        Window.getWindows().stream()
                .filter(w -> w instanceof Stage && w.isShowing() && w != stage)
                .findFirst()
                .ifPresent(stage::initOwner);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");
        label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(320);
        label.setStyle(buildLabelStyle());
        label.setTextFill(textColor);
        glow = new DropShadow();
        glow.setColor(textColor);
        glow.setRadius(2);
        glow.setSpread(0.06);
        label.setEffect(glow);
        root.getChildren().add(label);
        scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        WindowHelper.setFontFamily(label, ConfigHelper.getUsageFontFamily());
        stage.sizeToScene();
        root.setOpacity(0);
        root.setTranslateY(INITIAL_TRANSLATE_Y);
        stage.show();
        stageWidth = stage.getWidth();
        stageHeight = stage.getHeight();
        sceneX = mouseX - stageWidth;
        sceneY = mouseY - stageHeight;
        if (sceneX < 0) {
            sceneX = mouseX;
        }
        if (sceneY < 0) {
            sceneY = mouseY;
        }
        stage.setX(sceneX);
        stage.setY(sceneY);
        addActiveStage(stage);
        fadeInTranslate = new TranslateTransition(FADE_IN_DURATION, root);
        fadeInTranslate.setFromY(INITIAL_TRANSLATE_Y);
        fadeInTranslate.setToY(0);
        fadeIn = new FadeTransition(FADE_IN_DURATION, root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeInParallel = new ParallelTransition(fadeInTranslate, fadeIn);
        pause = new PauseTransition(STAY_DURATION);
        exitTranslate = new TranslateTransition(EXIT_DURATION, root);
        exitTranslate.setFromY(0);
        exitTranslate.setToY(EXIT_TRANSLATE_Y);
        exitFade = new FadeTransition(EXIT_DURATION, root);
        exitFade.setFromValue(1);
        exitFade.setToValue(0);
        exitParallel = new ParallelTransition(exitTranslate, exitFade);
        sequentialTransition = new SequentialTransition(
                fadeInParallel,
                pause,
                exitParallel
        );
        sequentialTransition.setOnFinished(evt -> {
            stage.close();
            removeActiveStage(stage);
        });
        sequentialTransition.play();
    }

    private static synchronized void addActiveStage(Stage stage) {
        if (ACTIVE_STAGES.size() >= LIMIT) {
            Stage oldestStage = ACTIVE_STAGES.remove(0);
            if (oldestStage.isShowing()) {
                oldestStage.close();
            }
        }
        ACTIVE_STAGES.add(stage);
    }

    private static synchronized void removeActiveStage(Stage stage) {
        ACTIVE_STAGES.remove(stage);
    }

    private static String buildLabelStyle() {
        return "-fx-font-size: 15px; -fx-padding: 0;";
    }
}
