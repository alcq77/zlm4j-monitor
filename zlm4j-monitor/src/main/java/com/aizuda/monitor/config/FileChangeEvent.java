package com.aizuda.monitor.config;

/**
 * 文件变更事件
 */
public class FileChangeEvent {

    /** 文件变更类型 */
    private final FileChangeType type;
    
    /** 变更的文件 */
    public FileChangeEvent(FileChangeType type) {
        this.type = type;
    }
    
    /**
     * 获取文件变更类型
     */
    public FileChangeType type() {
        return type;
    }
} 