package cn.laifuzhi.RocketHttp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;

import static cn.laifuzhi.RocketHttp.RocketClient.PROMISE;

/**
 * writeAndFlush发出http请求的listener，负责对channel绑定promise，并且处理请求异常
 */
@AllArgsConstructor
public final class RocketWriteListener implements ChannelFutureListener {
    private final Promise<String> promise;

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        future.channel().attr(PROMISE).set(promise);
        if (!future.isSuccess()) {
            promise.setFailure(future.cause());
            if (future.channel().isActive()) {
                future.channel().pipeline().fireExceptionCaught(future.cause());
                return;
            }
            future.channel().attr(PROMISE).set(null);
        }
    }
}
