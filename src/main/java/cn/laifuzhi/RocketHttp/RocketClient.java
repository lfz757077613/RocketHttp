package cn.laifuzhi.RocketHttp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public RocketClient() {
        bootstrap = new Bootstrap().group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                // 其他参数全部用netty默认
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 10s主动关闭channel
//                                .addLast(new RocketIdleHandler(0, 0, 10))
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(10 * 1024 * 1024))
                                .addLast(new HttpContentDecompressor())
                                .addLast(new ChunkedWriteHandler())
                                .addLast(new RocketHandler());
                    }
                });
    }

    public Promise<String> execute(String host, int port, String uri) throws InterruptedException, ExecutionException {
        if (isClosed.get()) {
            throw new RuntimeException("RocketClient already close");
        }
        DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri, Unpooled.EMPTY_BUFFER);
        HttpHeaders headers = httpRequest.headers();
        headers.set(HttpHeaderNames.HOST, host);
        headers.set(HttpHeaderNames.USER_AGENT, "RocketClient");
        headers.set(HttpHeaderNames.ACCEPT, "*/*");

        ChannelFuture connectFuture = bootstrap.connect(host, port);
        Promise<String> promise = connectFuture.channel().eventLoop().newPromise();
        connectFuture.addListener((ChannelFutureListener) channelFuture -> {
            channelFuture.channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener(promise));
        });
        // 执行writeAndFlush时，如果channel已经关闭，则ChannelFutureListener中的channel pipeline已经没有自定义handler了
        // 所以单纯用FIRE_EXCEPTION_ON_FAILURE没有办法处理promise，因为RocketHandler已经没了
        return promise;
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            log.info("RocketClient closing...");
            bootstrap.config().group().shutdownGracefully().syncUninterruptibly();
        }
    }
}
