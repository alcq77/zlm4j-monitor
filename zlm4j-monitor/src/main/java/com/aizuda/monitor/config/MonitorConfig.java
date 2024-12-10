package com.aizuda.monitor.config;

import com.aizuda.monitor.metrics.enums.MetricsType;
import java.util.Arrays;
import java.util.List;

/**
 * 监控配置类
 * 提供监控模块的所有配置参数，支持默认配置和自定义配置
 * 
 * @author Cursor
 * @since 1.0
 */
public class MonitorConfig {
    /** 默认采样间隔(毫秒) */
    private static final long DEFAULT_SAMPLE_INTERVAL = 5000;
    /** 默认初始延迟(毫秒) */
    private static final long DEFAULT_INITIAL_DELAY = 0;
    /** 默认是否为守护线程 */
    private static final boolean DEFAULT_DAEMON = true;
    /** 默认核心线程数 */
    private static final int DEFAULT_CORE_POOL_SIZE = 2;
    /** 默认最大线程数 */
    private static final int DEFAULT_MAX_POOL_SIZE = 4;
    /** 默认队列容量 */
    private static final int DEFAULT_QUEUE_CAPACITY = 100;
    
    /** 采样间隔(毫秒) */
    private long sampleInterval = DEFAULT_SAMPLE_INTERVAL;
    /** 初始延迟(毫秒) */
    private long initialDelay = DEFAULT_INITIAL_DELAY;
    /** 是否为守护线程 */
    private boolean daemon = DEFAULT_DAEMON;
    
    /** 线池配置 */
    private ThreadConfig thread = new ThreadConfig();
    /** 导出器配置 */
    private ExporterConfig exporter = new ExporterConfig();
    /** 指标配置 */
    private MetricsConfig metrics = new MetricsConfig();
    
    /**
     * 默认构造函数
     * 使用默认配置初始化所有参数
     */
    public MonitorConfig() {
        initDefaultConfig();
    }
    
    /**
     * 初始化默认配置
     */
    public void initDefaultConfig() {
        // 基础配置默认值
        this.sampleInterval = DEFAULT_SAMPLE_INTERVAL;
        this.initialDelay = DEFAULT_INITIAL_DELAY;
        this.daemon = DEFAULT_DAEMON;
        
        // 线程池默认配置
        this.thread = new ThreadConfig();
        this.thread.setCorePoolSize(DEFAULT_CORE_POOL_SIZE);
        this.thread.setMaxPoolSize(DEFAULT_MAX_POOL_SIZE);
        this.thread.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);
        this.thread.setKeepAliveTime(60);
        this.thread.setAllowCoreTimeout(false);
        
        // 导出器默认配置
        this.exporter = new ExporterConfig();
        this.exporter.setNames(Arrays.asList("default"));
        this.exporter.setBatchEnabled(false);
        this.exporter.setBatchSize(100);
        this.exporter.setBatchTimeout(60);
        
