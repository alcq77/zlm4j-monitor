# ZLM Monitor配置说明

## 配置加载顺序
1. 系统属性 (System Properties)
2. 环境变量 (Environment Variables)
3. 配置文件 (Configuration File)
4. 默认值 (Default Values)

## 配置项说明

### 基础配置
| 配置项 | 说明 | 类型 | 默认值 | 取值范围 | 单位 |
|-------|------|------|--------|----------|------|
| basic.sample-interval | 采样间隔 | long | 5000 | 1000-60000 | 毫秒 |
| basic.initial-delay | 初始延迟 | long | 0 | 0-60000 | 毫秒 |
| basic.daemon | 是否守护线程 | boolean | true | true/false | - |

### 线程池配置
| 配置项 | 说明 | 类型 | 默认值 | 取值范围 | 单位 |
|-------|------|------|--------|----------|------|
| thread.core-pool-size | 核心线程数 | int | 2 | 1-100 | - |
| thread.max-pool-size | 最大线程数 | int | 4 | core-size到100 | - |
| thread.queue-capacity | 队列容量 | int | 100 | 1-10000 | - |
| thread.keep-alive-time | 线程保活时间 | int | 60 | 1-3600 | 秒 |

### 指标配置
| 配置项 | 说明 | 类型 | 默认值 | 取值范围 | 单位 |
|-------|------|------|--------|----------|------|
| metrics.system.enabled | 系统指标开关 | boolean | true | true/false | - |
| metrics.stream.enabled | 流媒体指标开关 | boolean | true | true/false | - |
| metrics.network.enabled | 网络指标开关 | boolean | true | true/false | - |
| metrics.performance.enabled | 性能指标开关 | boolean | true | true/false | - |

### 导出器配置
| 配置项 | 说明 | 类型 | 默认值 | 取值范围 | 单位 |
|-------|------|------|--------|----------|------|
| exporter.names | 启用的导出器列表 | List | [default] | - | - |
| exporter.batch-enabled | 是否启用批量导出 | boolean | false | true/false | - |
| exporter.batch-size | 批量导出大小 | int | 100 | 1-1000 | - |
| exporter.batch-timeout | 批量导出超时 | int | 60 | 1-300 | 秒 |

## 配置示例

### 1. 基础配置
```yaml
basic:
  sample-interval: 5000  # 采样间隔(毫秒)
  initial-delay: 0       # 初始延迟(毫秒)
  daemon: true          # 是否守护线程
```

### 2. 生产环境配置
```yaml
basic:
  sample-interval: 10000  # 采样间隔调大
  daemon: true           # 使用守护线程

thread:
  core-pool-size: 4      # 根据CPU核心数调整
  max-pool-size: 8
  queue-capacity: 500    # 加大队列容量

metrics:
  system:
    enabled: true
  stream:
    enabled: true
    batch-size: 100     # 启用批处理
  network:
    enabled: true
```

### 3. 最小化配置
```yaml
basic:
  sample-interval: 30000  # 降低采样频率
  daemon: true

metrics:
  system:
    enabled: true
  stream:
    enabled: false      # 禁用不需要的指标
  network:
    enabled: false
  performance:
    enabled: false

exporter:
  names:
    - default          # 只使用默认导出器
```

## 配置最佳实践

### 1. 开发环境
- 使用较短的采样间隔(1-5秒)
- 启用所有指标
- 使用控制台导出器

### 2. 生产环境
- 使用较长的采样间隔(10-30秒)
- 只启用必要的指标
- 使用高性能导出器
- 启用批处理

### 3. 资源受限环境
- 使用最长的采样间隔(30秒以上)
- 最小化指标采集
- 使用最小化配置
 