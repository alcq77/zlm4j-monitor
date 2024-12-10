package com.aizuda.monitor.storage;

import com.aizuda.monitor.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象指标导出器
 * 提供基础的导出器功能实现
 */
public abstract class AbstractMetricsExporter implements MetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(AbstractMetricsExporter.class);
    
    /** 是否已初始化 */
    private volatile boolean initialized = false;
    
    /** 是否已启动 */
    private volatile boolean started = false;
    
    @Override
    public <T extends Metrics> void export(Class<T> type, T metrics) throws Exception {
        checkState();
        if (metrics == null) {
            return;
        }
        
        try {
            doExport(type, metrics);
        } catch (Exception e) {
            log.error("导出指标失败: type={}, metrics={}", type.getSimpleName(), metrics, e);
            throw e;
        }
    }
    
    // ... 其他生命周期方法 ...
    
    /**
     * 检查导出器状态
     */
    protected void checkState() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("导出器未初始化: " + getName());
        }
        if (!started) {
            throw new IllegalStateException("导出器未启动: " + getName());
        }
    }
    
    /**
     * 执行通用导出逻辑
     */
    protected abstract <T extends Metrics> void doExport(Class<T> type, T metrics) throws Exception;
} 