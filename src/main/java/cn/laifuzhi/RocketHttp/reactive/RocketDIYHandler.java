package cn.laifuzhi.RocketHttp.reactive;

public interface RocketDIYHandler {
    void onCompleted(String response) throws Exception;

    void onThrowable(Throwable t);
}
