# ZLM4J Monitor

ZLM4J Monitor 是 ZLMediaKit 的 Java 监控模块，提供系统资源、流媒体、网络状态等多维度监控能力。基于 ZLM4J 开发，支持配置热加载、指标自定义、数据导出等特性。

## 目录
1. [项目介绍](#项目介绍)
2. [功能特性](#功能特性)
3. [架构设计](#架构设计)
4. [调用流程](#调用流程)
5. [快速开始](#快速开始)
6. [技术实现](#技术实现)
7. [项目进度](#项目进度)
8. [最佳实践](#最佳实践)
9. [常见问题](#常见问题)
10. [参与贡献](#参与贡献)

## 项目介绍

ZLM4J Monitor 是一个专门为 ZLMediaKit 设计的监控模块，具有以下特点：
- 轻量级：低资源占用，对业务无侵入
- 可扩展：支持自定义指标和导出器
- 高性能：异步处理，对象池复用
- 易使用：配置简单，接入方便

## 功能特性

### 1. 多维度监控
- **系统监控**
  - CPU、内存、磁盘使用率
  - 进程状态
  - JVM运行状态
- **流媒体监控**
  - 在线流统计
  - 推拉流状态
  - 编解码信息
  - 流量统计
- **网络监控**
  - 连接数统计
  - 协议分布
  - 带宽使用
  - 延迟统计

### 2. 可扩展性
- SPI扩展机制
- 自定义指标收集
- 自定义数据导出
- 配置热加载

### 3. 高性能
- 对象池复用
- 异步处理
- 批量导出
- 资源隔离

## 架构设计

### 1. 系统架构
```
+------------------+     +-----------------+     +------------------+
|    ZLM Server    |     |   ZLM4J Core   |     |   ZLM Monitor   |
|                  +---->|                 +---->|                 |
| (Media Server)   |     | (API Wrapper)   |     | (Monitor Core)  |
+------------------+     +-----------------+     +--------+--------+
                                                         |
                                                         v
                              +-------------------------------------------------------------------------+
                              |                       Collectors                                       |
                              |  +---------------+  +---------------+  +---------------+  +---------+  |
                              |  |    System     |  |    Stream     |  |    Network    |  |   ...  |  |
                              |  |   Collector   |  |   Collector   |  |   Collector   |  |        |  |
                              |  +---------------+  +---------------+  +---------------+  +---------+  |
                              +-------------------------------------------------------------------------+
                                                         |
                                                         v
                              +-------------------------------------------------------------------------+
                              |                       Exporters                                       |
                              |  +---------------+  +---------------+  +---------------+  +---------+  |
                              |  |    Console    |  |  Prometheus   |  |   InfluxDB    |  |   ...  |  |
                              |  |   Exporter    |  |   Exporter    |  |   Exporter    |  |        |  |
                              |  +---------------+  +---------------+  +---------------+  +---------+  |
                              +-------------------------------------------------------------------------+
```

### 2. 核心组件
- **ZLMMonitor**: 监控核心，负责生命周期管理
- **Collectors**: 指标收集器，负责数据采集
- **Processors**: 数据处理器，负责指标转换
- **Exporters**: 数据导出器，负责数据输出

## 调用流程

### 1. 初始化流程
```
用户代码
   ↓
ZLMMonitor.builder()
   ↓
ConfigManager初始化
   - 加载配置文件
   - 验证配置
   - 启用热加载
   ↓
初始化组件
   - 创建线程池
   - 初始化收集器
   - 初始化导出器
```

### 2. 监控流程
```
定时调度线程                 工作线程池
     ↓                         ↓
触发收集任务              处理收集结果
     ↓                         ↓
Collectors收集数据    →    数据验证
     ↓                         ↓
提交到工作线程池     ←    指标转换
                              ↓
                         数据聚合
                              ↓
                         导出数据
```

## 快速开始

### 1. 添加依赖
```xml
<dependency>
    <groupId>com.aizuda</groupId>
    <artifactId>zlm4j-monitor</artifactId>
    <version>1.1.9</version>
</dependency>
```

### 2. 创建监控实例
```java
// 1. 创建 ZLM API 客户端
ZLMConfig config = new ZLMConfig()
    .setHost("localhost")
    .setPort(8080)
    .setSecret("035c73f7-bb6b-4889-a715-d9eb2d1925cc");
    
ZLMApi zlmApi = new ZLMApi(config);

// 2. 创建监控器
ZLMMonitor monitor = ZLMMonitor.builder(zlmApi)
    .withConfigFile("monitor.yml")  // 可选：使用配置文件
    .build();

// 3. 添加监控回调
monitor.setCallback(new MonitorCallback() {
    @Override
    public void onSystemMetrics(SystemMetrics metrics) {
        System.out.printf("CPU: %.2f%%, Memory: %.2f%%\n", 
            metrics.getCpuUsage(), metrics.getMemoryUsage());
    }
});

// 4. 启动监控
monitor.start();
```

### 3. 配置说明
```yaml
# monitor.yml
basic:
  sample-interval: 5000  # 采样间隔(毫秒)
  initial-delay: 0       # 初始延迟(毫秒)
  daemon: true          # 是否守护线程

metrics:
  system:
    enabled: true      # 系统指标
  stream:
    enabled: true      # 流媒体指标
  network:
    enabled: true      # 网络指标
```

## 技术实现

### 1. 核心技术栈
- **基础框架**: Java 8+
- **配置解析**: Jackson YAML/JSON
- **系统监控**: OSHI
- **日志框架**: SLF4J + Logback
- **并发处理**: JUC
- **扩展机制**: Java SPI

### 2. 关键特性实现

#### 配置热加载
```java
public class ConfigManager {
    private final FileWatcher fileWatcher;
    
    public void enableHotReload() {
        fileWatcher.startWatch();
        fileWatcher.addListener(event -> {
            if (event.kind() == ENTRY_MODIFY) {
                reloadConfig();
            }
        });
    }
}
```

#### 对象池复用
```java
public class ObjectPool<T> {
    private final ArrayBlockingQueue<T> pool;
    private final Supplier<T> factory;
    
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }
    
    public void release(T obj) {
        pool.offer(obj);
    }
}
## 项目进度

### 已完成功能
- ✅ 核心监控框架
  - 基础架构设计
  - 生命周期管理
  - 配置热加载
  - 线程池管理
  - 资源隔离

- ✅ 指标收集器
  - 系统指标收集器
  - 流媒体指标收集器
  - 网络指标收集器
  - 性能指标收集器

- ✅ 导出机制
  - SPI扩展机制
  - 默认日志导出器
  - JSON文件导出器
  - 控制台导出器

### 进行中
- 🚧 监控扩展
  - Prometheus导出器
  - InfluxDB导出器
  - 告警机制
  - 指标聚合

- 🚧 性能优化
  - 对象池机制
  - 批量处理
  - GC优化
  - 资源限流

### 规划中
- 📅 可观测性增强
  - 链路追踪
  - 日志完善
  - 监控大盘
  - 告警规则

## 最佳实践

### 1. 性能优化
- 合理设置采样间隔
  ```yaml
  basic:
    sample-interval: 5000  # 一般场景
    sample-interval: 1000  # 高精度场景
    sample-interval: 30000 # 资源受限场景
  ```

- 启用对象池
  ```java
  // 创建对象池
  ObjectPool<StreamMetrics> pool = new ObjectPool<>(
      () -> new StreamMetrics(),
      metrics -> metrics.reset(),
      Runtime.getRuntime().availableProcessors() * 2
  );
  ```

### 2. 错误处理
```java
monitor.setCallback(new MonitorCallback() {
    @Override
    public void onError(Throwable error) {
        log.error("监控异常", error);
    }
});

// 优雅关闭
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try {
        monitor.stop();
        monitor.close();
    } catch (Exception e) {
        log.error("关闭失败", e);
    }
}));
```

### 3. 配置建议
- 开发环境
  ```yaml
  basic:
    sample-interval: 1000
    daemon: false
  metrics:
    all-enabled: true
  exporter:
    names: [console, json]
  ```

- 生产环境
  ```yaml
  basic:
    sample-interval: 5000
    daemon: true
  metrics:
    system.enabled: true
    stream.enabled: true
    network.enabled: true
  exporter:
    names: [prometheus]
    batch-enabled: true
  ```

## 常见问题

### 1. 性能影响
Q: 监控会影响 ZLM 服务器性能吗？
A: 默认配置下影响很小：
- CPU: < 1%
- 内存: < 50MB
- 网络: 可忽略

### 2. 配置问题
Q: 配置文件修改后不生效？
A: 检查以下几点：
- 配置文件路径是否正确
- 文件是否有读取权限
- 配置格式是否正确
- 查看错误日志

### 3. 扩展开发
Q: 如何开发自定义导出器？
A: 实现以下步骤：
1. 实现 MetricsExporter 接口
2. 添加 @SPI 注解
3. 创建 SPI 配置文件
4. 在配置中启用

## 参与贡献

欢迎提交 Issue 和 PR：
- Bug 修复
- 功能改进
- 文档完善
- 性能优化

## 许可证

Apache License 2.0
