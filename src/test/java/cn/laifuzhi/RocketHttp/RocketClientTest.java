package cn.laifuzhi.RocketHttp;


import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
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
                    Promise<String> promise1 = rocketClient.execute("www.qq.com", 80, "");
                    promise1.addListener((GenericFutureListener<Future<String>>) future -> {
                        if (future.isSuccess()) {
                            if (future.get().length() > 280000) {
                                System.out.println("qq" + future.get());
                            }
                            System.out.println(count.incrementAndGet());
                        } else {
                            log.error("baidu", future.cause());
                        }
                    });
                    Promise<String> promise2 = rocketClient.execute("www.baidu.com", 80, "");
                    promise2.addListener((GenericFutureListener<Future<String>>) future -> {
                        if (future.isSuccess()) {
                            if (future.get().length() < 280000) {
                                System.out.println("baidu" + future.get());
                            }
                            System.out.println(count.incrementAndGet());
                        }
                    });
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
