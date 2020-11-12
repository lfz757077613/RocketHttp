package cn.laifuzhi.RocketHttp;

import cn.laifuzhi.RocketHttp.model.RocketChannelWrapper;
import cn.laifuzhi.RocketHttp.model.RocketConfig;
import cn.laifuzhi.RocketHttp.model.RocketRequest;
import cn.laifuzhi.RocketHttp.model.RocketResponse;
import cn.laifuzhi.RocketHttp.reactive.RocketChannelConsumer;
import cn.laifuzhi.RocketHttp.reactive.RocketConnectListener;
import cn.laifuzhi.RocketHttp.reactive.RocketDIYConsumer;
import cn.laifuzhi.RocketHttp.reactive.RocketDIYHandler;
import cn.laifuzhi.RocketHttp.reactive.RocketNettyHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    public static final AttributeKey<CompletableFuture<RocketResponse>> FUTURE = AttributeKey.valueOf("FUTURE");
    private RocketConfig config;
    private final Bootstrap bootstrap;
    private final GenericKeyedObjectPool<String, RocketChannelWrapper> channelPool;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final RocketNettyHandler rocketNettyHandler = new RocketNettyHandler();
    // 负责处理请求超时和http请求的异步回调(包含调用用户自定义回调handler、归还或销毁channel)，不占用netty线程
    private final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup();

    public RocketClient(RocketConfig config) {
        this.config = config;
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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectTimeout())
//                .option(ChannelOption.SO_RCVBUF, 32 * 1024)
//                .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(config.getHttpMaxContent()))
                                .addLast(new HttpContentDecompressor())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(rocketNettyHandler);
                    }
                });

        GenericKeyedObjectPoolConfig<RocketChannelWrapper> poolConfig = new GenericKeyedObjectPoolConfig<>();
        poolConfig.setMaxTotalPerKey(config.getMaxConnectPerHost());
        poolConfig.setMaxIdlePerKey(config.getMaxConnectPerHost());
        // 纯异步则不能阻塞了
        poolConfig.setBlockWhenExhausted(config.isBlockWhenExhausted());
        poolConfig.setMaxWaitMillis(config.getBlockTimeout());
        poolConfig.setTestOnBorrow(true);
        // 每EvictIdleConnectPeriod秒清理一次空闲时间超过IdleConnectKeepAliveTime秒的连接，调用destroyObject
        poolConfig.setTimeBetweenEvictionRunsMillis(config.getEvictIdleConnectPeriod());
        poolConfig.setMinEvictableIdleTimeMillis(config.getIdleConnectKeepAliveTime());
        // -1代表每次检查全部空闲连接是否超过MinEvictableIdleTimeMillis，超过则evict
        poolConfig.setNumTestsPerEvictionRun(-1);
        channelPool = new GenericKeyedObjectPool<>(new BaseKeyedPooledObjectFactory<String, RocketChannelWrapper>() {
            @Override
            public RocketChannelWrapper create(String key) throws Exception {
                log.debug("create key:{}", key);
                String[] hostPort = splitHostPort(key);
                return new RocketChannelWrapper(true, false, bootstrap.connect(hostPort[0], Integer.parseInt(hostPort[1])));
            }

            @Override
            public PooledObject<RocketChannelWrapper> wrap(RocketChannelWrapper value) {
                return new DefaultPooledObject<>(value);
            }

            @Override
            public void destroyObject(String key, PooledObject<RocketChannelWrapper> p) throws Exception {
                log.debug("destroyObject key:{} channel:{}", key, getSocketName(p.getObject().getConnectFuture().channel()));
                // 纯异步则不能阻塞，close也就不能调用syncUninterruptibly()
                // 所以执行完destroyObject后，可能channel的isActive仍为true
                p.getObject().setDestroyed(true);
                p.getObject().getConnectFuture().channel().close();
            }

            @Override
            public boolean validateObject(String key, PooledObject<RocketChannelWrapper> p) {
                // 当设置了testOnBorrow为true的话，会对makeObject新建的对象调用validateObject，如果false则会抛出异常
                // 所以当isFirstUsed时(刚刚创建的连接)，validateObject直接返回true
                log.debug("validateObject key:{} firstUsed:{} destroyed:{} channel:{}",
                        key, p.getObject().isFirstUsed(), p.getObject().isDestroyed(), getSocketName(p.getObject().getConnectFuture().channel()));
                if (p.getObject().isDestroyed()) {
                    return false;
                }
                return p.getObject().isFirstUsed() || p.getObject().getConnectFuture().channel().isActive();
            }
        }, poolConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public CompletableFuture<RocketResponse> execute(RocketRequest request, RocketDIYHandler diyHandler) {
        CompletableFuture<RocketResponse> result = new CompletableFuture<>();
        if (diyHandler != null) {
            result.whenCompleteAsync(new RocketDIYConsumer(diyHandler), eventLoopGroup);
        }
        try {
            if (isClosed.get()) {
                result.completeExceptionally(new IOException("RocketClient closed"));
                return result;
            }
            DefaultFullHttpRequest nettyHttpRequest = RocketRequest2NettyHttpRequest(request);
            String channelKey = joinHostPort(request.getHost(), request.getPort());
            RocketChannelWrapper channelWrapper = channelPool.borrowObject(channelKey);
            result.whenCompleteAsync(new RocketChannelConsumer(channelPool, channelWrapper, channelKey), eventLoopGroup);
            if (channelWrapper.isFirstUsed()) {
                channelWrapper.setFirstUsed(false);
            }
            Channel channel = channelWrapper.getConnectFuture().channel();
            channel.attr(FUTURE).set(result);
            channelWrapper.getConnectFuture().addListener(new RocketConnectListener(nettyHttpRequest));
            // 设置请求的整体超时时间(如果设置了阻塞，不包含等待连接的阻塞时间)，如果是新建的连接，则包含连接时间
            int requestTimeout = request.getTimeout() > 0 ? request.getTimeout() : config.getRequestTimeout();
            eventLoopGroup.schedule(() -> result.completeExceptionally(new TimeoutException("total timeout")), requestTimeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    public CompletableFuture<RocketResponse> execute(RocketRequest request) {
        return execute(request, null);
    }

    private DefaultFullHttpRequest RocketRequest2NettyHttpRequest(RocketRequest rocketRequest) {
        ByteBuf body = Unpooled.EMPTY_BUFFER;
        if (rocketRequest.getBody() != null) {
            body = Unpooled.wrappedBuffer(rocketRequest.getBody());
        }
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(rocketRequest.getVersion(), rocketRequest.getMethod(), rocketRequest.getUri(), body);
        HttpHeaders headers = httpRequest.headers();
        if (rocketRequest.getHeaders() != null) {
            headers.set(rocketRequest.getHeaders());
        }
        if (!headers.contains(HttpHeaderNames.HOST)) {
            headers.set(HttpHeaderNames.HOST, rocketRequest.getHost());
        }
        if (!headers.contains(HttpHeaderNames.USER_AGENT)) {
            headers.set(HttpHeaderNames.USER_AGENT, "RocketClient");
        }
        if (!headers.contains(HttpHeaderNames.ACCEPT)) {
            headers.set(HttpHeaderNames.ACCEPT, "*/*");
        }
        return httpRequest;
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            log.info("RocketClient closing...");
            bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
            eventLoopGroup.shutdownGracefully();
            channelPool.close();
        }
    }
}
