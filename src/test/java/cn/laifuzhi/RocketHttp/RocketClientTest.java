package cn.laifuzhi.RocketHttp;


import cn.laifuzhi.RocketHttp.model.RocketConfig;
import cn.laifuzhi.RocketHttp.model.RocketRequest;
import cn.laifuzhi.RocketHttp.model.RocketResponse;
import cn.laifuzhi.RocketHttp.reactive.RocketDIYHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient(RocketConfig.defaultConfig());
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(9, 9, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());
        AtomicInteger count = new AtomicInteger();
        for (; ; ) {
            TimeUnit.MILLISECONDS.sleep(100);
            threadPoolExecutor.execute(() -> {
                long start = System.currentTimeMillis();
                rocketClient.execute(RocketRequest.get("www.baidu.com", 80, ""), new RocketDIYHandler() {
                    @Override
                    public void onCompleted(RocketResponse response) throws Exception {
                        log.info("count:{} cast:{}", count.incrementAndGet(), System.currentTimeMillis() - start);
                        if (response.getBody().length < 280000) {
                            log.info("length:{}", response.getBody().length);
                        }
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        log.error("baidu", t);
                    }
                });
            });
        }
    }
}
