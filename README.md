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

    public static void main(String[] args) throws InterruptedException {
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




平台列表展示:
![平台列表](https://github.com/cloudinsight/cloudinsight-java-sdk/blob/master/images/1.overview.png)

平台列表指标展示:
![平台列表指标展示](https://github.com/cloudinsight/cloudinsight-java-sdk/blob/master/images/2.overview%20metric.png)

仪表盘展示:
![仪表盘展示](https://github.com/cloudinsight/cloudinsight-java-sdk/blob/master/images/3.customer%20dashboard.png)

- [statsd 相关介绍](https://github.com/wyvernnot/introduction-to-statsd)
