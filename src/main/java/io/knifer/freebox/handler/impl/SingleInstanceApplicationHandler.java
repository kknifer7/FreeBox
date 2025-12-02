package io.knifer.freebox.handler.impl;

import cn.hutool.core.text.StrPool;
import io.knifer.freebox.handler.NoParameterHandler;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 确保应用单例
 * 参考：<a href="https://github.com/prat-man/unique4j/blob/main/src/main/java/in/pratanumandal/unique4j/Unique4j.java">Unique4j</a>
 * @author Knifer
 */
@Slf4j
public class SingleInstanceApplicationHandler implements NoParameterHandler {

    private ServerSocket server;

    private final static String WAKEUP = "wakeup";
    private final static Path LOCKFILE_PATH = StorageHelper.getLocalStoragePath().resolve(".lock");

    private final static SingleInstanceApplicationHandler INSTANCE = new SingleInstanceApplicationHandler();

    public static SingleInstanceApplicationHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void handle() {
        String port;

        try {
            if (!Files.exists(LOCKFILE_PATH)) {
                startServer();

                return;
            }
            port = Files.readString(LOCKFILE_PATH);
        } catch (IOException e) {
            log.error("unknown exception", e);

            return;
        }
        if (StringUtils.isBlank(port) || !ValidationUtil.isPort(port)) {
            startServer();

            return;
        }
        // 尝试唤醒已存在的实例，如果失败则启动服务
        if (tryToWakeupExistedInstance(Integer.parseInt(port))) {
            log.info("wakeup exists application");
            System.exit(0);
        } else {
            startServer();
        }
    }

    /**
     * 启动服务（会创建锁文件）
     */
    private void startServer() {
        int port = 10000;

        do {
            try {
                server = new ServerSocket(port, 0, InetAddress.getLocalHost());
            } catch (IOException e) {
                server = null;
                port++;
            }
        } while (server == null);
        createLockfile(port);
        new Thread(() -> {
            while (!server.isClosed()) {
                try {
                    Socket socket = server.accept();

                    new Thread(() -> {
                        try (socket) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            OutputStream out = socket.getOutputStream();
                            String msg = reader.readLine();

                            if (WAKEUP.equals(msg)) {
                                out.write(WAKEUP.getBytes());
                                out.write(StrPool.LF.getBytes());
                                wakeup();
                            }
                        } catch (IOException e) {
                            log.error("read socket failed", e);
                        }
                    }).start();
                } catch (IOException e) {
                    if (!server.isClosed()) {
                        log.error("accept socket failed", e);
                    }
                }
            }
        }).start();
    }

    private void createLockfile(int port) {
        try {
            Files.write(LOCKFILE_PATH, String.valueOf(port).getBytes());
            log.info("lockfile created: {}", LOCKFILE_PATH);
        } catch (IOException e) {
            log.error("create lockfile failed", e);
        }
    }

    private void wakeup() {
        log.info("user try starting another application instance, wakeup exists application");
        Platform.runLater(() -> Window.getWindows().forEach(window -> {
            if (window instanceof Stage stage) {
                // 唤起窗口
                stage.toFront();
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
            }
        }));
    }

    private boolean tryToWakeupExistedInstance(int port) {
        try (
                Socket socket = new Socket(InetAddress.getLocalHost(), port);
                OutputStream out = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.write(WAKEUP.getBytes());
            out.write(StrPool.LF.getBytes());
            out.flush();
            if (WAKEUP.equals(reader.readLine())) {

                return true;
            }
        } catch (Exception e) {
            log.warn("wakeup failed, maybe another app is using port: {}", port, e);
        }

        return false;
    }

    public void releaseLock() {
        try {
            server.close();
            Files.delete(LOCKFILE_PATH);
        } catch (IOException e) {
            log.error("release lock failed", e);
        }
    }
}
