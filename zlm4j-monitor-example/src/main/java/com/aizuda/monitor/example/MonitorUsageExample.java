package com.aizuda.monitor.example;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.core.ZLMMonitor;
import com.aizuda.monitor.metrics.*;
import com.aizuda.zlm4j.core.ZLMApi;

/**
 * ZLM4J Monitor 使用示例
 * 展示从配置、初始化到使用的完整流程
 */
public class MonitorUsageExample {

    public static void main(String[] args) {
        try {
            // 1. 创建并配置 ZLM API 客户端
            ZLMApi zlmApi = createZLMApi();

            // 2. 创建监控配置
            MonitorConfig config = createMonitorConfig();

            // 3. 创建监控器
            ZLMMonitor monitor = new ZLMMonitor(zlmApi, config);

            // 4. 注册监控数据处理回调
            registerCallbacks(monitor);

            // 5. 启动监控
            monitor.start();

            // 6. 添加关闭钩子
            addShutdownHook(monitor);

            // 保持运行
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ZLMApi createZLMApi() {
        return new ZLMApi()
            .setHost("localhost")
            .setPort(80)
            .setSecret("your-secret");
    }

    private static MonitorConfig createMonitorConfig() {
        return new MonitorConfig()
            // 基础配置
            .setSampleInterval(5000)
            .setInitialDelay(0)
            .setDaemon(true)

            // 启用指标
            .setSystemMetricsEnabled(true)
            .setStreamMetricsEnabled(true)
            .setNetworkMetricsEnabled(true)

            // 配置导出器
            .addExporter("console")  // 控制台输出
            .addExporter("json")     // JSON文件输出
            .addExporter("prometheus");  // Prometheus导出器
    }

    private static void registerCallbacks(ZLMMonitor monitor) {
        // 注册指标回调
        monitor.addCallback((metrics) -> {
            if (metrics instanceof SystemMetrics) {
                handleSystemMetrics((SystemMetrics) metrics);
            } else if (metrics instanceof StreamMetrics) {
                handleStreamMetrics((StreamMetrics) metrics);
            } else if (metrics instanceof NetworkMetrics) {
                handleNetworkMetrics((NetworkMetrics) metrics);
            }
        });
    }

    private static void handleSystemMetrics(SystemMetrics metrics) {
        System.out.printf("系统指标: CPU=%.1f%%, 内存=%.1f%%, 磁盘=%.1f%%%n",
            metrics.getCpuUsage() * 100,
            metrics.getMemoryUsage() * 100,
            metrics.getDiskUsage() * 100);
    }

    private static void handleStreamMetrics(StreamMetrics metrics) {
        System.out.printf("流媒体指标: 在线流=%d, 带宽=%d MB/s, 客户端=%d%n",
            metrics.getStreamCount(),
            metrics.getBandwidthUsage() / 1024 / 1024,
            metrics.getClientCount());
    }

    private static void handleNetworkMetrics(NetworkMetrics metrics) {
        System.out.printf("网络指标: 连接数=%d, 上行=%d MB, 下行=%d MB%n",
            metrics.getConnectionCount(),
            metrics.getUploadBytes() / 1024 / 1024,
            metrics.getDownloadBytes() / 1024 / 1024);
    }

    private static void addShutdownHook(ZLMMonitor monitor) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("正在停止监控...");
                monitor.stop();
                System.out.println("监控已停止");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
} 