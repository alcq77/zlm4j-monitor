package com.aizuda.monitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * 监控配置加载器
 */
public class MonitorConfigLoader implements AutoCloseable {
    
    private static final Logger log = LoggerFactory.getLogger(MonitorConfigLoader.class);
    
    private final ObjectMapper yamlMapper;
    private final ObjectMapper jsonMapper;
    private final ConfigChangeListener listener;
    private FileWatcher fileWatcher;
    private final ConfigManager configManager;
    private final Object lock = new Object();
    
    public MonitorConfigLoader(ConfigManager configManager) {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
        this.configManager = configManager;
        this.listener = new ConfigChangeListener(configManager);
    }
    
    /**
     * 从文件加载配置
     */
    public MonitorConfig loadConfigFromFile(String configFile) throws IOException {
        File file = new File(configFile);
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + configFile);
        }
        
        try {
            MonitorConfig config;
            if (configFile.endsWith(".yml") || configFile.endsWith(".yaml")) {
                config = yamlMapper.readValue(file, MonitorConfig.class);
            } else if (configFile.endsWith(".json")) {
                config = jsonMapper.readValue(file, MonitorConfig.class);
            } else if (configFile.endsWith(".properties")) {
                config = loadFromProperties(new FileInputStream(file));
            } else {
                throw new IllegalArgumentException("不支持的配置文件类型: " + configFile);
            }
            
            // 验证配置
            config.validate();
            return config;
            
        } catch (IOException e) {
            throw new IOException("加载配置文件失败: " + configFile, e);
        }
    }
    
    /**
     * 从Properties输入流加载配置
     */
    private MonitorConfig loadFromProperties(InputStream input) throws IOException {
        Properties props = new Properties();
        props.load(input);
        
        MonitorConfig config = new MonitorConfig();
        // 设置基本配置
        config.setSystemMetricsEnabled(Boolean.parseBoolean(props.getProperty("metrics.system.enabled", "true")));
        config.setStreamMetricsEnabled(Boolean.parseBoolean(props.getProperty("metrics.stream.enabled", "true")));
        config.setNetworkMetricsEnabled(Boolean.parseBoolean(props.getProperty("metrics.network.enabled", "true")));
        config.setPerformanceMetricsEnabled(Boolean.parseBoolean(props.getProperty("metrics.performance.enabled", "true")));
        
        // 设置导出器
        String exporters = props.getProperty("exporters", "");
        if (!exporters.isEmpty()) {
            String[] exporterArray = exporters.split(",");
            for (String exporter : exporterArray) {
                config.getExporterNames().add(exporter.trim());
            }
        }
        
        // 验证配置
        config.validate();
        return config;
    }
    
    /**
     * 添加配置热加载支持
     */
    public void enableHotReload(String configFile) {
        fileWatcher = new FileWatcher(configFile);
        fileWatcher.addListener(listener);
        fileWatcher.start();
        log.info("已启用配置热加载: {}", configFile);
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        synchronized (lock) {
            try {
                if (configManager != null) {
                    configManager.reloadConfig();
                }
            } catch (Exception e) {
                log.error("重新加载配置失败", e);
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        if (fileWatcher != null) {
            try {
                fileWatcher.close();
            } catch (Exception e) {
                log.error("关闭文件监控失败", e);
                throw e;
            }
        }
    }
} 