package cn.laifuzhi.RocketHttp.listener;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static cn.laifuzhi.RocketHttp.RocketClient.PROMISE;

@Slf4j
@AllArgsConstructor
public final class RocketConnectListener implements ChannelFutureListener {
    private final DefaultFullHttpRequest httpRequest;
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        Promise<String> promise = future.channel().attr(PROMISE).get();
        try {
            if (!future.isSuccess()) {
                promise.tryFailure(future.cause());
                return;
            }
            future.channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener());
        } catch (Exception e) {
            promise.tryFailure(e);
        }
    }
}
