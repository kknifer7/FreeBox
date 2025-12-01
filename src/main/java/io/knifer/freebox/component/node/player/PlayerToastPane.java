package io.knifer.freebox.component.node.player;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class PlayerToastPane extends StackPane {
    private final Label messageLabel;
    private Timeline currentTimeline;
    private final DoubleProperty duration = new SimpleDoubleProperty(4.0);
    private final DoubleProperty fadeDuration = new SimpleDoubleProperty(0.3);
    private final DoubleProperty cornerRadius = new SimpleDoubleProperty(6.0);
    private final DoubleProperty opacity = new SimpleDoubleProperty(0.5);

    // 跟踪当前动画状态
    private final BooleanProperty isShowing = new SimpleBooleanProperty(false);
    private final BooleanProperty isHiding = new SimpleBooleanProperty(false);

    public PlayerToastPane() {
        getStyleClass().add("player-toast-pane");

        messageLabel = new Label();
        messageLabel.getStyleClass().add("player-toast-label");

        getChildren().add(messageLabel);
        updateStyle();
        setVisible(false);

        cornerRadius.addListener((obs, oldVal, newVal) -> updateStyle());
        opacity.addListener((obs, oldVal, newVal) -> updateStyle());
    }

    /**
     * 安全显示提示信息 - 修复动画冲突版本
     */
    public void showToast(String message) {
        showToast(message, duration.get());
    }

    public void showToast(String message, double displayDuration) {
        // 停止当前运行的任何动画
        stopCurrentAnimationSafely();

        // 设置消息和初始状态
        messageLabel.setText(message);
        setVisible(true);

        // 重置可能正在进行的隐藏动画状态
        isHiding.set(false);
        isShowing.set(true);

        // 创建新的时间线动画，避免使用SequentialTransition
        createAndPlayTimeline(displayDuration);
    }

    /**
     * 安全停止当前动画
     */
    private void stopCurrentAnimationSafely() {
        if (currentTimeline != null) {
            // 检查动画状态而不是直接停止[citation:1]
            Animation.Status status = currentTimeline.getStatus();
            if (status == Animation.Status.RUNNING) {
                // 使用更安全的方式停止动画
                currentTimeline.stop();
            }
            currentTimeline = null;
        }

        // 重置所有可能的内嵌动画属性
        setOpacity(1.0);
        setTranslateY(0);
    }

    /**
     * 创建并播放时间线动画（替代SequentialTransition方案）
     */
    private void createAndPlayTimeline(double displayDuration) {
        // 清除任何可能冲突的过渡
        setOpacity(0.0);
        setTranslateY(10);

        // 创建单一时间线管理所有动画阶段
        currentTimeline = new Timeline(
                // 第一阶段：淡入动画
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), 0.0),
                        new KeyValue(translateYProperty(), 10)
                ),
                new KeyFrame(Duration.seconds(fadeDuration.get()),
                        new KeyValue(opacityProperty(), 1.0),
                        new KeyValue(translateYProperty(), 0)
                ),

                // 第二阶段：保持显示
                new KeyFrame(Duration.seconds(displayDuration - fadeDuration.get())),

                // 第三阶段：淡出动画
                new KeyFrame(Duration.seconds(displayDuration),
                        new KeyValue(opacityProperty(), 0.0)
                )
        );

        currentTimeline.setOnFinished(e -> {
            setVisible(false);
            isShowing.set(false);
            isHiding.set(false);
            currentTimeline = null;
        });

        currentTimeline.play();
    }

    /**
     * 隐藏提示
     */
    public void hide() {
        stopCurrentAnimationSafely();
        setVisible(false);
        isShowing.set(false);
        isHiding.set(false);
    }

    private void updateStyle() {
        String style = String.format(
                "-fx-background-color: rgba(0, 0, 0, %f); " +
                        "-fx-background-radius: %f; " +
                        "-fx-border-radius: %f; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 8, 0.5, 0, 2);",
                opacity.get(), cornerRadius.get(), cornerRadius.get()
        );
        setStyle(style);
    }

    /**
     * 新增：检查是否正在显示动画
     */
    public boolean isShowing() { return isShowing.get(); }
}