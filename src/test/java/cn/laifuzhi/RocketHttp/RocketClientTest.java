package cn.laifuzhi.RocketHttp;


import cn.laifuzhi.RocketHttp.model.RocketConfig;
import cn.laifuzhi.RocketHttp.model.RocketRequest;
import cn.laifuzhi.RocketHttp.model.RocketResponse;
import cn.laifuzhi.RocketHttp.reactive.RocketDIYHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient(RocketConfig.defaultConfig());
        AtomicInteger completeCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        AtomicInteger cancelCount = new AtomicInteger();
        for (int i = 0; i < 2000; i++) {
            rocketClient.execute(RocketRequest.get("httpbin.org", 80, "/get"), new RocketDIYHandler() {
                @Override
                public void onCompleted(RocketResponse response) throws Exception {
                    log.info("complete count:{}", completeCount.incrementAndGet());
                }

                @Override
                public void onThrowable(Throwable t) {
                    log.info("fail count:{}", failCount.incrementAndGet());
                }
            });
        }
    }
}
