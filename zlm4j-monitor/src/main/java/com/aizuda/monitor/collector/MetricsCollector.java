package com.aizuda.monitor.collector;

import com.aizuda.monitor.metrics.Metrics;
import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.enums.MetricsType;

/**
 * 指标收集器接口
 * 定义了所有收集器必须实现的基本操作
 * 
 * @author Cursor
 * @since 1.0
 * @param <T> 指标类型
 */
public interface MetricsCollector<T extends Metrics> {
    
    /**
     * 获取收集器名称
     * 用于标识不同的收集器
     *
     * @return 收集器名称
     */
    String getName();
    
    /**
     * 获取指标类型
     * 返回此收集器收集的指标类型
     *
     * @return 指标类型
     */
    MetricsType getType();
    
    /**
     * 获取指标类
     * 返回此收集器收集的指标类型
     *
     * @return 指标类型的Class对象
     */
    Class<T> getMetricsClass();
    
    /**
     * 初始化收集器
     * 在收集器开始工作前调用，用于初始化配置和资源
     *
     * @param config 监控配置
     * @throws Exception 初始化过程中的异常
     */
    void init(MonitorConfig config) throws Exception;
    
    /**
     * 启动收集器
     * 在收集器开始工作前调用
     *
     * @throws Exception 启动过程中的异常
     */
    void start() throws Exception;
    
    /**
     * 收集指标
     * 执行一次指标收集操作
     *
     * @return 收集到的指标对象
     * @throws Exception 收集过程中的异常
     */
    T collect() throws Exception;
    
    /**
     * 销毁收集器
     * 在收集器停止工作时调用，用于释放资源
     *
     * @throws Exception 销毁过程中的异常
     */
    void destroy() throws Exception;
} 