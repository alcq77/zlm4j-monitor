package com.aizuda.monitor.callback;

import com.aizuda.monitor.metrics.SystemMetrics;
import com.aizuda.monitor.metrics.StreamMetrics;
import com.aizuda.monitor.metrics.NetworkMetrics;
import com.aizuda.monitor.metrics.PerformanceMetrics;

/**
 * 监控回调接口
 * 用于接收监控指标数据
 * 
 * @author Cursor
 * @since 1.0
 */
public interface MonitorCallback {
    
    /**
     * 系统指标回调
     *
     * @param metrics 系统指标对象
     */
    void onSystemMetrics(SystemMetrics metrics);
    
    /**
     * 流媒体指标回调
     *
     * @param metrics 流媒体指标对象
     */
    void onStreamMetrics(StreamMetrics metrics);
    
    /**
     * 网络指标回调
     *
     * @param metrics 网络指标对象
     */
    void onNetworkMetrics(NetworkMetrics metrics);

    /**
     * 性能指标回调
     *
     * @param metrics 性能指标对象
     */
    void onPerformanceMetrics(PerformanceMetrics metrics);
} 