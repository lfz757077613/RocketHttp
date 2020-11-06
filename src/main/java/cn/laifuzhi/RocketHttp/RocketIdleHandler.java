package cn.laifuzhi.RocketHttp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 既可以回收空闲连接，又可以解决假死连接造成的各种问题。最终都会触发channel的close
public final class RocketIdleHandler extends IdleStateHandler {
    public RocketIdleHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        log.debug("channelIdle channel:{}", ctx.channel().localAddress().toString() + ctx.channel().remoteAddress().toString());
        ctx.close();
    }
}
