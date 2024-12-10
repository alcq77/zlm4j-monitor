package com.aizuda.monitor.example;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.core.ZLMMonitor;
import com.aizuda.zlm4j.core.ZLMApi;

public class MonitorExample {
    public static void main(String[] args) {
        try {
            // 1. 创建ZLM API客户端
            ZLMApi zlmApi = ZLMApi.builder()
                .host("localhost")
                .port(80)
                .secret("035c73f7-bb6b-4889-a715-d9eb2d1925cc")
                .build();

            // 2. 创建监控配置
            MonitorConfig config = MonitorConfig.builder()
                .sampleInterval(5000)
                .addExporter("prometheus")
                .enableSystemMetrics()
                .enableStreamMetrics()
                .build();

            // 3. 创建并启动监控器
            ZLMMonitor monitor = ZLMMonitor.builder()
                .zlmApi(zlmApi)
                .config(config)
                .build();
            monitor.start();

            // 4. 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    monitor.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));

            // 保持运行
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 