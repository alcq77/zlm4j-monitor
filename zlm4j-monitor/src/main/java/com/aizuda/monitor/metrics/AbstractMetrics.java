package com.aizuda.monitor.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象指标基类
 * 实现了 Metrics 接口的通用功能
 * 
 * @author Cursor
 * @since 1.0
 */
public abstract class AbstractMetrics implements Metrics {
    
    /** 指标值存储 */
    private final Map<String, Number> metrics = new ConcurrentHashMap<>();
    
    /** 标签存储 */
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    
    @Override
    public Map<String, String> getTags() {
        return tags;
    }
    
    @Override
    public Map<String, Number> getValues() {
        return metrics;
    }
    
    @Override
    public void reset() {
        metrics.clear();
        tags.clear();
    }
    
    /**
     * 获取指标前缀
     * 用于在指标名称前添加统一的前缀
     * 子类可以重写此方法来自定义前缀
     *
     * @return 指标前缀
     */
    protected String getMetricsPrefix() {
        return "";
    }
    
    @Override
    public void setMetric(String name, Number value) {
        String fullName = getMetricsPrefix() + name;
        metrics.put(fullName, value);
    }
    
    @Override
    public Number getMetric(String name) {
        String fullName = getMetricsPrefix() + name;
        return metrics.getOrDefault(fullName, 0);
    }
    
    @Override
    public void incrementMetric(String name, Number delta) {
        String fullName = getMetricsPrefix() + name;
        metrics.compute(fullName, (k, v) -> {
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

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new ConcurrentHashMap<>();
        // 添加所有指标值
        map.putAll(getValues().entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                    Map.Entry::getValue
            )));
        // 添加所有标签
        map.putAll(getTags());
        return map;
    }
} 