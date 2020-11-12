# [RocketClient](https://github.com/lfz757077613/RocketHttp)
> 工作中一直使用[AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)作为http调用的client，之前经过压测比apache的[HttpAsyncClient](https://hc.apache.org/httpcomponents-asyncclient-dev/index.html)性能好很多

> 最近想学习下[AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)里对netty的使用，但是源码写的太复杂看不懂。所以决定自己撸一个类似的异步http client
- 基于netty实现了基础的httpClient，基于common-pool池化连接，分别实现http的同步和异步调用
- 本工程的重点在于连接的管理和异步响应式编程，对http协议的支持没有太多开发
- 代码简洁清晰，方便二次开发
### 分支说明
> asyncPool分支是开发最完善的分支，其他三个分支可以当做学习资料
- sync：最基础的同步http调用，每次请求都新建连接，阻塞等待连接完成后发出http请求，阻塞等待http响应
- async：最基础的异步http调用，每次请求都新建连接，连接完成后回调发出http请求，不阻塞等待http响应，直接返回包含响应的promise，http响应后回调写入promise
- syncPool：使用连接池同步http调用，每次从池中获取连接。如果没有可用连接则新建连接，阻塞等待连接完成后发出http请求，阻塞等待http响应
- asyncPool：使用连接池异步http调用，每次从池中获取连接。如果没有可用连接则新建连接，连接完成后回调发出http请求，不阻塞等待http响应，直接返回CompletableFuture，http响应后回调写入CompletableFuture
### 使用示例
```
// 异步调用
RocketClient rocketClient = new RocketClient(RocketConfig.defaultConfig());
long start = System.currentTimeMillis();
rocketClient.execute(RocketRequest.get("www.qq.com", 80, ""), new RocketDIYHandler() {
    @Override
    public void onCompleted(RocketResponse response) throws Exception {
        log.info("resp:{} cast:{}", new String(response.getBody(), CharsetUtil.UTF_8), System.currentTimeMillis() - start);
    }

    @Override
    public void onThrowable(Throwable t) {
        log.error("error", t);
    }
});
// 同步调用
RocketResponse rocketResponse = rocketClient.execute(RocketRequest.get("www.baidu.com", 80, "")).get();
// 实际上execute方法返回的是CompletableFuture<RocketResponse>，如果不传入RocketDIYHandler可以自己处理future
CompletableFuture<RocketResponse> future = rocketClient.execute(RocketRequest.get("www.qq.com", 80, ""))
```
### 性能测试，分别同[HttpAsyncClient](https://hc.apache.org/httpcomponents-asyncclient-dev/index.html)和[AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)测试
> 分别使用100个连接对同一个地址请求2000次，对比整体耗时，如果觉得测试方法不科学，跪求请一定要告诉我
###### 同HttpAsyncClient对比
> 测试用例分别如下，HttpAsyncClient在获取不到连接时会阻塞等待，所以RocketClient也需要设置成获取不到连接时阻塞等待
```
// HttpAsyncClient测试如下
AtomicInteger completeCount = new AtomicInteger();
AtomicInteger failCount = new AtomicInteger();
AtomicInteger cancelCount = new AtomicInteger();
HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create();
builder.setMaxConnPerRoute(100);
CloseableHttpAsyncClient httpclient = builder.build();
httpclient.start();
for (int i = 0; i < 2000; i++) {
    HttpGet request = new HttpGet("http://httpbin.org/get");
    httpclient.execute(request, new FutureCallback<HttpResponse>() {
        @Override
        public void completed(HttpResponse httpResponse) {
            if (completeCount.get() >= 0) {
                log.info("complete count:{}", completeCount.incrementAndGet());
            }
        }

        @Override
        public void failed(Exception e) {
            log.info("fail count:{}", failCount.incrementAndGet());
        }

        @Override
        public void cancelled() {
            log.info("cancel count:{}", cancelCount.incrementAndGet());
        }
    });
}
// RocketClient测试如下
RocketConfig rocketConfig = RocketConfig.defaultConfig();
rocketConfig.setBlockWhenExhausted(true);
RocketClient rocketClient = new RocketClient(rocketConfig);
AtomicInteger completeCount = new AtomicInteger();
AtomicInteger failCount = new AtomicInteger();
AtomicInteger cancelCount = new AtomicInteger();
for (int i = 0; i < 2000; i++) {
    rocketClient.execute(RocketRequest.get("httpbin.org", 80, "/get"), new RocketDIYHandler() {
        @Override
        public void onCompleted(RocketResponse response) throws Exception {
            log.info("complete count:{}", completeCount.incrementAndGet());
        }

        @Override
        public void onThrowable(Throwable t) {
            log.info("fail count:{}", failCount.incrementAndGet());
        }
    });
}
```
###### 同AsyncHttpClient对比
> 测试用例分别如下，AsyncHttpClient在获取不到连接时不会阻塞，直接抛出异常，所以RocketClient也需要设置成不阻塞等待连接。并且要使用线程池并发阻塞调用
```
// AsyncHttpClient测试如下
AtomicInteger completeCount = new AtomicInteger();
AtomicInteger failCount = new AtomicInteger();
AtomicInteger cancelCount = new AtomicInteger();
ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000));
AsyncHttpClientConfig ahcConfig = new DefaultAsyncHttpClientConfig.Builder()
        .setMaxConnectionsPerHost(100)
        .build();
DefaultAsyncHttpClient asyncHttpClient = new DefaultAsyncHttpClient(ahcConfig);
for (int i = 0; i < 2000; i++) {
    poolExecutor.execute(()->{
        try {
            asyncHttpClient.executeRequest(Dsl.get("http://httpbin.org/get").setRequestTimeout(60000), new AsyncCompletionHandlerBase() {
                @Override
                public Response onCompleted(Response response) throws Exception {
                    log.info("complete count:{}", completeCount.incrementAndGet());
                    return response;
                }

                @Override
                public void onThrowable(Throwable t) {
                    System.err.println(t);
                }
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
// RocketClient测试如下
AtomicInteger completeCount = new AtomicInteger();
AtomicInteger failCount = new AtomicInteger();
AtomicInteger cancelCount = new AtomicInteger();
ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2000));
RocketClient rocketClient = new RocketClient(RocketConfig.defaultConfig());
for (int i = 0; i < 2000; i++) {
    poolExecutor.execute(()->{
        try {
            rocketClient.execute(RocketRequest.get("httpbin.org", 80, "/get"), new RocketDIYHandler() {
                @Override
                public void onCompleted(RocketResponse response) throws Exception {
                    log.info("complete count:{}", completeCount.incrementAndGet());
                }

                @Override
                public void onThrowable(Throwable t) {
                    log.info("fail count:{}", failCount.incrementAndGet());
                }
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}
```
##### 因为和HttpAsyncClient、AsyncHttpClient对比方式不同，所以结果不能对比HttpAsyncClient和AsyncHttpClient之间的性能
##### 同HttpAsyncClient对比最终结果
- RocketClient 2000次请求：9s
- HttpAsyncClient 2000次请求：28s
##### 同AsyncHttpClient对比最终结果
> 底层都是netty实现，整体思路也类似，最终测试结果都是一样的...
- RocketClient 2000次请求：65s
- AsyncHttpClient 2000次请求：65s

### 依赖清单(仅额外依赖三个包)
```
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>

<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```
