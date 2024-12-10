package com.aizuda.monitor.core;

import com.aizuda.monitor.callback.MonitorCallback;
import com.aizuda.monitor.collector.*;
import com.aizuda.monitor.config.ConfigManager;
import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.*;
import com.aizuda.monitor.storage.MetricsExporter;
import com.aizuda.monitor.storage.MetricsExporterLoader;
import com.aizuda.zlm4j.core.ZLMApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aizuda.monitor.util.ObjectPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ZLM监控核心类
 * 负责整个监控系统的生命周期管理
 * 
 * 调用链:
 * 1. 初始化链:
 *    ZLMMonitor构造函数
 *    ↓
 *    ConfigManager初始化
 *      - 加载配置文件(如果存在)
 *      - 使用默认配置(如果配置文件不存在或加载失败)
 *      - 启用配置热加载(如果使用配置文件)
 *    ↓
 *    初始化组件
 *      - 初始化线程池
 *      - 初始化收集器
 *      - 初始化导出器
 * 
 * 2. 启动链:
 *    start()
 *      - 启动所有收集器(collector.start)
 *      - 启动所有导出器(exporter.start)
 *      - 启动调度任务(scheduleCollectors)
 * 
 * 3. 运行链:
 *    scheduleCollectors() -> 定时执行
 *      - 收集指标(collector.collect)
 *      - 通知回调(notifyCallback)
 *      - 导出指标(exporter.export)
 * 
 * 4. 停止链:
 *    stop()
 *      - 停止调度任务(executor.shutdown)
 *      - 停止所有收集器(collector.close)
 *      - 停止所有导出器(exporter.stop)
 * 
 * 5. 关闭链:
 *    close()
 *      - 关闭执行器(executor.shutdown)
 *      - 关闭收集器(collector.close)
 *      - 关闭导出器(exporter.close)
 *      - 关闭配置管理器(configManager.close)
 * 
 * @author Cursor
 * @since 1.0
 */
