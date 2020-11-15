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
            // resp和throwable只会有一个有值，只有正常获得http响应的连接才会归还连接池复用，否则可能有各种并发问题
            // PS: 请求1，发生错误 -> 归还连接 -> 获取连接发出请求2 -> 请求1响应 -> 会把请求1的响应当做请求2的响应
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
