package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static cn.laifuzhi.RocketHttp.RocketClient.FUTURE;

@Slf4j
@AllArgsConstructor
public final class RocketConnectListener implements ChannelFutureListener {
    private final DefaultFullHttpRequest httpRequest;

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        CompletableFuture<RocketResponse> result = future.channel().attr(FUTURE).get();
        try {
            if (!future.isSuccess()) {
                result.completeExceptionally(future.cause());
                return;
            }
            future.channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener());
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
    }
}
