package io.knifer.freebox.service;

import io.knifer.freebox.util.NetworkUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * 读取网卡配置服务
 *
 * @author Knifer
 */
@Slf4j
public class LoadNetworkInterfaceDataService extends Service<Collection<Pair<NetworkInterface, String>>> {

    @Override
    protected Task<Collection<Pair<NetworkInterface, String>>> createTask() {
        return new Task<>() {
            @Override
            protected Collection<Pair<NetworkInterface, String>> call() {
                return NetworkUtil.getAvailableNetworkInterfaceAndIPv4();
            }
        };
    }
}
