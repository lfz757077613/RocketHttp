package cn.laifuzhi.RocketHttp;


import cn.laifuzhi.RocketHttp.model.RocketConfig;
import cn.laifuzhi.RocketHttp.model.RocketRequest;
import cn.laifuzhi.RocketHttp.model.RocketResponse;
import cn.laifuzhi.RocketHttp.reactive.RocketDIYHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Slf4j
public class RocketClientTest {
    @Test
    public void test() throws Exception {
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(80, 80, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000));
        RocketClient rocketClient = new RocketClient(RocketConfig.defaultConfig());
        AtomicInteger completeCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        AtomicInteger cancelCount = new AtomicInteger();
        for (;;) {
            TimeUnit.MILLISECONDS.sleep(10);
            poolExecutor.execute(() -> {
                try {
                    long start = System.currentTimeMillis();
                    rocketClient.execute(RocketRequest.get("httpbin.org", 80, "/get"), new RocketDIYHandler() {
                        @Override
                        public void onCompleted(RocketResponse response) throws Exception {
                            log.info("complete count:{} cast:{}", completeCount.incrementAndGet(), System.currentTimeMillis() - start);
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            log.info("fail count:{}", failCount.incrementAndGet());
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
//        TimeUnit.HOURS.sleep(1);
    }
}
