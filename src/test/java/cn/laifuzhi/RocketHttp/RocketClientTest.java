package cn.laifuzhi.RocketHttp;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 50, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());

//        for (; ; ) {
//            TimeUnit.MILLISECONDS.sleep(1);
//            threadPoolExecutor.execute(() -> {
//                try {
//                    System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            });
//        }
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
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
