cloudinsight-java-sdk
================

Sdk for oneapm cloudinsight.

> [Cloud Insight](http://www.oneapm.com/ci/feature.html) (次世代系统监控工具):
集监控、管理、协作、计算、可视化、报警于一身，减少在系统监控上的人力和时间成本投入，让运维工作变得更加高效、简单。


###SDK使用步骤
  - 安装[Cloud Insight](http://www.oneapm.com/ci/feature.html)探针，见[文档](http://docs-ci.oneapm.com/quick-start/)。
  - 在 pom.xml 文件中添加依赖

```xml
<dependency>
    <groupId>com.oneapm.cloud</groupId>
    <artifactId>cloudinsight-sdk</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

  - 以一个小时中每隔一分钟向 cloudinsight 探针发送一个点为例．
  

``` java
package com.oneapm.tps.sdk;

import java.util.Random;

import cloudinsight.sdk.CloudInsightStatsDClient;

public class CloudInsightSDKTest {

    private static final Random RAND = new Random();

    public static void main2(String[] args) throws InterruptedException {
        CloudInsightStatsDClient client = new CloudInsightStatsDClient();

        String metric = "sdk.java.cloudinsight";
        String[] tags = new String[] {"key1:val1"};

        int count = 0;
        while (count <= 60) {
            client.gauge(metric, RAND.nextDouble(), tags);
            count += 1;
            Thread.sleep(1000 * 60);
        }
    }

}
```
- cloudinsight 效果图如下:
![平台列表](http://f.picphotos.baidu.com/album/s=1600;q=90/sign=f7fa8ccfdef9d72a1364141be41a1345/d788d43f8794a4c290a80fdb09f41bd5ac6e39b3.jpg)
![平台列表指标展示](http://c.picphotos.baidu.com/album/s=1600;q=90/sign=1e0aec6dccef7609380b9d991eed98bd/faedab64034f78f0e5c872037e310a55b2191c7a.jpg)
![仪表盘展示](http://h.picphotos.baidu.com/album/s=1600;q=90/sign=f29c58f5e2dde711e3d247f097dff56a/3812b31bb051f819a1e6ebbaddb44aed2f73e77b.jpg)

- [statsd 相关介绍](https://github.com/wyvernnot/introduction-to-statsd)


