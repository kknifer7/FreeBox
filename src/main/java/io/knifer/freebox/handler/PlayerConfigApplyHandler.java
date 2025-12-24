package io.knifer.freebox.handler;

import io.knifer.freebox.constant.PlayerType;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Consumer;

/**
 * 播放器配置应用处理器
 */
public interface PlayerConfigApplyHandler {

    /**
     * 应用播放器配置，若有需要，让用户选择播放器位置并返回
     * @param playerType 播放器类型
     * @param stage 当前Stage（用于弹出文件选择窗口）
     * @param callback 回调。包含了是否成功, 播放器位置
     */
    void handle(PlayerType playerType, Stage stage, Consumer<Pair<Boolean, String>> callback);
}
