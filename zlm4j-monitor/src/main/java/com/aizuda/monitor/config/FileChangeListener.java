package com.aizuda.monitor.config;

/**
 * 文件变更监听器
 */
@FunctionalInterface
public interface FileChangeListener {
    void onChange(FileChangeEvent event);
} 