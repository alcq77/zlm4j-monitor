package com.aizuda.monitor.storage;

import com.aizuda.monitor.metrics.*;
import com.aizuda.monitor.annotation.SPI;
/**
 * 指标导出器接口
 * 定义了所有导出器必须实现的基本操作
 * 
 * @author Cursor
 * @since 1.0
 */
@SPI("default")
public interface MetricsExporter extends AutoCloseable {
    
    /**
     * 获取导出器名称
     *
     * @return 导出器名称
     */
    String getName();
    
    /**
     * 初始化导出器
     */
    void init() throws Exception;
    
    /**
     * 导出指标数据
     */
    <T extends Metrics> void export(Class<T> type, T metrics) throws Exception;

    /**
     * 导出系统指标数据
     */
    void exportSystem(SystemMetrics metrics) throws Exception;
    
    /**
     * 导出流媒体指标数据
     */
    void exportStream(StreamMetrics metrics) throws Exception;

    /**
     * 导出网络指标数据
     */
    void exportNetwork(NetworkMetrics metrics) throws Exception;
    
    /**
     * 导出性能指标数据
     */
    void exportPerformance(PerformanceMetrics metrics) throws Exception;
    
    /**
     * 启动导出器
     */
    void start() throws Exception;
    
    /**
     * 停止导出器
     */
    void stop() throws Exception;
    
    /**
     * 关闭导出器
     */
    @Override
    void close() throws Exception;
} 