public class ZLMMonitor implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ZLMMonitor.class);
    
    // 监控组件
    private final ZLMApi zlmApi;
    // 配置管理器
    private final ConfigManager configManager;
    // 收集器
    private final List<AbstractMetricsCollector<?>> collectors = new ArrayList<>();
    // 导出器
    private final List<MetricsExporter> exporters = new ArrayList<>();
    // 回调
    private MonitorCallback callback;
    // 
    private ScheduledExecutorService scheduleExecutor;
    // 工作线程池
    private ThreadPoolExecutor workExecutor;
    // 运行状态
    private volatile boolean running = false;
    // 线程池监控指标
    private ThreadPoolMetrics threadPoolMetrics;
    // 配置
    private final MonitorConfig config;
    
    // 对象池
    private final ObjectPool<StreamMetrics> metricsPool = new ObjectPool<>(
        () -> new StreamMetrics(),
        metrics -> metrics.reset(),
        Runtime.getRuntime().availableProcessors() * 2
    );
    
    public static MonitorBuilder builder(ZLMApi zlmApi) {
        return new MonitorBuilder(zlmApi);
    }
    
    ZLMMonitor(ZLMApi zlmApi, ConfigManager configManager, MonitorConfig config) {
        this.zlmApi = zlmApi;
        this.configManager = configManager;
        this.config = config;
        
        // 注册配置变更监听
        this.configManager.addConfigChangeListener(this::handleConfigChange);
        
        // 初始化组件
        initComponents();
    }
    
    private void initComponents() {
        try {
            // 1. 初始化线程池
            initThreadPool(config);
            
            // 2. 初始化收集器
            initCollectors(config);
            
            // 3. 初始化导出器
            initExporters(config);
            
            log.info("ZLM监控初始化完成");
        } catch (Exception e) {
            log.error("ZLM监控初始化失败", e);
            throw new RuntimeException("初始化失败", e);
        }
    }
    
    /**
     * 初始化线程池
     */
    private void initThreadPool(MonitorConfig config) {
        // 1. 创建工作线程池
        workExecutor = createWorkThreadPool(config.getThread());
        
        // 2. 创建调度线程池
        scheduleExecutor = createScheduleThreadPool(config);
        
        // 3. 初始化线程池监控
        threadPoolMetrics = new ThreadPoolMetrics(workExecutor);
        
        log.info("线程池初始化完成: workPool[core={}, max={}, queueSize={}], schedulePool[size=1]", 
            config.getThread().getCorePoolSize(),
            config.getThread().getMaxPoolSize(),
            config.getThread().getQueueCapacity());
    }
    
    /**
     * 创建工作程池
     */
    private ThreadPoolExecutor createWorkThreadPool(MonitorConfig.ThreadConfig config) {
        // 1. 自定义线程工厂
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ZLMMonitor-Worker-" + threadNumber.getAndIncrement());
                t.setDaemon(configManager.getConfig().isDaemon());
                // 设置为较低优，避免影响主要业务
                t.setPriority(Thread.NORM_PRIORITY - 1);
                return t;
            }
        };
        
        // 2. 自定义拒绝策略
        RejectedExecutionHandler rejectedHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    // 记录任务被拒绝
                    threadPoolMetrics.taskRejected();
                    
                    // 尝试将任务放入队列
                    if (!executor.getQueue().offer(r, 100, TimeUnit.MILLISECONDS)) {
                        log.warn("工作线程队列已满，丢弃任务");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        
        // 3. 创建增强的线程池
        return new ThreadPoolExecutor(
            config.getCorePoolSize(),
            config.getMaxPoolSize(),
            config.getKeepAliveTime(),
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(config.getQueueCapacity()),
            threadFactory,
            rejectedHandler
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                try {
                    super.beforeExecute(t, r);
                    threadPoolMetrics.beforeExecute();
                } catch (Exception e) {
                    log.error("执行beforeExecute钩子方法失败", e);
                }
            }
            
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                try {
                    threadPoolMetrics.afterExecute(t);
                    super.afterExecute(r, t);
                } catch (Exception e) {
                    log.error("执行afterExecute钩子方法失败", e);
                } finally {
                    // 确清理ThreadLocal
                    threadPoolMetrics.cleanupThreadLocal();
                }
            }
            
            @Override
            protected void terminated() {
                try {
                    threadPoolMetrics.cleanup();
                    super.terminated();
                } catch (Exception e) {
                    log.error("执行terminated钩子方法失败", e);
                }
            }
        };
    }
    
    /**
     * 创建调度线程池
     */
    private ScheduledExecutorService createScheduleThreadPool(MonitorConfig config) {
        return Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "ZLMMonitor-Scheduler");
            t.setDaemon(config.isDaemon());
            // 设置为较高优先级，确保调度准确性
            t.setPriority(Thread.NORM_PRIORITY + 1);
            return t;
        });
    }
    
    /**
     * 线程池监控指标
     */
    private static class ThreadPoolMetrics {
        private final ThreadPoolExecutor executor;
        private final AtomicLong totalTasks = new AtomicLong();
        private final AtomicLong activeTasks = new AtomicLong();
        private final AtomicLong completedTasks = new AtomicLong();
        private final AtomicLong rejectedTasks = new AtomicLong();
        private final AtomicLong failedTasks = new AtomicLong();
        private final AtomicLong totalExecutionTime = new AtomicLong();
        private final ThreadLocal<Long> startTime = new ThreadLocal<>();
        
        public ThreadPoolMetrics(ThreadPoolExecutor executor) {
            this.executor = executor;
        }
        
        public void beforeExecute() {
            totalTasks.incrementAndGet();
            activeTasks.incrementAndGet();
            startTime.set(System.nanoTime());
        }
        
        public void afterExecute(Throwable t) {
            activeTasks.decrementAndGet();
            completedTasks.incrementAndGet();
            
            // 处理执行异常
            if (t != null) {
                failedTasks.incrementAndGet();
            }
            
            // 计算执行时间
            Long start = startTime.get();
            if (start != null) {
                totalExecutionTime.addAndGet(System.nanoTime() - start);
            }
        }
        
        public void taskRejected() {
            rejectedTasks.incrementAndGet();
        }
        
        public void cleanupThreadLocal() {
            startTime.remove();
        }
        
        public void cleanup() {
            // 清理所有资源
            startTime.remove();
        }
        
        public Map<String, Number> getMetrics() {
            Map<String, Number> metrics = new HashMap<>();
            // 线程池状态
            metrics.put("poolSize", executor.getPoolSize());
            metrics.put("activeThreads", executor.getActiveCount());
            metrics.put("corePoolSize", executor.getCorePoolSize());
            metrics.put("maximumPoolSize", executor.getMaximumPoolSize());
            metrics.put("queueSize", executor.getQueue().size());
            metrics.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
            
            // 任务统计
            metrics.put("totalTasks", totalTasks.get());
            metrics.put("activeTasks", activeTasks.get());
            metrics.put("completedTasks", completedTasks.get());
            metrics.put("rejectedTasks", rejectedTasks.get());
            metrics.put("failedTasks", failedTasks.get());
            
            // 性能指标
            long completed = completedTasks.get();
            if (completed > 0) {
                metrics.put("avgExecutionTime", totalExecutionTime.get() / completed);
                metrics.put("taskSuccessRate", 
                    (completed - failedTasks.get()) * 100.0 / completed);
            }
            
            // 线程池饱和度
            int maximumPoolSize = executor.getMaximumPoolSize();
            int queueCapacity = executor.getQueue().size() + 
                              executor.getQueue().remainingCapacity();
            metrics.put("poolUsage", 
                executor.getPoolSize() * 100.0 / maximumPoolSize);
            metrics.put("queueUsage", 
                executor.getQueue().size() * 100.0 / queueCapacity);
            
            return metrics;
        }
    }
    
    /**
     * 初始化收集器
     */
    private void initCollectors(MonitorConfig config) {
        // 添加系统指标收集器
        if (config.isSystemMetricsEnabled()) {
            collectors.add(new SystemMetricsCollector(zlmApi, config));
        }
        
        // 添加流媒体指标收集器
        if (config.isStreamMetricsEnabled()) {
            collectors.add(new StreamMetricsCollector(zlmApi, config));
        }
        
        // 添加网络指收集器
        if (config.isNetworkMetricsEnabled()) {
            collectors.add(new NetworkMetricsCollector(zlmApi, config));
        }
        
        // 添加性能指标收集器
        if (config.isPerformanceMetricsEnabled()) {
            collectors.add(new PerformanceMetricsCollector(zlmApi, config));
        }
        
        // 始化所有收集器
        for (AbstractMetricsCollector<?> collector : collectors) {
            try {
                collector.init(config);
            } catch (Exception e) {
                log.error("初始化收集器失败: {}", collector.getName(), e);
            }
        }
    }
    
    /**
     * 初始化导出器
     */
    private void initExporters(MonitorConfig config) {
        // 加载配置的导出器
        for (String name : config.getExporterNames()) {
            try {
                MetricsExporter exporter = MetricsExporterLoader.getExporter(name);
                exporters.add(exporter);
            } catch (Exception e) {
                log.error("加载导出器失败: {}", name, e);
            }
        }
        
        // 如果没有加载到任何导出器，使用默导出器
        if (exporters.isEmpty()) {
            log.warn("未找到任何导出器，用默认导出器");
            try {
                exporters.add(MetricsExporterLoader.getDefaultExporter());
            } catch (Exception e) {
                log.error("加载默认导出器失败", e);
            }
        }
        
        // 初始化所有导出器
        for (MetricsExporter exporter : exporters) {
            try {
                exporter.init();
            } catch (Exception e) {
                log.error("初始化导出器失败: {}", exporter.getName(), e);
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        try {
            stop();
        } finally {
            // 关闭执行器
            if (scheduleExecutor != null) {
                scheduleExecutor.shutdown();
                try {
                    if (!scheduleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduleExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    scheduleExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // 关闭收集器
            for (AbstractMetricsCollector<?> collector : collectors) {
                try {
                    collector.close();
                } catch (Exception e) {
                    log.error("关闭集器失败: {}", collector.getName(), e);
                }
            }
            
            // 关闭导出器
            for (MetricsExporter exporter : exporters) {
                try {
                    exporter.close();
                } catch (Exception e) {
                    log.error("关闭导出器失败: {}", exporter.getName(), e);
                }
            }
            
            // 关闭配置管理器
            if (configManager != null) {
                try {
                    configManager.close();
                } catch (Exception e) {
                    log.error("关闭配置管理器失败", e);
                    throw e;
                }
            }
        }
    }
    
    /**
     * 启动监控
     */
    public void start() {
        if (running) {
            return;
        }
        synchronized (this) {
            if (running) {
                return;
            }
            
            // 启动所有收集器
            for (AbstractMetricsCollector<?> collector : collectors) {
                try {
                    collector.start();
                } catch (Exception e) {
                    log.error("启动收集器失败: {}", collector.getName(), e);
                }
            }
            
            // 启动所有导出器
            for (MetricsExporter exporter : exporters) {
                try {
                    exporter.start();
                } catch (Exception e) {
                    log.error("启动导出器失败: {}", exporter.getName(), e);
                }
            }
            
            // 启动调度任务
            scheduleCollectors();
            
            running = true;
            log.info("ZLM监控已启动");
        }
    }
    
    /**
     * 调度收集器
     */
    private void scheduleCollectors() {
        MonitorConfig config = configManager.getConfig();
        
        // 1. 调度指标收集任务
        for (AbstractMetricsCollector<?> collector : collectors) {
            scheduleExecutor.scheduleWithFixedDelay(
                () -> {
                    try {
                        // 收集标
                        Object metrics = collector.collect();
                        
                        // 使用工作线程池处理指标导出
                        workExecutor.execute(() -> {
                            try {
                                // 通知回调
                                notifyCallback(metrics);
                                
                                // 导出指标
                                exportMetrics(metrics);
                            } catch (Exception e) {
                                log.error("处理指标败: {}", collector.getName(), e);
                            }
                        });
                    } catch (Exception e) {
                        log.error("收指标失败: {}", collector.getName(), e);
                    }
                },
                config.getInitialDelay(),
                config.getSampleInterval(),
                TimeUnit.MILLISECONDS
            );
        }
        
        // 2. 调度线程池指标收集任务
        scheduleExecutor.scheduleWithFixedDelay(
            () -> {
                try {
                    // 收集线程池指标
                    Map<String, Number> poolMetrics = threadPoolMetrics.getMetrics();
                    
                    // 使用工作线程池处理指标导出
                    workExecutor.execute(() -> {
                        try {
                            // 导出线程池指标
                            for (MetricsExporter exporter : exporters) {
                                try {
                                    // 创建性能指标对象并设置线程池指标
                                    PerformanceMetrics metrics = new PerformanceMetrics();
                                    metrics.setThreadPoolMetrics(poolMetrics);
                                    exporter.exportPerformance(metrics);
                                } catch (Exception e) {
                                    log.error("导出线程池指标失败: {}", exporter.getName(), e);
                                }
                            }
                            
                            // 记录关键指标到日志
                            if (log.isDebugEnabled()) {
                                log.debug("线程池状态: 活跃线程={}, 队列大小={}, 完成任务={}, 拒绝任务={}, 失败任务={}, 池使用率={}%, 队列使用率={}%",
                                    poolMetrics.get("activeThreads"),
                                    poolMetrics.get("queueSize"),
                                    poolMetrics.get("completedTasks"),
                                    poolMetrics.get("rejectedTasks"),
                                    poolMetrics.get("failedTasks"),
                                    poolMetrics.get("poolUsage"),
                                    poolMetrics.get("queueUsage")
                                );
                            }
                        } catch (Exception e) {
                            log.error("处理线程池指标失败", e);
                        }
                    });
                } catch (Exception e) {
                    log.error("收集线程池指标失败", e);
                }
            },
            config.getInitialDelay(),
            config.getSampleInterval(),
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * 导出指标
     */
    private void exportMetrics(Object metrics) {
        for (MetricsExporter exporter : exporters) {
            try {
                if (metrics instanceof SystemMetrics) {
                    exporter.exportSystem((SystemMetrics) metrics);
                } else if (metrics instanceof StreamMetrics) {
                    exporter.exportStream((StreamMetrics) metrics);
                } else if (metrics instanceof NetworkMetrics) {
                    exporter.exportNetwork((NetworkMetrics) metrics);
                } else if (metrics instanceof PerformanceMetrics) {
                    exporter.exportPerformance((PerformanceMetrics) metrics);
                }
            } catch (Exception e) {
                log.error("导出指标失败: {}", exporter.getName(), e);
            }
        }
    }
    
    /**
     * 设置监控回调
     */
    public void setCallback(MonitorCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 通知回调
     */
    private void notifyCallback(Object metrics) {
        if (callback != null) {
            try {
                if (metrics instanceof SystemMetrics) {
                    callback.onSystemMetrics((SystemMetrics) metrics);
                } else if (metrics instanceof StreamMetrics) {
                    callback.onStreamMetrics((StreamMetrics) metrics);
                } else if (metrics instanceof NetworkMetrics) {
                    callback.onNetworkMetrics((NetworkMetrics) metrics);
                } else if (metrics instanceof PerformanceMetrics) {
                    callback.onPerformanceMetrics((PerformanceMetrics) metrics);
                }
            } catch (Exception e) {
                log.error("回处理失败", e);
            }
        }
    }
    
    /**
     * 停止监控
     */
    public void stop() throws Exception {
        if (!running) {
            return;
        }
        synchronized (this) {
            if (!running) {
                return;
            }
            
            // 停止调度任务
            scheduleExecutor.shutdown();
            
            // 停止工作线程池
            workExecutor.shutdown();
            
            // 等待任务完成
            try {
                if (!scheduleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduleExecutor.shutdownNow();
                }
                if (!workExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    workExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduleExecutor.shutdownNow();
                workExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            running = false;
            log.info("ZLM监控已停止");
        }
    }
    
    /**
     * 处理配置变更
     */
    private void handleConfigChange(MonitorConfig newConfig) {
        try {
            synchronized (this) {
                // 1. 更新线程池配置
                updateThreadPoolConfig(newConfig);
                
                // 2. 更新收集器配置
                updateCollectorsConfig(newConfig);
                
                // 3. 更新导出器配置
                updateExportersConfig(newConfig);
                
                log.info("配置变更处理完成");
            }
        } catch (Exception e) {
            log.error("处理配置变更失败", e);
        }
    }
    
    private void updateThreadPoolConfig(MonitorConfig newConfig) {
        MonitorConfig.ThreadConfig threadConfig = newConfig.getThread();
        if (threadConfig != null) {
            // 更新工作线程池配置
            workExecutor.setCorePoolSize(threadConfig.getCorePoolSize());
            workExecutor.setMaximumPoolSize(threadConfig.getMaxPoolSize());
            workExecutor.setKeepAliveTime(threadConfig.getKeepAliveTime(), TimeUnit.SECONDS);
            
            log.info("线程池配置已更新: core={}, max={}, keepAlive={}s",
                threadConfig.getCorePoolSize(),
                threadConfig.getMaxPoolSize(),
                threadConfig.getKeepAliveTime());
        }
    }
    
    private void updateCollectorsConfig(MonitorConfig newConfig) {
        // 1. 停止已禁用收集器
        collectors.removeIf(collector -> {
            if (!isCollectorEnabled(collector, newConfig)) {
                try {
                    collector.stop();
                    log.info("收集器已停止: {}", collector.getName());
                    return true;
                } catch (Exception e) {
                    log.error("停止收集器失败: {}", collector.getName(), e);
                }
            }
            return false;
        });
        
        // 2. 启动新启用的收集器
        if (newConfig.isSystemMetricsEnabled() && !hasCollector(SystemMetricsCollector.class)) {
            addCollector(new SystemMetricsCollector(zlmApi, newConfig));
        }
        // ... 其他收集器类似处理
    }
    
    private void updateExportersConfig(MonitorConfig newConfig) {
        // 1. 停止已移的导出器
        List<String> enabledExporters = newConfig.getExporterNames();
        exporters.removeIf(exporter -> {
            if (!enabledExporters.contains(exporter.getName())) {
                try {
                    exporter.stop();
                    log.info("导出器已停止: {}", exporter.getName());
                    return true;
                } catch (Exception e) {
                    log.error("停止导出器失败: {}", exporter.getName(), e);
                }
            }
            return false;
        });
        
        // 2. 添加新的导出器
        for (String name : enabledExporters) {
            if (!hasExporter(name)) {
                try {
                    MetricsExporter exporter = MetricsExporterLoader.getExporter(name);
                    exporters.add(exporter);
                    exporter.start();
                    log.info("导出器已添加并启动: {}", name);
                } catch (Exception e) {
                    log.error("添加导出器失败: {}", name, e);
                }
            }
        }
    }
    
    private boolean isCollectorEnabled(AbstractMetricsCollector<?> collector, MonitorConfig config) {
        switch (collector.getType()) {
            case SYSTEM:
                return config.isSystemMetricsEnabled();
            case STREAM:
                return config.isStreamMetricsEnabled();
            case NETWORK:
                return config.isNetworkMetricsEnabled();
            case PERFORMANCE:
                return config.isPerformanceMetricsEnabled();
            default:
                return false;
        }
    }
    
    private boolean hasCollector(Class<?> collectorClass) {
        return collectors.stream().anyMatch(c -> c.getClass().equals(collectorClass));
    }
    
    private boolean hasExporter(String name) {
        return exporters.stream().anyMatch(e -> e.getName().equals(name));
    }
    
    private void addCollector(AbstractMetricsCollector<?> collector) {
        try {
            collector.init(config);
            collector.start();
            collectors.add(collector);
            log.info("收集器已添加并启动: {}", collector.getName());
        } catch (Exception e) {
            log.error("添加收集器失败: {}", collector.getName(), e);
        }
    }
    
    // 需要补充线程池关闭的优雅退出
    private void shutdownExecutors() {
        try {
            // 1. 先关闭调度线程池
            if (scheduleExecutor != null) {
                scheduleExecutor.shutdown();
                if (!scheduleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduleExecutor.shutdownNow();
                }
            }
            
            // 2. 再关闭工作线程池
            if (workExecutor != null) {
                workExecutor.shutdown();
                if (!workExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    workExecutor.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // 需要补充资源清理
    private void releaseResources() {
        try {
            // 1. 清理收集器资源
            for (AbstractMetricsCollector<?> collector : collectors) {
                try {
                    collector.close();
                } catch (Exception e) {
                    log.error("清理收集器资源失败: {}", collector.getName(), e);
                }
            }
            collectors.clear();
            
            // 2. 清理导出器资源
            for (MetricsExporter exporter : exporters) {
                try {
                    exporter.close();
                } catch (Exception e) {
                    log.error("清理导出器资源失败: {}", exporter.getName(), e);
                }
            }
            exporters.clear();
            
            // 3. 清理其他资源
            if (callback != null) {
                callback = null;
            }
        } catch (Exception e) {
            log.error("清理资源失败", e);
        }
    }
    
    // 批量处理指标
    private void batchProcessMetrics(List<Metrics> metricsBatch) {
        if (metricsBatch.isEmpty()) {
            return;
        }
        
        // 并行处理
        CompletableFuture.allOf(
            metricsBatch.stream()
                .map(metrics -> CompletableFuture.runAsync(
                    () -> processMetrics(metrics),
                    workExecutor
                ))
                .toArray(CompletableFuture[]::new)
        ).join();
    }
    
    /**
     * 处理单个指标
     */
    private void processMetrics(Metrics metrics) {
        try {
            // 通知回调
            notifyCallback(metrics);
            
            // 导出指标
            exportMetrics(metrics);
        } catch (Exception e) {
            log.error("处理指标失败", e);
        }
    }
} 