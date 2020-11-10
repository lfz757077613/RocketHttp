package cn.laifuzhi.RocketHttp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static cn.laifuzhi.RocketHttp.Utils.getSocketName;

@Slf4j
public final class RocketIdleHandler extends IdleStateHandler {
    public RocketIdleHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        log.debug("channelIdle channel:{}", getSocketName(ctx.channel()));
        ctx.close();
    }
}
