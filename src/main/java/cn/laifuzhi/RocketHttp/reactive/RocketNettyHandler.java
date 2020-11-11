package cn.laifuzhi.RocketHttp.reactive;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static cn.laifuzhi.RocketHttp.RocketClient.FUTURE;
import static cn.laifuzhi.RocketHttp.Utils.getSocketName;

/**
 * promise一旦set完值之后，就可以被连接池复用了，
 */
@Slf4j
@Sharable
public final class RocketNettyHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        CompletableFuture<String> result = ctx.channel().attr(FUTURE).get();
        if (result == null) {
            // 永远不会有这种情况才对，否则是netty本身有bug
            log.error("promise null channel:{}", getSocketName(ctx.channel()));
            ctx.close();
            return;
        }
        // 此时可能已经超时，被设置了timeoutException
        result.complete(msg.content().toString(CharsetUtil.UTF_8));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelInactive channel:{}", getSocketName(ctx.channel()));
        CompletableFuture<String> result = ctx.channel().attr(FUTURE).get();
        if (result != null) {
            result.completeExceptionally(new IOException("channel close"));
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught channel:{}", getSocketName(ctx.channel()));
        CompletableFuture<String> result = ctx.channel().attr(FUTURE).get();
        if (result != null) {
            result.completeExceptionally(cause);
        }
        ctx.close();
    }
}
