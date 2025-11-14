package io.knifer.freebox.component.node;

import cn.hutool.core.text.StrPool;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.util.CollectionUtil;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.PopOver;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * 首页 - 展示所有IP弹出框
 *
 * @author Knifer
 */
public class IPInfoPopOver extends PopOver {

    public IPInfoPopOver() {
        super();

        ProgressIndicator loadingProgressIndicator = new ProgressIndicator();
        Label ipLabel = new Label();
        StackPane contentStackPane = new StackPane(loadingProgressIndicator, ipLabel);
        LoadNetworkInterfaceDataService service = new LoadNetworkInterfaceDataService();

        service.setOnSucceeded(evt -> {
            Collection<Pair<NetworkInterface, String>> value = service.getValue();
            String info;

            if (CollectionUtil.isEmpty(value)) {

                return;
            }
            info = value.stream()
                    .map(networkInterfaceAndIp ->
                            networkInterfaceAndIp.getKey().getName() + " : " + networkInterfaceAndIp.getValue()
                    )
                    .collect(Collectors.joining(StrPool.LF));
            loadingProgressIndicator.setVisible(false);
            ipLabel.setText(info);
        });
        loadingProgressIndicator.setVisible(false);
        ipLabel.getStyleClass().add("fs-big");
        contentStackPane.setPadding(new Insets(10));
        setOnShowing(evt -> {
            loadingProgressIndicator.setVisible(true);
            ipLabel.setText(StringUtils.EMPTY);
            service.restart();
        });
        setDetachable(false);
        setContentNode(contentStackPane);
    }
}
