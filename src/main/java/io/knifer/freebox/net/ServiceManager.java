package io.knifer.freebox.net;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import javafx.concurrent.Service;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * 服务管理器
 *
 * @author Knifer
 */
@Getter
@Slf4j
public class ServiceManager {

    private final FreeBoxHttpServerHolder httpServer = new FreeBoxHttpServerHolder();

    private final KebSocketServerHolder wsServer;

    private final ClientManager clientManager;

    public ServiceManager(ClientManager clientManager) {
        this.clientManager = clientManager;
        this.wsServer = new KebSocketServerHolder(clientManager);
    }

    public void init(Runnable callback) {
        String ip = ConfigHelper.getServiceIPv4();
        Service<Collection<Pair<NetworkInterface, String>>> loadNetworkInterfaceService;
        boolean httpAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp());
        boolean wsAutoStartFlag = BooleanUtils.toBoolean(ConfigHelper.getAutoStartWs());
        boolean needAutoStart = httpAutoStartFlag || wsAutoStartFlag;

        if (!needAutoStart) {
            callback.run();

            return;
        }
        if (StringUtils.isBlank(ip)) {
            log.info("no available ip, skip auto start service");
            callback.run();

            return;
        }
        loadNetworkInterfaceService = new LoadNetworkInterfaceDataService();
        loadNetworkInterfaceService.setOnSucceeded(evt -> {
            boolean ipChangedFlag = !ip.equals(BaseValues.ANY_LOCAL_IP) &&
                    loadNetworkInterfaceService.getValue()
                            .stream()
                            .noneMatch(pair -> ip.equals(pair.getValue()));
            int port;

            if (ipChangedFlag) {
                ToastHelper.showInfoI18n(I18nKeys.INIT_IP_CHANGED_MESSAGE);
                callback.run();

                return;
            }
            if (httpAutoStartFlag) {
                port = ConfigHelper.getHttpPort();
                httpServer.start(ip, port);
            }
            if(wsAutoStartFlag) {
                wsServer.start(ip, ConfigHelper.getWsPort());
            }
            callback.run();
        });
        loadNetworkInterfaceService.start();
    }

    public void destroy() {
        if (httpServer.isRunning()) {
            log.info("stopping http service......");
            httpServer.stopBlocking();
        }
        if (wsServer.isRunning()) {
            log.info("stopping websocket service......");
            wsServer.stopBlocking();
        }
    }
}
