package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketResponse;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static cn.laifuzhi.RocketHttp.RocketClient.FUTURE;
import static cn.laifuzhi.RocketHttp.Utils.getSocketName;

@Slf4j
@Sharable
public final class RocketNettyHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        CompletableFuture<RocketResponse> result = ctx.channel().attr(FUTURE).get();
        if (result == null) {
            // 永远不会有这种情况才对，否则是netty本身有bug
            log.error("promise null channel:{}", getSocketName(ctx.channel()));
            ctx.close();
            return;
        }
        // 此时可能已经超时，被设置了timeoutException
        RocketResponse response = new RocketResponse();
        response.setCode(msg.status().code());
        response.setVersion(msg.protocolVersion());
        response.setHeaders(msg.headers());
        byte[] body = new byte[msg.content().readableBytes()];
        msg.content().readBytes(body);
        response.setBody(body);
        result.complete(response);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelInactive channel:{}", getSocketName(ctx.channel()));
        CompletableFuture<RocketResponse> result = ctx.channel().attr(FUTURE).get();
        if (result != null) {
            result.completeExceptionally(new IOException("channel close"));
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught channel:{}", getSocketName(ctx.channel()));
        CompletableFuture<RocketResponse> result = ctx.channel().attr(FUTURE).get();
        if (result != null) {
            result.completeExceptionally(cause);
        }
        ctx.close();
    }
}
