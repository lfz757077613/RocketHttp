package cn.laifuzhi.RocketHttp;

import io.netty.channel.Channel;

import java.net.SocketAddress;

public final class Utils {
    private static final String UNDERLINE = "_";
    private static final String COLON = ":";

    public static String getSocketName(Channel channel) {
        SocketAddress remoteAddress = channel.remoteAddress();
        SocketAddress localAddress = channel.localAddress();
        return String.join(UNDERLINE,
                remoteAddress == null ? "" : remoteAddress.toString(),
                localAddress == null ? "" : localAddress.toString());
    }

    public static String joinHostPort(String host, int port) {
        return String.join(COLON, host, String.valueOf(port));
    }

    public static String[] splitHostPort(String hostPort) {
        return hostPort.split(COLON);
    }
}
