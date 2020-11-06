package cn.laifuzhi.RocketHttp;


import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class RocketClientTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        RocketClient rocketClient = new RocketClient();
        System.out.println(rocketClient.execute("www.baidu.com", 80, "").get());
        rocketClient.close();
        TimeUnit.SECONDS.sleep(1000);
    }
}
