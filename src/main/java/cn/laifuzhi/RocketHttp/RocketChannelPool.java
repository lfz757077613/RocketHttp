package cn.laifuzhi.RocketHttp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import static cn.laifuzhi.RocketHttp.Utils.getSocketName;
import static cn.laifuzhi.RocketHttp.Utils.joinHostPort;
import static cn.laifuzhi.RocketHttp.Utils.splitHostPort;

@Slf4j
public final class RocketChannelPool implements Closeable {
    private GenericKeyedObjectPool<String, Channel> channelPool;

    public RocketChannelPool(Bootstrap bootstrap, int maxCountPerHost) {
        GenericKeyedObjectPoolConfig<Channel> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotalPerKey(maxCountPerHost);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWaitMillis(3000);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(10000);
        poolConfig.setMinEvictableIdleTimeMillis(10000);

        channelPool = new GenericKeyedObjectPool<>(new KeyedPooledObjectFactory<String, Channel>() {
            @Override
            public PooledObject<Channel> makeObject(String key) throws Exception {
                log.debug("makeObject key:{}", key);
                List<String> hostPort = splitHostPort(key);
                ChannelFuture channelFuture = bootstrap.connect(hostPort.get(0), Integer.parseInt(hostPort.get(1))).syncUninterruptibly();
                return new DefaultPooledObject<>(channelFuture.channel());
            }

            @Override
            public void destroyObject(String key, PooledObject<Channel> p) throws Exception {
                log.debug("destroyObject key:{} channel:{}", key, getSocketName(p.getObject()));
                p.getObject().close().syncUninterruptibly();
            }

            @Override
            public boolean validateObject(String key, PooledObject<Channel> p) {
                log.debug("validateObject key:{} channel:{}", key, getSocketName(p.getObject()));
                return p.getObject().isActive();
            }

            @Override
            public void activateObject(String key, PooledObject<Channel> p) throws Exception {
                //忽略
            }

            @Override
            public void passivateObject(String key, PooledObject<Channel> p) throws Exception {
                //忽略
            }
        }, poolConfig);
    }

    public Channel acquire(String host, int port) throws Exception {
        return channelPool.borrowObject(joinHostPort(host, port));
    }

    public void release(String host, int port, Channel channel) throws Exception {
        channelPool.returnObject(joinHostPort(host, port), channel);
    }

    @Override
    public void close() throws IOException {
        channelPool.close();
    }
}
