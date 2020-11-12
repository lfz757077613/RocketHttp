package cn.laifuzhi.RocketHttp.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 所有和时间有关的单位都是毫秒
 */
@Getter
@Setter
public final class RocketConfig {
    private int connectTimeout = 10 * 1000;
    private int requestTimeout = 60 * 1000;
    private int httpMaxContent = 10 * 1024 * 1024;
    private int maxConnectPerHost = 100;
    private int evictIdleConnectPeriod = 60 * 1000;
    private int idleConnectKeepAliveTime = 60 * 1000;
    private RocketConfig(){}

    public static RocketConfig defaultConfig() {
        return new RocketConfig();
    }
}
