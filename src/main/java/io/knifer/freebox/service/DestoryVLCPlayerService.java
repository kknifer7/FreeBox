package io.knifer.freebox.service;

import io.knifer.freebox.component.node.VLCPlayer;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;

/**
 * 释放VLC播放器服务
 *
 * @author Knifer
 */
@AllArgsConstructor
public class DestoryVLCPlayerService extends Service<Void> {

    private final VLCPlayer player;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                player.destroy();

                return null;
            }
        };
    }
}
