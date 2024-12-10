package com.aizuda.monitor.storage;

import com.aizuda.monitor.annotation.SPI;
import com.aizuda.monitor.metrics.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SPI("default")
public class DefaultMetricsExporter extends AbstractMetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(DefaultMetricsExporter.class);

    @Override
    protected <T extends Metrics> void doExport(Class<T> type, T metrics) throws Exception {
        if (metrics instanceof SystemMetrics) {
            exportSystem((SystemMetrics) metrics);
        } else if (metrics instanceof NetworkMetrics) {
            exportNetwork((NetworkMetrics) metrics);
        } else if (metrics instanceof StreamMetrics) {
            exportStream((StreamMetrics) metrics);
        } else if (metrics instanceof PerformanceMetrics) {
            exportPerformance((PerformanceMetrics) metrics);
        } else {
            log.warn("未知的指标类型: {}", type.getName());
        }
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public void exportSystem(SystemMetrics metrics) throws Exception {
        // 标准化的log日志输出日志信息
        log.info("导出系统指标: {}", metrics);
    }

    @Override
    public void exportNetwork(NetworkMetrics metrics) throws Exception {
        log.info("导出网络指标: {}", metrics);
    }

    @Override
    public void exportStream(StreamMetrics metrics) throws Exception {
        log.info("导出流媒体指标: {}", metrics);
    }

    @Override
    public void exportPerformance(PerformanceMetrics metrics) throws Exception {
        log.info("导出性能指标: {}", metrics);
    }

    @Override
    public void init() throws Exception {
        log.info("指标导出器初始化完成");
    }

    @Override
    public void start() throws Exception {
        log.info("指标导出器已启动");
    }

    @Override
    public void stop() throws Exception {
        log.info("指标导出器已停止");
    }

    @Override
    public void close() throws Exception {
        log.info("指标导出器已关闭");
    }

}