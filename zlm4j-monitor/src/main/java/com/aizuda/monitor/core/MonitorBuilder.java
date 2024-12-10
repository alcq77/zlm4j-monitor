package com.aizuda.monitor.core;

import com.aizuda.monitor.config.ConfigManager;
import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.zlm4j.core.ZLMApi;

/**
 * ZLM监控构建器
 */
public class MonitorBuilder {
    private final ZLMApi zlmApi;
    private MonitorConfig config;
    private String configFile;
    
    public MonitorBuilder(ZLMApi zlmApi) {
        this.zlmApi = zlmApi;
    }
    
    public MonitorBuilder withConfig(MonitorConfig config) {
        this.config = config;
        return this;
    }
    
    public MonitorBuilder withConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }
    
    public ZLMMonitor build() {
        ConfigManager configManager = ConfigManager.getInstance();
        
        try {
            // 1. 优先使用配置文件
            if (configFile != null) {
                configManager.setConfigFile(configFile);
                config = configManager.getConfig();
            } 
            // 2. 其次使用自定义配置
            else if (config != null) {
                configManager.updateConfig(config);
                config = configManager.getConfig(); // 获取更新后的配置
            }
            // 3. 最后使用默认配置
            else {
                config = configManager.getConfig();
            }
            
            // 验证配置
            config.validate();
            
            return new ZLMMonitor(zlmApi, configManager, config);
            
        } catch (Exception e) {
            throw new RuntimeException("构建监控实例失败", e);
        }
    }
} 