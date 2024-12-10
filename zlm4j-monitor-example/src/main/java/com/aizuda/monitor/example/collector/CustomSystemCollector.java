package com.aizuda.monitor.example.collector;

import com.aizuda.monitor.collector.AbstractMetricsCollector;
import com.aizuda.monitor.metrics.SystemMetrics;
import com.aizuda.monitor.annotation.SPI;

/**
 * 自定义系统指标收集器
 */
@SPI("custom-system")
public class CustomSystemCollector extends AbstractMetricsCollector<SystemMetrics> {

    @Override
    protected void doInit() throws Exception {
        // 初始化收集器
    }

    @Override
    protected SystemMetrics createMetrics() {
        return new SystemMetrics();
    }

    @Override
    protected void doCollect(SystemMetrics metrics) throws Exception {
        // 收集系统指标
        metrics.setCpuUsage(0.5f);
        metrics.setMemoryUsage(0.3f);
        metrics.setDiskUsage(0.4f);
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