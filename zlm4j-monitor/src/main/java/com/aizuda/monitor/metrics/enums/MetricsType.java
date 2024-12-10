package com.aizuda.monitor.metrics.enums;

/**
 * 指标类型枚举
 * 定义了所有支持的指标类型
 * 
 * @author Cursor
 * @since 1.0
 */
public enum MetricsType {
    /** 系统指标 - CPU、内存、磁盘等 */
    SYSTEM,
    
    /** 流媒体指标 - 推流、拉流、转码等 */
    STREAM,
    
    /** 网络指标 - 带宽、延迟、丢包等 */
    NETWORK,
    
    /** 性能指标 - 编解码、缓冲区等 */
    PERFORMANCE
} 