package cn.laifuzhi.RocketHttp;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(90, 90, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());

//        for (; ; ) {
//            TimeUnit.MILLISECONDS.sleep(1);
//            threadPoolExecutor.execute(() -> {
//                try {
//                    String qqResp = rocketClient.execute("www.qq.com", 80, "").get();
//                    if (qqResp.length() > 280000) {
//                        System.out.println(qqResp);
//                    }
//                    String baiduResp = rocketClient.execute("www.baidu.com", 80, "").get();
//                    if (baiduResp.length() < 280000) {
//                        System.out.println(baiduResp);
//                    }
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
