package cn.laifuzhi.RocketHttp.reactive;

import cn.laifuzhi.RocketHttp.model.RocketResponse;
import lombok.AllArgsConstructor;

import java.util.function.BiConsumer;

@AllArgsConstructor
public final class RocketDIYConsumer implements BiConsumer<RocketResponse, Throwable> {
    private RocketDIYHandler diyHandler;

    @Override
    public void accept(RocketResponse resp, Throwable throwable) {
        try {
            if (resp != null) {
                diyHandler.onCompleted(resp);
            }
            if (throwable != null) {
                diyHandler.onThrowable(throwable);
            }
        } catch (Exception e) {
            diyHandler.onThrowable(e);
        }
    }
}
