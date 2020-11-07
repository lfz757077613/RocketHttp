package cn.laifuzhi.RocketHttp;


import java.util.concurrent.TimeUnit;

public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient();
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
        TimeUnit.SECONDS.sleep(4);
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());
        TimeUnit.SECONDS.sleep(4);
        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());
        TimeUnit.SECONDS.sleep(22);
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get().length());
        System.out.println(rocketClient.execute("qq.com", 80, "").get().length());

//        rocketClient.close();
        TimeUnit.SECONDS.sleep(1000);
    }
}
