package cn.laifuzhi.RocketHttp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.laifuzhi.RocketHttp.Utils.getSocketName;
import static cn.laifuzhi.RocketHttp.Utils.joinHostPort;
import static cn.laifuzhi.RocketHttp.Utils.splitHostPort;

/*
                       _oo0oo_
                      o8888888o
                      88" . "88
                      (| -_- |)
                      0\  =  /0
                    ___/`---'\___
                  .' \\|     |// '.
                 / \\|||  :  |||// \
                / _||||| -:- |||||- \
               |   | \\\  -  /// |   |
               | \_|  ''\---/''  |_/ |
               \  .-\__  '-'  ___/-. /
             ___'. .'  /--.--\  `. .'___
          ."" '<  `.___\_<|>_/___.' >' "".
         | | :  `- \`.;`\ _ /`;.`/ - ` : | |
         \  \ `_.   \_ __\ /__ _/   .-` /  /
     =====`-.____`.___ \_____/___.-`___.-'=====
                       `=---='
     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

               佛祖保佑         永无BUG
*/
@Slf4j
public final class RocketClient implements Closeable {
    public static final AttributeKey<Promise<String>> PROMISE = AttributeKey.valueOf("PROMISE");

    private final Bootstrap bootstrap;
    private final GenericKeyedObjectPool<String, Channel> channelPool;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public RocketClient() {
        bootstrap = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
//                .option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT)
//                .option(ChannelOption.AUTO_CLOSE, false)
//                .option(ChannelOption.SO_REUSEADDR, true)
//                .option(ChannelOption.SO_KEEPALIVE, false)
//                .option(ChannelOption.TCP_NODELAY, true)
//                关了AUTO_READ需要自己手动读取响应
//                .option(ChannelOption.AUTO_READ, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
//                .option(ChannelOption.SO_RCVBUF, 32 * 1024)
//                .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new RocketIdleHandler(0, 0, 60))
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(10 * 1024 * 1024))
                                .addLast(new HttpContentDecompressor())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new RocketHandler());
                    }
                });

        GenericKeyedObjectPoolConfig<Channel> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotalPerKey(100);
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

    public String execute(String host, int port, String uri) throws Exception {
        if (isClosed.get()) {
            throw new RuntimeException("RocketClient already close");
        }
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri, Unpooled.EMPTY_BUFFER);
        HttpHeaders headers = httpRequest.headers();
        headers.set(HttpHeaderNames.HOST, host);
        headers.set(HttpHeaderNames.USER_AGENT, "RocketClient");
        headers.set(HttpHeaderNames.ACCEPT, "*/*");
        Channel channel = channelPool.borrowObject(joinHostPort(host, port));
        try {
            Promise<String> promise = channel.eventLoop().newPromise();
            // 执行writeAndFlush时，如果channel已经关闭，则ChannelFutureListener中的channel pipeline已经没有自定义handler了
            // 所以单纯用FIRE_EXCEPTION_ON_FAILURE没有办法处理promise，因为RocketHandler已经没了
            channel.writeAndFlush(httpRequest).addListener(new RocketWriteListener(promise));
            return promise.get();
        } finally {
            channelPool.returnObject(joinHostPort(host, port), channel);
        }
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            log.info("RocketClient closing...");
            bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
            channelPool.close();
        }
    }
}