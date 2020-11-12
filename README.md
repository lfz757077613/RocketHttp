# RocketClient
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
// 实际上execute方法返回的是CompletableFuture<RocketResponse>，如果不传入RocketDIYHandler可以自己处理CompletableFuture<RocketResponse>
```