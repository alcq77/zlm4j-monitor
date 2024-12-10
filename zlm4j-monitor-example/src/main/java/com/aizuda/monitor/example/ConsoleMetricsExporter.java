package com.aizuda.monitor.example;

import com.aizuda.monitor.metrics.*;
import com.aizuda.monitor.storage.MetricsExporter;
import com.aizuda.monitor.annotation.SPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 控制台指标导出器
 */
@SPI("console") 
public class ConsoleMetricsExporter implements MetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(ConsoleMetricsExporter.class);

    @Override
    public String getName() {
        return "console";
    }

    @Override
    public <T extends Metrics> void export(Class<T> type, T metrics) throws Exception {
        if (metrics == null) {
            return;
        }
        
        // 格式化输出到控制台
        System.out.printf("[%s] %s: %s%n", 
            type.getSimpleName(),
            System.currentTimeMillis(),
            metrics.toString()
        );
    }

    @Override
    public void init() throws Exception {
        log.info("初始化控制台指标导出器");
    }

    @Override
    public void destroy() throws Exception {
        log.info("销毁控制台指标导出器");
    }

    @Override
    public void start() throws Exception {
        log.info("启动控制台指标导出器");
    }

    @Override
    public void stop() throws Exception {
        log.info("停止控制台指标导出器");
    }

    @Override
    public void exportSystem(SystemMetrics metrics) throws Exception {
        export(SystemMetrics.class, metrics);
    }

    @Override
    public void exportStream(StreamMetrics metrics) throws Exception {
        export(StreamMetrics.class, metrics);
    }

    @Override
    public void exportNetwork(NetworkMetrics metrics) throws Exception {
        export(NetworkMetrics.class, metrics);
    }

    @Override
    public void exportPerformance(PerformanceMetrics metrics) throws Exception {
        export(PerformanceMetrics.class, metrics);
    }

    @Override
    public void close() throws Exception {
        log.info("关闭控制台指标导出器");
    }
} 