package io.knifer.freebox.handler.impl;

import com.sun.jna.NativeLibrary;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.handler.PlayerCheckHandler;
import io.knifer.freebox.handler.PlayerConfigApplyHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.service.CommandExecService;
import io.knifer.freebox.util.AsyncUtil;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 播放器配置应用处理器实现
 *
 * @author Knifer
 */
@Slf4j
public class PlayerConfigApplyHandlerImpl implements PlayerConfigApplyHandler {

    private Function<String, Boolean> mpvResultChecker;
    private CommandExecService externalPlayerCmdExecService;

    private static final PlayerConfigApplyHandlerImpl INSTANCE = new PlayerConfigApplyHandlerImpl();

    public static PlayerConfigApplyHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void handle(PlayerType playerType, Stage stage, Consumer<Pair<Boolean, String>> callback) {
        switch (playerType) {
            case VLC -> AsyncUtil.execute(() -> {
                NativeDiscovery vlcDiscovery;

                vlcDiscovery = new NativeDiscovery();
                if (vlcDiscovery.discover()) {
                    callback.accept(Pair.of(true, vlcDiscovery.discoveredPath()));
                } else {
                    // 框架无法找到vlc，需要让用户手动选择vlc目录
                    Platform.runLater(() -> {
                        String playerDirectory = choosePlayerDirectory(stage);
                        String backupPlayerDirectory = ConfigHelper.getVlcPath();
                        PlayerType backupPlayerType = ConfigHelper.getPlayerType();
                        boolean isSuccess;

                        try {
                            // 暂时设置vlc目录，便于让handler检测该目录是否可用
                            ConfigHelper.setVlcPath(playerDirectory);
                            ConfigHelper.setPlayerType(PlayerType.VLC);
                            isSuccess = PlayerCheckHandler.select().handle();
                        } finally {
                            // 恢复之前的vlc目录、播放器类型
                            ConfigHelper.setVlcPath(backupPlayerDirectory);
                            ConfigHelper.setPlayerType(backupPlayerType);
                        }
                        log.info("apply player, success={}, path={}", isSuccess, playerDirectory);
                        if (isSuccess) {
                            NativeLibrary.addSearchPath(
                                    SystemUtils.IS_OS_WINDOWS ? "libvlc" : "vlc",
                                    playerDirectory
                            );
                            callback.accept(Pair.of(true, playerDirectory));
                        } else {
                            callback.accept(Pair.of(false, null));
                        }
                    });
                }
            });
            case MPV_EXTERNAL -> {
                if (mpvResultChecker == null) {
                    mpvResultChecker = execResult ->
                            StringUtils.isNotBlank(execResult) &&
                                    execResult.contains("Copyright") &&
                                    execResult.contains("mpv");
                }
                checkExternalPlayer(
                        new String[]{ "mpv", "--version" },
                        mpvResultChecker,
                        () -> {
                            // 自动检测成功
                            log.info("apply player, success=true, path=mpv");
                            callback.accept(Pair.of(true, "mpv"));
                        },
                        () -> {
                            // 自动检测失败，手动选择
                            String mpvPath;

                            mpvPath = choosePlayer(
                                    new FileChooser.ExtensionFilter("mpv", "mpv", "mpv.exe"), stage
                            );
                            if (mpvPath == null) {
                                log.info("apply player, success=false, path=null");
                                callback.accept(Pair.of(false, null));
                            } else {
                                // 手动选择后，再次检测以确定播放器是可用的
                                checkExternalPlayer(
                                        new String[]{ mpvPath, "--version" },
                                        mpvResultChecker,
                                        () -> {
                                            log.info("apply player, success=true, path={}", mpvPath);
                                            callback.accept(Pair.of(true, mpvPath));
                                        },
                                        () -> {
                                            log.info("apply player, success=false, path={}", mpvPath);
                                            callback.accept(Pair.of(false, null));
                                        }
                                );
                            }
                        }
                );
            }
        }
    }

    @Nullable
    private String choosePlayer(FileChooser.ExtensionFilter extensionFilter, Stage stage) {
        FileChooser fileChooser = new FileChooser();
        File externalPlayerFile;

        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setTitle(I18nHelper.get(I18nKeys.SETTINGS_SELECT_PLAYER));
        externalPlayerFile = fileChooser.showOpenDialog(stage);

        return externalPlayerFile == null || !externalPlayerFile.exists() ? null : externalPlayerFile.getAbsolutePath();
    }

    @Nullable
    private String choosePlayerDirectory(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File externalPlayerDirectory;

        directoryChooser.setTitle(I18nHelper.get(I18nKeys.SETTINGS_SELECT_PLAYER_DIRECTORY));
        externalPlayerDirectory = directoryChooser.showDialog(stage);

        return externalPlayerDirectory == null || !externalPlayerDirectory.isDirectory() ?
                null : externalPlayerDirectory.getAbsolutePath();
    }

    /**
     * 自动检测mpv外部播放器
     */
    private void checkExternalPlayer(
            String[] commands,
            Function<String, Boolean> resultChecker,
            Runnable successCallback,
            Runnable failCallback
    ) {
        if (externalPlayerCmdExecService == null) {
            externalPlayerCmdExecService = new CommandExecService();
        }
        externalPlayerCmdExecService.setCommands(commands);
        externalPlayerCmdExecService.setChecker(resultChecker);
        externalPlayerCmdExecService.setOnSucceeded(evt -> {
            Pair<Boolean, String> pair = externalPlayerCmdExecService.getValue();

            if (pair.getLeft()) {
                successCallback.run();
            } else {
                failCallback.run();
            }
        });
        externalPlayerCmdExecService.restart();
    }
}
