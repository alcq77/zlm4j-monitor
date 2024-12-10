package com.aizuda.monitor.collector;

import com.aizuda.monitor.config.ConfigManager;
import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.Metrics;
import com.aizuda.monitor.metrics.enums.MetricsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象指标收集器基类
 * 实现了 MetricsCollector 接口的通用功能
 * 
 * @author Cursor
 * @since 1.0
 * @param <T> 指标类型
 */
public abstract class AbstractMetricsCollector<T extends Metrics> implements MetricsCollector<T>, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AbstractMetricsCollector.class);
    
    /** 监控配置 */
    protected final MonitorConfig config;
    
    /** 配置管理器 */
    private final ConfigManager configManager;
    
    /** 上次收集的指标 */
    protected T lastMetrics;
    
    /** 收集器状态 */
    private volatile boolean initialized = false;
    private volatile boolean running = false;
    
    protected AbstractMetricsCollector(MonitorConfig config) {
        this.config = config;
        this.configManager = ConfigManager.getInstance();
        // 注册配置变更监听
        this.configManager.addConfigChangeListener(this::handleConfigChange);
    }
    
    /**
     * 处理配置变更
     */
    private void handleConfigChange(MonitorConfig newConfig) {
        try {
            // 检查是否需要停止收集器
            if (!isCollectorEnabled(newConfig)) {
                if (running) {
                    stop();
                }
                return;
            }
            
            // 通知子类处理配置变更
            onConfigChange(newConfig);
            
        } catch (Exception e) {
            log.error("处理配置变更失败: {}", getName(), e);
        }
    }
    
    /**
     * 检查收集器是否启用
     */
    protected abstract boolean isCollectorEnabled(MonitorConfig config);
    
    /**
     * 子类处理配置变更
     */
    protected void onConfigChange(MonitorConfig newConfig) {
        // 子类可以重写此方法处理配置变更
    }
    
    /**
     * 获取当前生效的配置
     */
    protected MonitorConfig getConfig() {
        return configManager.getConfig();
    }
    
    /**
     * 获取收集器名称
     */
    public abstract String getName();
    
    /**
     * 启动收集器
     */
    public abstract void start() throws Exception;
    
    /**
     * 初始化收集器
     */
    @Override
    public void init(MonitorConfig config) throws Exception {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            doInit();
            initialized = true;
            log.info("收集器已初始化: {}", getName());
        }
    }
    
    /**
     * 销毁收集器
     */
    @Override
    public void destroy() throws Exception {
        if (!initialized) {
            return;
        }
        synchronized (this) {
            if (!initialized) {
                return;
            }
            try {
                if (running) {
                    doStop();
                    running = false;
                }
                doDestroy();
                initialized = false;
                log.info("收集器已销毁: {}", getName());
            } catch (Exception e) {
                log.error("销毁收集器失败: {}", getName(), e);
                throw e;
            }
        }
    }
    
    /**
     * 收集指标
     */
    @Override
    public T collect() throws Exception {
        checkState();
        T metrics = createMetrics();
        try {
            doCollect(metrics);
            lastMetrics = metrics;
            return metrics;
        } catch (Exception e) {
            log.error("收集指标失败: {}", getName(), e);
            throw e;
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getMetricsClass() {
        return (Class<T>) lastMetrics.getClass();
    }
    
    /**
     * 获取指标类型
     */
    @Override
    public abstract MetricsType getType();
    
    /**
     * 创建新的指标对象
     */
    protected abstract T createMetrics();
    
    /**
     * 执行指标收集
     */
    protected abstract void doCollect(T metrics) throws Exception;
    
    /**
     * 关闭收集器
     */
    @Override
    public void close() {
        try {
            // 移除配置变更监听
            configManager.removeConfigChangeListener(this::handleConfigChange);
            // 销毁收集器
            destroy();
        } catch (Exception e) {
            log.error("关闭收集器失败: {}", getName(), e);
        }
    }
    
    /**
     * 检查收集器状态
     */
    protected void checkState() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("收集器未初始化: " + getName());
        }
        if (!running) {
            throw new IllegalStateException("收集器未启动: " + getName());
        }
    }
    
    /**
     * 初始化收集器
     */
    protected abstract void doInit() throws Exception;
    
    /**
     * 启动收集器
     */
    protected abstract void doStart() throws Exception;
    
    /**
     * 停止收集器的具体实现
     */
    protected abstract void doStop() throws Exception;
    
    /**
     * 停止收集器
     */
    public void stop() throws Exception {
        if (!running) {
            return;
        }
        synchronized (this) {
            if (!running) {
                return;
            }
            doStop();
            running = false;
            log.info("收集器已停止: {}", getName());
        }
    }
    
    /**
     * 销毁收集器
     */
    protected abstract void doDestroy() throws Exception;
} 