package cn.laifuzhi.RocketHttp.model;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RocketResponse {
    private HttpVersion version;
    private int code;
    private HttpHeaders headers;
    private byte[] body;

}
