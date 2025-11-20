package io.knifer.freebox.handler.impl;

import cn.hutool.core.text.StrPool;
import io.knifer.freebox.handler.NoParameterHandler;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.NetworkUtil;
import io.knifer.freebox.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 确保应用单例
 *
 * @author Knifer
 */
@Slf4j
public class SingleInstanceApplicationHandler implements NoParameterHandler {

    private final static String WAKEUP = "wakeup";
    private final static Path LOCKFILE_PATH = Path.of(".lock");

    @Override
    public void handle() {
        String port;

        try {
            if (!Files.exists(LOCKFILE_PATH)) {
                createLockfile();

                return;
            }
            port = Files.readString(LOCKFILE_PATH);
        } catch (IOException e) {
            log.error("unknown exception", e);

            return;
        }
        if (StringUtils.isBlank(port) || !ValidationUtil.isPort(port)) {
            createLockfile();

            return;
        }
        if (NetworkUtil.isPortUsing("localhost", NumberUtils.toInt(port), 1000)) {
            wakeupExistInstance(port);
            System.exit(0);
        } else {
            createLockfile();
        }
    }

    private void createLockfile() {
        try {
            Files.write(LOCKFILE_PATH, String.valueOf(ConfigHelper.getHttpPort()).getBytes());
        } catch (IOException e) {
            log.error("create lockfile failed", e);
        }
    }

    private void wakeupExistInstance(String port) {
        log.info("exists application instance");
        try {
            HttpUtil.getAsync("http://127.0.0.1:" + port + StrPool.SLASH + WAKEUP)
                    .get(6, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }

    public static void release() {
        try {
            Files.delete(LOCKFILE_PATH);
        } catch (IOException e) {
            log.error("release lockfile failed", e);
        }
    }
}
