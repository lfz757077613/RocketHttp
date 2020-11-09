package cn.laifuzhi.RocketHttp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import static cn.laifuzhi.RocketHttp.RocketClient.PROMISE;

/**
 * writeAndFlush发出http请求的listener，负责对channel绑定promise，并且处理请求异常
 */
@Slf4j
public final class RocketWriteListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            Promise<String> promise = future.channel().attr(PROMISE).get();
            promise.setFailure(future.cause());
            if (future.channel().isActive()) {
                future.channel().pipeline().fireExceptionCaught(future.cause());
            }
            // isActive为false说明写入发生错误时，连接已经关闭
        }
    }
}
