package io.knifer.freebox.net;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.CheckPortUsingService;
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
            CheckPortUsingService httpCheckPortUsingService;
            CheckPortUsingService wsCheckPortUsingService;

            if (ipChangedFlag) {
                ToastHelper.showInfoI18n(I18nKeys.INIT_IP_CHANGED_MESSAGE);
                callback.run();

                return;
            }
            if (httpAutoStartFlag) {
                port = ConfigHelper.getHttpPort();
                httpCheckPortUsingService = new CheckPortUsingService(ip, port);
                httpCheckPortUsingService.setOnSucceeded(ignore -> {
                    Boolean isPortUsing = httpCheckPortUsingService.getValue();
                    Integer httpPort = httpCheckPortUsingService.getPort();

                    if (isPortUsing) {
                        ToastHelper.showError(String.format(
                                I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                                httpPort
                        ));
                    } else {
                        httpServer.start(ip, httpPort);
                        log.info("http service started");
                    }
                    if (!wsAutoStartFlag) {
                        // 启动http服务后，如果不需要再启动ws服务，就直接触发init的完成回调
                        callback.run();
                    }
                });
                httpCheckPortUsingService.start();
            }
            if(wsAutoStartFlag) {
                port = ConfigHelper.getWsPort();
                wsCheckPortUsingService = new CheckPortUsingService(ip, port);
                wsCheckPortUsingService.setOnSucceeded(ignore -> {
                    Boolean isPortUsing = wsCheckPortUsingService.getValue();
                    Integer wsPort = wsCheckPortUsingService.getPort();

                    if (isPortUsing) {
                        ToastHelper.showError(String.format(
                                I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                                wsPort
                        ));
                    } else {
                        wsServer.start(ip, wsPort);
                    }
                    callback.run();
                });
                wsCheckPortUsingService.start();
            }
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
