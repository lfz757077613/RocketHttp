package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketResponse;

public interface RocketDIYHandler {
    void onCompleted(RocketResponse response) throws Exception;

    void onThrowable(Throwable t);
}
