package cn.laifuzhi.RocketHttp.listener;

import cn.laifuzhi.RocketHttp.model.RocketChannelWrapper;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.AllArgsConstructor;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

@AllArgsConstructor
public final class RocketRespListener implements GenericFutureListener<Future<String>> {
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final RocketChannelWrapper channelWrapper;
    private final String channelKey;

    @Override
    public void operationComplete(Future<String> future) throws Exception {
        if (future.isSuccess()) {
            channelPool.returnObject(channelKey, channelWrapper);
            return;
        }
        channelPool.invalidateObject(channelKey, channelWrapper);
    }
}
