package cn.laifuzhi.RocketHttp;


import java.util.concurrent.TimeUnit;

public class RocketClientTest {
    public static void main(String[] args) throws Exception {
        RocketClient rocketClient = new RocketClient();
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").length());
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").length());
//        TimeUnit.SECONDS.sleep(8);
        System.out.println(rocketClient.execute("qq.com", 80, "").length());
        System.out.println(rocketClient.execute("qq.com", 80, "").length());
        TimeUnit.SECONDS.sleep(22);
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").length());
        System.out.println(rocketClient.execute("qq.com", 80, "").length());

//        rocketClient.close();
        TimeUnit.SECONDS.sleep(1000);
    }
}
