package io.knifer.freebox.component.converter;

import javafx.util.StringConverter;
import org.apache.commons.lang3.tuple.Pair;

import java.net.NetworkInterface;

/**
 * 网卡-IP 数据转换器
 *
 * @author Knifer
 */
public class NetworkInterfaceAndIP2StringConverter extends StringConverter<Pair<NetworkInterface, String>> {

    @Override
    public String toString(Pair<NetworkInterface, String> object) {
        NetworkInterface networkInterface;

        if (object == null) {
            return null;
        }
        networkInterface = object.getLeft();

        return String.format(
                "%s (%s)",
                object.getRight(),
                networkInterface == null ? "*" : networkInterface.getDisplayName()
        );
    }

    @Override
    public Pair<NetworkInterface, String> fromString(String string) {
        throw new UnsupportedOperationException();
    }
}
