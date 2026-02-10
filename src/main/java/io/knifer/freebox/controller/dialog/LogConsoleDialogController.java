package io.knifer.freebox.controller.dialog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.Tailer;
import cn.hutool.core.text.StrPool;
import com.google.common.base.Charsets;
import io.knifer.freebox.component.router.Router;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.controller.BaseController;
import io.knifer.freebox.helper.*;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.tinylog.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 日志控制台对话框
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class LogConsoleDialogController extends BaseController {

    @FXML
    private TextFlow logTextFlow;
    @FXML
    private ScrollPane logScrollPane;
    @FXML
    private Label statusLabel;

    private Tailer logFileTailer;

    private Stage stage;

    private final Router router;

    @FXML
    private void initialize() {
        logTextFlow.prefWidthProperty().bind(logScrollPane.widthProperty().subtract(20));
        Platform.runLater(() -> {
            stage = WindowHelper.getStage(logTextFlow);
            stage.setOnCloseRequest(evt -> {
                logFileTailer.stop();
                router.removeSecondary(Views.LOG_CONSOLE_DIALOG);
            });
            startListeningLatestLog();
        });
        updateTip(I18nKeys.LOG_CONSOLE_TIP);
    }

    private void startListeningLatestLog() {
        Path logFile = StorageHelper.getLogStoragePath().resolve("latest.log");

        try {
            Files.readAllLines(logFile, Charsets.UTF_8).forEach(this::appendLog);
        } catch (IOException e) {
            log.error("read latest.log failed");
            ToastHelper.showException(e);
        }
        logFileTailer = new Tailer(logFile.toFile(), line -> Platform.runLater(() -> appendLog(line)));
        logFileTailer.start(true);
    }

    private void appendLog(String line) {
        Text logEntry;
        ObservableList<String> styleClasses;
        List<Node> texts;
        Text lastText;

        if (!stage.isShowing()) {
            logFileTailer.stop();

            return;
        }
        if (StringUtils.isBlank(line)) {

            return;
        }
        texts = logTextFlow.getChildren();
        if (!texts.isEmpty() && line.trim().startsWith("at")) {
            // exception stacktrace
            lastText = (Text) CollUtil.getLast(texts);
            lastText.setText(lastText.getText() + line + StrPool.LF);
            logScrollPane.setVvalue(1.0);

            return;
        }
        logEntry = new Text(line + StrPool.LF);
        styleClasses = logEntry.getStyleClass();
        styleClasses.add("log-entry");
        if (line.startsWith(Level.DEBUG.name())) {
            styleClasses.add("text-log-debug");
        } else if (line.startsWith(Level.INFO.name())) {
            styleClasses.add("text-log-info");
        } else if (line.startsWith(Level.WARN.name())) {
            styleClasses.add("text-log-warn");
        } else if (line.startsWith(Level.ERROR.name())) {
            styleClasses.add("text-log-error");
        }
        logEntry.setOnMouseClicked(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY || evt.getClickCount() > 1) {

                return;
            }
            ClipboardHelper.setContent(logEntry.getText());
            updateTip(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
        });
        texts.add(logEntry);
        logScrollPane.setVvalue(1.0);
    }

    /**
     * 清空日志
     */
    @FXML
    private void onClearButtonAction() {
        logTextFlow.getChildren().clear();
        updateTip(I18nKeys.LOG_CONSOLE_TIP);
    }

    @FXML
    private void onCopyAllButtonAction() {
        ObservableList<Node> children = logTextFlow.getChildren();
        StringBuilder stringBuilder;

        if (children.isEmpty()) {

            return;
        }
        stringBuilder = new StringBuilder();
        for (Node node : children) {
            stringBuilder.append(((Text) node).getText());
        }
        ClipboardHelper.setContent(stringBuilder.toString());
        updateTip(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
        ToastHelper.showSuccessI18n(I18nKeys.COMMON_MESSAGE_COPY_SUCCEED);
    }

    /**
     * 更新tip显示
     */
    private void updateTip(String messageI18n) {
        if (!Objects.equals(statusLabel.getText(), messageI18n)) {
            statusLabel.setText(I18nHelper.get(messageI18n));
        }
    }

    public void stopListening() {
        if (logFileTailer != null) {
            logFileTailer.stop();
            logFileTailer = null;
            onClearButtonAction();
        }
    }

    public void startListening() {
        if (logFileTailer != null) {

            return;
        }
        startListeningLatestLog();
    }
}
