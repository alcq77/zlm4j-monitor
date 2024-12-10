package com.aizuda.monitor.example;

import com.aizuda.monitor.config.MonitorConfig;

public class MonitorConfigExample {

    /**
     * 创建开发环境配置
     */
    public static MonitorConfig createDevConfig() {
        return new MonitorConfig.Builder()
            .sampleInterval(3000)  // 3秒采样
            .exporterNames("console")  // 使用控制台输出
            .enableAllMetrics()  // 启用所有指标
            .build();
    }
    
    /**
     * 创建生产环境配置
     */
    public static MonitorConfig createProdConfig() {
        return new MonitorConfig.Builder()
            .sampleInterval(10000)  // 10秒采样
            .exporterNames("json")  // 使用JSON导出
            .enableSystemMetrics()  // 启用系统指标
            .enableStreamMetrics()  // 启用流媒体指标
            .build();
    }
    
    /**
     * 创建最小化配置
     */
    public static MonitorConfig createMinimalConfig() {
        return new MonitorConfig.Builder()
            .sampleInterval(30000)  // 30秒采样
            .exporterNames("default")  // 使用默认导出器
            .enableSystemMetrics()  // 仅启用系统指标
            .build();
    }
    
    /**
     * 创建调试配置
     */
    public static MonitorConfig createDebugConfig() {
        return new MonitorConfig.Builder()
            .sampleInterval(1000)  // 1秒采样
            .exporterNames("console", "json")  // 使用控制台和JSON导出
            .enableAllMetrics()  // 启用所有指标
            .build();
    }
} 