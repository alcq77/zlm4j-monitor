package com.aizuda.monitor.metrics;

import com.aizuda.monitor.metrics.enums.MetricsType;

import java.util.Map;

/**
 * 指标接口
 * 定义了所有指标类型必须实现的基本操作
 * 
 * @author Cursor
 * @since 1.0
 */
public interface Metrics {
    
    /**
     * 获取指标类型
     *
     * @return 指标类型
     */
    MetricsType getType();
    
    /**
     * 获取指标标签
     * 标签用于对指标进行分类和过滤
     *
     * @return 标签Map，key为标签名，value为标签值
     */
    Map<String, String> getTags();
    
    /**
     * 获取指标值
     * 返回当前时间点的所有指标值
     *
     * @return 指标Map，key为指标名，value为指标值
     */
    Map<String, Number> getValues();
    
    /**
     * 重置指标
     * 清空所有指标值，准备下一轮收集
     */
    void reset();
    
    /**
     * 获取指标值
     * 获取指定名称的指标值
     *
     * @param name 指标名称
     * @return 指标值，如果不存在返回0
     */
    default Number getMetric(String name) {
        return getValues().getOrDefault(name, 0);
    }
    
    /**
     * 设��指标值
     * 设置指定名称的指标值
     *
     * @param name 指标名称
     * @param value 指标值
     */
    default void setMetric(String name, Number value) {
        getValues().put(name, value);
    }
    
    /**
     * 增加指标值
     * 对指定名称的指标值进行累加
     *
     * @param name 指标名称
     * @param delta 增加值
     */
    default void incrementMetric(String name, Number delta) {
        Map<String, Number> values = getValues();
        values.compute(name, (k, v) -> {
            if (v == null) {
                return delta;
            }
            if (v instanceof Long && delta instanceof Long) {
                return v.longValue() + delta.longValue();
            }
            if (v instanceof Integer && delta instanceof Integer) {
                return v.intValue() + delta.intValue();
            }
            return v.doubleValue() + delta.doubleValue();
        });
    }
    
    /**
     * 添加标签
     * 添加一个指标标签
     *
     * @param name 标签名
     * @param value 标签值
     */
    default void addTag(String name, String value) {
        getTags().put(name, value);
    }
    
    /**
     * 移除标签
     * 移除指定名称的标签
     *
     * @param name 标签名
     */
    default void removeTag(String name) {
        getTags().remove(name);
    }
    
    /**
     * 将指标转换为Map
     * 包含所有指标值和标签
     *
     * @return 包含所有指标数据的Map
     */
    Map<String, Object> toMap();
} 