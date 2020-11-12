package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketChannelWrapper;
import cn.laifuzhi.RocketHttp.model.RocketResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.util.function.BiConsumer;

@Slf4j
@AllArgsConstructor
public final class RocketChannelConsumer implements BiConsumer<RocketResponse, Throwable> {
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final RocketChannelWrapper channelWrapper;
    private final String channelKey;

    @Override
    public void accept(RocketResponse resp, Throwable throwable) {
        try {
            // resp和throwable只会有一个有值
            if (resp != null) {
                channelPool.returnObject(channelKey, channelWrapper);
            }
            if (throwable != null) {
                channelPool.invalidateObject(channelKey, channelWrapper);
            }
        } catch (Exception e) {
            // 按理说invalidateObject不会抛出异常，这里永远不会执行
            log.error("RocketChannelConsumer error", e);
            channelPool.returnObject(channelKey, channelWrapper);
            channelWrapper.getConnectFuture().channel().close();
        }
    }

}
