package io.knifer.freebox.util;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * 网络工具类
 *
 * @author Knifer
 */
@UtilityClass
public class NetworkUtil {

    private static final Set<String> VIRTUAL_MAC_PREFIXES = ImmutableSet.of(
            "00:50:56",
            "00:0C:29",
            "00:1C:14",
            "00:1C:42",
            "00:05:69",
            "00:03:FF",
            "00:0F:4B",
            "00:16:3E",
            "00:00:27"
    );

    private static final String[] VIRTUAL_KEY_WORD = {
            "vm",
            "virtual",
            "vnet",
            "vbox",
            "vpn"
    };

    public Collection<Pair<NetworkInterface, String>> getAvailableNetworkInterfaceAndIPv4() {
        Enumeration<NetworkInterface> networkInterfaces;
        NetworkInterface networkInterface;
        Enumeration<InetAddress> inetAddresses;
        InetAddress inetAddress;
        boolean isVirtual;
        Pair<NetworkInterface, String> pair;
        Deque<Pair<NetworkInterface, String>> result;

        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        result = new ArrayDeque<>();
        while (networkInterfaces.hasMoreElements()) {
            networkInterface = networkInterfaces.nextElement();
            try {
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            isVirtual = isVirtualNetworkInterface(networkInterface);
            inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                inetAddress = inetAddresses.nextElement();

                if (inetAddress instanceof Inet4Address && inetAddress.isSiteLocalAddress()) {
                    pair = Pair.of(networkInterface, inetAddress.getHostAddress());
                    if (isVirtual) {
                        // 虚拟网卡地址排在最后
                        result.addLast(pair);
                    } else {
                        result.addFirst(pair);
                    }
                }
            }
        }

        return result;
    }

    public boolean isVirtualNetworkInterface(NetworkInterface networkInterface) {
        try {
            return networkInterface.isVirtual() || VIRTUAL_MAC_PREFIXES.contains(
                    formatMacAddress(networkInterface.getHardwareAddress()).substring(0, 8)
            ) || StringUtils.containsAnyIgnoreCase(networkInterface.getDisplayName(), VIRTUAL_KEY_WORD);
        } catch (Exception e) {
            return false;
        }
    }

    public String formatMacAddress(byte[] address) {
        StringJoiner joiner = new StringJoiner(":");

        for (byte b : address) {
            joiner.add(String.format("%02X", b));
        }

        return joiner.toString().toUpperCase();
    }

    public boolean isPortUsing(String hostname, int port, int timeout) {
        boolean flag = false;
        InetSocketAddress address = null;

        try {
            address = new InetSocketAddress(hostname, port);
        } catch (IllegalArgumentException ignored) {}
        try (Socket socket = new Socket()) {
            socket.connect(address, timeout);
            flag = true;
        } catch (IOException ignored) {}

        return flag;
    }
}
