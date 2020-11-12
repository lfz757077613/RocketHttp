package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static cn.laifuzhi.RocketHttp.RocketClient.FUTURE;

/**
 * writeAndFlush发出http请求的listener，负责对channel绑定promise，并且处理请求异常
 */
@Slf4j
public final class RocketWriteListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            CompletableFuture<RocketResponse> result = future.channel().attr(FUTURE).get();
            result.completeExceptionally(future.cause());
            // isActive为false说明写入发生错误时，连接已经关闭
        }
    }
}
