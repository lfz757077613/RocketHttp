package cn.laifuzhi.RocketHttp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import static cn.laifuzhi.RocketHttp.RocketClient.PROMISE;

/**
 * writeAndFlush发出http请求的listener，负责对channel绑定promise，并且处理请求异常
 */
public final class RocketWriteListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            future.channel().attr(PROMISE).get().setFailure(future.cause());
            if (future.channel().isActive()) {
                future.channel().pipeline().fireExceptionCaught(future.cause());
                return;
            }
            future.channel().attr(PROMISE).set(null);
        }
    }
}
