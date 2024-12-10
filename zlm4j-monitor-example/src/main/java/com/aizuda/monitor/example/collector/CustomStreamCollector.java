package com.aizuda.monitor.example.collector;

import com.aizuda.monitor.collector.AbstractMetricsCollector;
import com.aizuda.monitor.metrics.StreamMetrics;
import com.aizuda.monitor.annotation.SPI;
import com.aizuda.monitor.config.MonitorConfig;

/**
 * 自定义流媒体指标收集器
 */
@SPI("custom-stream")
public class CustomStreamCollector extends AbstractMetricsCollector<StreamMetrics> {

    @Override
    protected void doInit() throws Exception {
        // 初始化收集器
    }

    @Override
    protected StreamMetrics createMetrics() {
        return new StreamMetrics();
    }

    @Override
    protected StreamMetrics doCollect(StreamMetrics metrics) throws Exception {
        metrics.setStreamCount(10);
        metrics.setBandwidthUsage(1024L);
        metrics.setClientCount(100);
        return metrics;
    }

    @Override
    public boolean isCollectorEnabled(MonitorConfig config) {
        return config.isStreamMetricsEnabled();
    }

    @Override
    protected void doStart() throws Exception {
        // 启动收集器
    }

    @Override
    protected void doStop() throws Exception {
        // 停止收集器
    }

    @Override
    protected void doDestroy() throws Exception {
        // 销毁收集器
    }
} 