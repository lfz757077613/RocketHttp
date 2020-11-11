package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketChannelWrapper;
import lombok.AllArgsConstructor;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.util.function.BiConsumer;

@AllArgsConstructor
public final class RocketChannelConsumer implements BiConsumer<String, Throwable> {
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final RocketChannelWrapper channelWrapper;
    private final String channelKey;

    @Override
    public void accept(String resp, Throwable throwable) {
        try {
            // resp和throwable只会有一个有值
            if (resp != null) {
                channelPool.returnObject(channelKey, channelWrapper);
            }
            if (throwable != null) {
                channelPool.invalidateObject(channelKey, channelWrapper);
            }
        } catch (Exception e) {
            channelWrapper.getConnectFuture().channel().close();
        }
    }

}