        // 指标默认配置
        this.metrics = new MetricsConfig();
        this.metrics.setSystem(new MetricTypeConfig(true));
        this.metrics.setStream(new MetricTypeConfig(true));
        this.metrics.setNetwork(new MetricTypeConfig(true));
        this.metrics.setPerformance(new MetricTypeConfig(true));
    }
    
    /**
     * 验证配置
     * 检查所有配置参数是否有效
     *
     * @throws IllegalArgumentException 如果配置参数无效
     */
    public void validate() {
        // 验证采样间隔
        if (sampleInterval <= 0) {
            throw new IllegalArgumentException("采样间隔必须大于0");
        }
        
        // 验证程池参数
        if (thread.getCorePoolSize() <= 0) {
            throw new IllegalArgumentException("核心线程数必须大于0");
        }
        if (thread.getMaxPoolSize() < thread.getCorePoolSize()) {
            throw new IllegalArgumentException("最大线程数不能小于核心线程数");
        }
        if (thread.getQueueCapacity() <= 0) {
            throw new IllegalArgumentException("队列容量必须大于0");
        }
        
        // 验证导出器配置
        if (exporter.getNames() == null || exporter.getNames().isEmpty()) {
            throw new IllegalArgumentException("至少需要配置一个导出器");
        }
        
        // 验证指标配置 - 至少启用一种指标
        if (!metrics.getSystem().isEnabled() 
            && !metrics.getStream().isEnabled()
            && !metrics.getNetwork().isEnabled()
            && !metrics.getPerformance().isEnabled()) {
            throw new IllegalArgumentException("至少需要启用一种指标采集");
        }
        
        // 线程池验证
        if (thread.getKeepAliveTime() <= 0) {
            throw new IllegalArgumentException("keepAliveTime必须大于0");
        }
        
        // 批处理验证
        if (exporter.isBatchEnabled()) {
            if (exporter.getBatchSize() <= 0) {
                throw new IllegalArgumentException("batchSize必须大于0");
            }
            if (exporter.getBatchTimeout() <= 0) {
                throw new IllegalArgumentException("batchTimeout必须大于0");
            }
        }
    }

    // Getter and Setter methods
    public long getSampleInterval() {
        return sampleInterval;
    }

    public void setSampleInterval(long sampleInterval) {
        this.sampleInterval = sampleInterval;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public ThreadConfig getThread() {
        return thread;
    }

    public void setThread(ThreadConfig thread) {
        this.thread = thread;
    }

    public ExporterConfig getExporter() {
        return exporter;
    }

    public void setExporter(ExporterConfig exporter) {
        this.exporter = exporter;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }
    
    /**
     * 线程池配置类
     */
    public static class ThreadConfig {
        private int corePoolSize = DEFAULT_CORE_POOL_SIZE;
        private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
        private long keepAliveTime = 60;
        private boolean allowCoreTimeout = false;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public long getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }

        public boolean isAllowCoreTimeout() {
            return allowCoreTimeout;
        }

        public void setAllowCoreTimeout(boolean allowCoreTimeout) {
            this.allowCoreTimeout = allowCoreTimeout;
        }
    }
    
    /**
     * 导出器配置类
     */
    public static class ExporterConfig {
        private List<String> names;
        private boolean batchEnabled = false;
        private int batchSize = 100;
        private long batchTimeout = 60;

        public List<String> getNames() {
            return names;
        }

        public void setNames(List<String> names) {
            this.names = names;
        }

        public boolean isBatchEnabled() {
            return batchEnabled;
        }

        public void setBatchEnabled(boolean batchEnabled) {
            this.batchEnabled = batchEnabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getBatchTimeout() {
            return batchTimeout;
        }

        public void setBatchTimeout(long batchTimeout) {
            this.batchTimeout = batchTimeout;
        }
    }
    
    /**
     * 指标配置类
     */
    public static class MetricsConfig {
        private MetricTypeConfig system;
        private MetricTypeConfig stream;
        private MetricTypeConfig network;
        private MetricTypeConfig performance;

        public MetricTypeConfig getSystem() {
            return system;
        }

        public void setSystem(MetricTypeConfig system) {
            this.system = system;
        }

        public MetricTypeConfig getStream() {
            return stream;
        }

        public void setStream(MetricTypeConfig stream) {
            this.stream = stream;
        }

        public MetricTypeConfig getNetwork() {
            return network;
        }

        public void setNetwork(MetricTypeConfig network) {
            this.network = network;
        }

        public MetricTypeConfig getPerformance() {
            return performance;
        }

        public void setPerformance(MetricTypeConfig performance) {
            this.performance = performance;
        }
    }
    
    /**
     * 指标类型配置
     */
    public static class MetricTypeConfig {
        private boolean enabled;

        public MetricTypeConfig() {
        }

        public MetricTypeConfig(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    /**
     * 配置构建器
     * 提供流式API构建配置对象
     */
    public static class Builder {
        private final MonitorConfig config = new MonitorConfig();
        
        /**
         * 设置采样间隔
         *
         * @param interval 采样间隔(毫秒)
         * @return Builder实例
         */
        public Builder sampleInterval(long interval) {
            config.setSampleInterval(interval);
            return this;
        }
        
        /**
         * 设置初始延迟
         *
         * @param delay 初始延迟(毫秒)
         * @return Builder实例
         */
        public Builder initialDelay(long delay) {
            config.setInitialDelay(delay);
            return this;
        }
        
        /**
         * 设置是否为守护线程
         *
         * @param daemon 是否为守护线程
         * @return Builder实例
         */
        public Builder daemon(boolean daemon) {
            config.setDaemon(daemon);
            return this;
        }
        
        /**
         * 启用指定的指标类型
         *
         * @param types 要启用的指标类型
         * @return Builder实例
         */
        public Builder enableMetrics(MetricsType... types) {
            // 首先禁用所有指标
            config.getMetrics().getSystem().setEnabled(false);
            config.getMetrics().getStream().setEnabled(false);
            config.getMetrics().getNetwork().setEnabled(false);
            config.getMetrics().getPerformance().setEnabled(false);
            
            // 启用指定的指标
            for (MetricsType type : types) {
                switch (type) {
                    case SYSTEM:
                        config.getMetrics().getSystem().setEnabled(true);
                        break;
                    case STREAM:
                        config.getMetrics().getStream().setEnabled(true);
                        break;
                    case NETWORK:
                        config.getMetrics().getNetwork().setEnabled(true);
                        break;
                    case PERFORMANCE:
                        config.getMetrics().getPerformance().setEnabled(true);
                        break;
                }
            }
            return this;
        }
        
        /**
         * 设置导出器名称列表
         *
         * @param exporterNames 导出器名称列表
         * @return Builder实例
         */
        public Builder exporters(String... exporterNames) {
            config.getExporter().setNames(Arrays.asList(exporterNames));
            return this;
        }
        
        /**
         * 构建配置对象
         *
         * @return 配置对象
         * @throws IllegalArgumentException 如果配置参数无效
         */
        public MonitorConfig build() {
            config.validate();
            return config;
        }
    }

    /**
     * 获取系统指标是否启用
     */
    public boolean isSystemMetricsEnabled() {
        return metrics.getSystem().isEnabled();
    }

    public void setSystemMetricsEnabled(boolean systemMetricsEnabled) {
        metrics.getSystem().setEnabled(systemMetricsEnabled);
    }

    /**
     * 获取流媒体指标是否启用
     */
    public boolean isStreamMetricsEnabled() {
        return metrics.getStream().isEnabled();
    }

    public void setStreamMetricsEnabled(boolean streamMetricsEnabled) {
        metrics.getStream().setEnabled(streamMetricsEnabled);
    }

    /**
     * 获取网络指标是否启用
     */
    public boolean isNetworkMetricsEnabled() {
        return metrics.getNetwork().isEnabled();
    }

    public void setNetworkMetricsEnabled(boolean networkMetricsEnabled) {
        metrics.getNetwork().setEnabled(networkMetricsEnabled);
    }

    /**
     * 获取性能指标是否启用
     */
    public boolean isPerformanceMetricsEnabled() {
        return metrics.getPerformance().isEnabled();
    }

    public void setPerformanceMetricsEnabled(boolean performanceMetricsEnabled) {
        metrics.getPerformance().setEnabled(performanceMetricsEnabled);
    }

    /**
     * 获取已启用的导出器名称列表
     */
    public List<String> getExporterNames() {
        return exporter.getNames();
    }

    public void setExporterNames(List<String> exporterNames) {
        exporter.setNames(exporterNames);
    }

    /**
     * 获取指定类型的指标配置
     *
     * @param type 指标类型
     * @return 指标配置
     */
    public MetricTypeConfig getMetricConfig(MetricsType type) {
        if (metrics == null) {
            return new MetricTypeConfig(true);
        }
        
        switch (type) {
            case SYSTEM:
                return metrics.getSystem();
            case STREAM:
                return metrics.getStream();
            case NETWORK:
                return metrics.getNetwork();
            case PERFORMANCE:
                return metrics.getPerformance();
            default:
                throw new IllegalArgumentException("不支持的指标类型: " + type);
        }
    }
} 