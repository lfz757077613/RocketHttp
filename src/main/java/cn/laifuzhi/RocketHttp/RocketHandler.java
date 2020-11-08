package cn.laifuzhi.RocketHttp;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static cn.laifuzhi.RocketHttp.RocketClient.PROMISE;

@Slf4j
@Sharable
@AllArgsConstructor
public final class RocketHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    // 收集所有活着的channel，以后备用
    public static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Promise<String> promise = ctx.channel().attr(PROMISE).get();
        if (promise == null || promise.isDone()) {
            // 永远不会有这种情况才对，否则是netty本身有bug
            log.error("promise already done channel:{}", ctx.channel().localAddress().toString() + ctx.channel().remoteAddress().toString());
            ctx.close();
            return;
        }
        promise.setSuccess(msg.content().toString(CharsetUtil.UTF_8));
        ctx.channel().attr(PROMISE).set(null);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelGroup.add(ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelInactive channel:{}", ctx.channel().localAddress().toString() + ctx.channel().remoteAddress().toString());
        Promise<String> promise = ctx.channel().attr(PROMISE).get();
        if (promise != null) {
            if (!promise.isDone()) {
                promise.setFailure(new IOException("channel close"));
            }
            ctx.channel().attr(PROMISE).set(null);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught channel:{}", ctx.channel().localAddress().toString() + ctx.channel().remoteAddress().toString());
        Promise<String> promise = ctx.channel().attr(PROMISE).get();
        if (promise != null) {
            if (!promise.isDone()) {
                promise.setFailure(cause);
            }
            ctx.channel().attr(PROMISE).set(null);
        }
        ctx.close();
    }
}
