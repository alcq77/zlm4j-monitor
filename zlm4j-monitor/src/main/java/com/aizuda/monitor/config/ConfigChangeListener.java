package com.aizuda.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置变更监听器
 */
public class ConfigChangeListener implements FileChangeListener {
    private static final Logger log = LoggerFactory.getLogger(ConfigChangeListener.class);
    
    private final ConfigManager configManager;
    
    public ConfigChangeListener(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public void onChange(FileChangeEvent event) {
        try {
            configManager.reloadConfig();
            log.info("配置已重新加载");
        } catch (Exception e) {
            log.error("重新加载配置失败", e);
        }
    }
} 