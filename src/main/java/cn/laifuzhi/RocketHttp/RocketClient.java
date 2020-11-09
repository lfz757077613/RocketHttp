package cn.laifuzhi.RocketHttp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
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
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
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
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final RocketHandler rocketHandler = new RocketHandler();

    public RocketClient() {
        // 增加Native transports能力，因为会按顺序在多个目录加载本地库，目录中没有本地库文件就会打印些debug级别的异常，最终找到本地库会打印Successfully loaded the library
        // 类似这种可以忽略netty_transport_native_kqueue_x86_64 cannot be loaded
        EventLoopGroup eventLoopGroup = null;
        Class<? extends Channel> channelClass = null;
        if (Epoll.isAvailable()) {
            eventLoopGroup = new EpollEventLoopGroup();
            channelClass = EpollSocketChannel.class;
        }
        if (KQueue.isAvailable()) {
            eventLoopGroup = new KQueueEventLoopGroup();
            channelClass = KQueueSocketChannel.class;
        }
        bootstrap = new Bootstrap().group(eventLoopGroup != null ? eventLoopGroup : new NioEventLoopGroup())
                .channel(channelClass != null ? channelClass : NioSocketChannel.class)
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
                                .addLast(rocketHandler);
                    }
                });

        GenericKeyedObjectPoolConfig<RocketChannelWrapper> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotalPerKey(100);
        poolConfig.setMaxIdlePerKey(100);
        // 纯异步则不能阻塞了
        poolConfig.setBlockWhenExhausted(false);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        // 每60s清理一次空闲时间超过60秒的连接，调用destroyObject
        poolConfig.setTimeBetweenEvictionRunsMillis(60000);
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        channelPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<String, RocketChannelWrapper>() {
            @Override
            public RocketChannelWrapper create(String key) throws Exception {
                log.debug("create key:{}", key);
                List<String> hostPort = splitHostPort(key);
                return new RocketChannelWrapper(true, false, bootstrap.connect(hostPort.get(0), Integer.parseInt(hostPort.get(1))));
            }

            @Override
            public PooledObject<RocketChannelWrapper> wrap(RocketChannelWrapper value) {
                return new DefaultPooledObject<>(value);
            }

            @Override
            public void destroyObject(String key, PooledObject<RocketChannelWrapper> p) throws Exception {
                log.debug("destroyObject key:{} channel:{}", key, getSocketName(p.getObject().getChannelFuture().channel()));
                // 纯异步则不能阻塞，close也就不能调用syncUninterruptibly()
                // 所以执行完destroyObject后，可能channel的isActive仍为true
                p.getObject().setDestroyed(true);
                p.getObject().getChannelFuture().channel().close();
            }

            @Override
            public boolean validateObject(String key, PooledObject<RocketChannelWrapper> p) {
                // 当设置了testOnBorrow为true的话，会对makeObject新建的对象调用validateObject，如果false则会抛出异常
                // 所以当isFirstUsed时(刚刚创建的连接)，validateObject直接返回true
                log.debug("validateObject key:{} firstUsed:{} destroyed:{} channel:{}",
                        key, p.getObject().isFirstUsed(), p.getObject().isDestroyed(), getSocketName(p.getObject().getChannelFuture().channel()));
                if (p.getObject().isDestroyed()) {
                    return false;
                }
                return p.getObject().isFirstUsed() || p.getObject().getChannelFuture().channel().isActive();
            }
        }, poolConfig);
    }

    public Promise<String> execute(String host, int port, String uri) throws Exception {
        if (isClosed.get()) {
            throw new RuntimeException("RocketClient already close");
        }
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri, Unpooled.EMPTY_BUFFER);
        HttpHeaders headers = httpRequest.headers();
        headers.set(HttpHeaderNames.HOST, host);
        headers.set(HttpHeaderNames.USER_AGENT, "RocketClient");
        headers.set(HttpHeaderNames.ACCEPT, "*/*");
        RocketChannelWrapper channelWrapper = channelPool.borrowObject(joinHostPort(host, port));
        // 连接归还不占用当前channel的eventLoop线程
//         Promise<String> promise = channelWrapper.getChannelFuture().channel().eventLoop().newPromise();
        Promise<String> promise = GlobalEventExecutor.INSTANCE.newPromise();
        promise.addListener(future -> channelPool.returnObject(joinHostPort(host, port), channelWrapper));
        channelWrapper.getChannelFuture().channel().attr(PROMISE).set(promise);
        if (channelWrapper.isFirstUsed()) {
            channelWrapper.setFirstUsed(false);
            channelWrapper.getChannelFuture().addListener((ChannelFutureListener) future -> future.channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener()));
            return promise;
        }
        // 执行writeAndFlush时，如果channel已经关闭，则ChannelFutureListener中的channel pipeline已经没有自定义handler了
        // 所以单纯用FIRE_EXCEPTION_ON_FAILURE没有办法处理promise，因为RocketHandler已经没了
        channelWrapper.getChannelFuture().channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener());
        return promise;
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
