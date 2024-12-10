# ZLM4J Monitor 示例项目

## 项目介绍
本项目展示了如何使用 ZLM4J Monitor 监控模块,包含:
- 自定义指标收集器
- 自定义指标处理器
- 自定义指标导出器
- 自定义告警处理器
- 完整的配置示例
- 最佳实践建议

## 快速开始

1. 添加依赖
```xml
<dependency>
    <groupId>com.aizuda</groupId>
    <artifactId>zlm4j-monitor</artifactId>
    <version>${zlm4j.version}</version>
</dependency>
```

2. 基础使用
```java
// 创建ZLM API客户端
ZLMApi zlmApi = new ZLMApi()
    .setHost("localhost")
    .setPort(80)
    .setSecret("your-secret");

// 创建监控配置
MonitorConfig config = new MonitorConfig()
    .setSampleInterval(5000)
    .addExporter("console")
    .setSystemMetricsEnabled(true);

// 创建并启动监控器
ZLMMonitor monitor = new ZLMMonitor(zlmApi, config);
monitor.start();
```

## 功能示例

### 1. 自定义指标收集器
```java
@SPI("custom-system")
public class CustomSystemCollector extends AbstractMetricsCollector<SystemMetrics> {
    @Override
    protected SystemMetrics doCollect(SystemMetrics metrics) {
        // 实现指标收集逻辑
        return metrics;
    }
}
```

### 2. 自定义指标处理器
```java
@SPI("custom-processor")
public class CustomMetricsProcessor implements MetricsProcessor {
    @Override
    public void process(Class<? extends Metrics> type, Metrics metrics) {
        // 实现指标处理逻辑
    }
}
```

### 3. 自定义指标导出器
```java
@SPI("custom-exporter")
public class CustomMetricsExporter implements MetricsExporter {
    @Override
    public void export(Class<? extends Metrics> type, Metrics metrics) {
        // 实现指标导出逻辑
    }
}
```

## 配置说明

### 监控配置
```yaml
# monitor.yml
basic:
  sample-interval: 5000  # 采样间隔(毫秒)
  initial-delay: 0       # 初始延迟(毫秒)

metrics:
  system:
    enabled: true       # 启用系统指标
  stream:
    enabled: true       # 启用流媒体指标

exporter:
  names:               # 启用的导出器
    - console
    - json
```

### SPI配置
```
# META-INF/zlmmonitor/com.aizuda.monitor.collector.MetricsCollector
custom-system=com.aizuda.monitor.example.collector.CustomSystemCollector
custom-stream=com.aizuda.monitor.example.collector.CustomStreamCollector
```

## 最佳实践

1. 指标收集
- 合理设置采样间隔
- 只启用需要的指标
- 异常处理和重试机制

2. 指标处理
- 数据���滤和转换
- 聚合计算
- 缓存优化

3. 指标导出
- 批量导出
- 异步处理
- 失败重试

4. 告警处理
- 分级告警
- 告警抑制
- 通知渠道

## 常见问题

1. 采样间隔设置
- 建议: 5-30秒
- 原因: 过短影响性能,过长数据不及时

2. 内存占用
- 建议: 启用必要指标
- 原因: 指标数据占用内存

3. 性能优化
- 建议: 使用异步处理
- 原因: 避免阻塞主线程

## 参考文档

- [ZLM4J Monitor 文档](https://gitee.com/aizuda/zlm4j/tree/main/zlm4j-monitor)
- [ZLMediaKit 文档](https://github.com/ZLMediaKit/ZLMediaKit)