package cn.laifuzhi.RocketHttp;


import cn.laifuzhi.RocketHttp.reactive.RocketDIYHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(9, 9, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());
        AtomicInteger count = new AtomicInteger();
        for (; ; ) {
            TimeUnit.MILLISECONDS.sleep(10);
            threadPoolExecutor.execute(() -> {
                try {
                    CompletableFuture<String> future1 = rocketClient.execute("www.qq.com", 80, "", new RocketDIYHandler() {
                        @Override
                        public void onCompleted(String response) throws Exception {
                            if (response.length() > 280000) {
                                System.out.println("qq" + response);
                            }
                            System.out.println(count.incrementAndGet());
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            log.error("qq", t);
                        }
                    });
//                    CompletableFuture<String> future2 = rocketClient.execute("www.baidu.com", 80, "");
//                    future2.whenComplete((response, throwable) -> {
//                        if (response != null && response.length() < 280000) {
//                            System.out.println("baidu" + response.length());
//                        }
//                        System.out.println(count.incrementAndGet());
//                        if (throwable != null) {
//                            log.error("baidu", throwable);
//                        }
//                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
//        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
////        TimeUnit.SECONDS.sleep(4);
//        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
//        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());
//        TimeUnit.SECONDS.sleep(4);
//        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());
//        TimeUnit.SECONDS.sleep(22);
//        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
//        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());
//
////        rocketClient.close();
//        TimeUnit.SECONDS.sleep(1000);
    }
}
