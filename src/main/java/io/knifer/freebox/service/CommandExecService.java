package io.knifer.freebox.service;

import cn.hutool.core.util.RuntimeUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Function;

/**
 * 命令执行服务
 *
 * @author Knifer
 */
@Slf4j
@Setter
public class CommandExecService extends Service<Pair<Boolean, String>> {

    /**
     * 要执行的命令
     */
    private String[] commands;
    /**
     * 命令执行成功检查（可选）
     */
    private Function<String, Boolean> checker;

    @Override
    protected Task<Pair<Boolean, String>> createTask() {
        return new Task<>() {
            @Override
            protected Pair<Boolean, String> call() {
                String result = null;
                boolean success;

                try {
                    result = RuntimeUtil.execForStr(commands);
                    success = checker == null || checker.apply(result);
                } catch (Exception e) {
                    log.warn("command run failed", e);
                    success = false;
                }
                log.info("\ncommand:\n{}\nresult:\n{}\nsuccess:{}", commands, result, success);

                return Pair.of(success, result);
            }
        };
    }
}
