package cn.laifuzhi.RocketHttp.model;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class RocketRequest {
    private HttpVersion version = HttpVersion.HTTP_1_1;
    private HttpMethod method;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private String host;
    private int port;
    private String uri;
    private byte[] body;
    private int timeout = -1;

    public static RocketRequest get(String host, int port, String uri) {
        RocketRequest rocketRequest = new RocketRequest();
        rocketRequest.setMethod(HttpMethod.GET);
        rocketRequest.setHost(host);
        rocketRequest.setPort(port);
        rocketRequest.setUri(uri);
        return rocketRequest;
    }

    public static RocketRequest post(String host, int port, String uri, byte[] body) {
        RocketRequest rocketRequest = new RocketRequest();
        rocketRequest.setMethod(HttpMethod.POST);
        rocketRequest.setHost(host);
        rocketRequest.setPort(port);
        rocketRequest.setUri(uri);
        rocketRequest.setBody(body);
        return rocketRequest;
    }

}
