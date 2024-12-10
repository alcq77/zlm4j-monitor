package com.aizuda.monitor.callback;

import com.aizuda.monitor.metrics.SystemMetrics;
import com.aizuda.monitor.metrics.StreamMetrics;
import com.aizuda.monitor.metrics.NetworkMetrics;
import com.aizuda.monitor.metrics.PerformanceMetrics;
import com.aizuda.monitor.storage.MetricsExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 抽象监控回调类
 * 提供了基本的回调实现
 * 
 * @author Cursor
 * @since 1.0
 */
public abstract class AbstractMonitorCallback implements MonitorCallback {
    private static final Logger log = LoggerFactory.getLogger(AbstractMonitorCallback.class);
    
    /** 指标导出器 */
    protected MetricsExporter exporter;
    
    @Override
    public void onSystemMetrics(SystemMetrics metrics) {
        try {
            if (exporter != null) {
                exporter.exportSystem(metrics);
            }
            handleSystemMetrics(metrics);
        } catch (Exception e) {
            log.error("处理系统指标失败", e);
        }
    }
    
    @Override
    public void onStreamMetrics(StreamMetrics metrics) {
        try {
            if (exporter != null) {
                exporter.exportStream(metrics);
            }
            handleStreamMetrics(metrics);
        } catch (Exception e) {
            log.error("处理流媒体指标失败", e);
        }
    }
    
    @Override
    public void onNetworkMetrics(NetworkMetrics metrics) {
        try {
            if (exporter != null) {
                exporter.exportNetwork(metrics);
            }
            handleNetworkMetrics(metrics);
        } catch (Exception e) {
            log.error("处理网络指标失败", e);
        }
    }

    @Override
    public void onPerformanceMetrics(PerformanceMetrics metrics) {
        try {
            if (exporter != null) {
                exporter.exportPerformance(metrics);
            }
            handlePerformanceMetrics(metrics);
        } catch (Exception e) {
            log.error("处理性能指标失败", e);
        }
    }
    
    /**
     * 处理系统指标
     *
     * @param metrics 系统指标对象
     */
    protected abstract void handleSystemMetrics(SystemMetrics metrics);
    
    /**
     * 处理流媒体指标
     *
     * @param metrics 流媒体指标对象
     */
    protected abstract void handleStreamMetrics(StreamMetrics metrics);
    
    /**
     * 处理网络指标
     *
     * @param metrics 网络指标对象
     */
    protected abstract void handleNetworkMetrics(NetworkMetrics metrics);
    
    /**
     * 处理性能指标
     *
     * @param metrics 性能指标对象
     */
    protected abstract void handlePerformanceMetrics(PerformanceMetrics metrics);
    
    /**
     * 设置指标导出器
     *
     * @param exporter 导出器实例
     */
    public void setExporter(MetricsExporter exporter) {
        this.exporter = exporter;
    }
} 