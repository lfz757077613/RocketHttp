package cn.laifuzhi.RocketHttp;


import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import io.netty.channel.Channel;

import java.util.List;

public final class Utils {
    private static final Joiner JOINER_UNDERLINE = Joiner.on("_").skipNulls();
    private static final Joiner JOINER_COLON = Joiner.on(":").skipNulls();
    private static final Splitter SPLITTER_COLON = Splitter.on(":").trimResults().omitEmptyStrings();

    public static String getSocketName(Channel channel) {
        return JOINER_UNDERLINE.join(channel.remoteAddress(), channel.localAddress());
    }

    public static String joinHostPort(String host, int port) {
        return JOINER_COLON.join(host, port);
    }

    public static List<String> splitHostPort(String hostPort) {
        return SPLITTER_COLON.splitToList(hostPort);
    }
}
