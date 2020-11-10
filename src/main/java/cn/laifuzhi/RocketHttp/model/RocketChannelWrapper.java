package cn.laifuzhi.RocketHttp.model;

import io.netty.channel.ChannelFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public final class RocketChannelWrapper {
    // create后，channelFuture中的channel可能还没有连接完成，isActive还是false
    private volatile boolean firstUsed;
    // destroyObject后，channelFuture中的channel可能还没有close完成，isActive还是true
    private volatile boolean destroyed;
    private ChannelFuture connectFuture;

}
