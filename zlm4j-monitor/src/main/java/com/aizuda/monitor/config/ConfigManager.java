package com.aizuda.monitor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 配置管理器
 * 负责配置的加载、热更新和管理
 */
public class ConfigManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String DEFAULT_CONFIG_FILE = "monitor.yml";
    private static final String CONFIG_FILE_ENV = "MONITOR_CONFIG_FILE";
    private static final String CONFIG_FILE_PROP = "monitor.config.file";
    
    // 单例实现
    private static final ConfigManager INSTANCE = new ConfigManager();
    
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    private final MonitorConfig config;
    private final MonitorConfigLoader configLoader;
    private String configFile;  // 保留配置文件路径
    private final List<Consumer<MonitorConfig>> listeners = new CopyOnWriteArrayList<>();
    
    private ConfigManager() {
        this.config = new MonitorConfig();
        this.configLoader = new MonitorConfigLoader(this);
        initConfig();
    }
    
    /**
     * 设置配置文件路径
     */
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
        // 重新加载配置
        reloadConfig();
    }
    
    /**
     * 初始化配置
     * 按优先级尝试加载配置
     */
    private void initConfig() {
        // 1. 尝试从配置文件加载
        if (tryLoadConfigFile()) {
            return;
        }
        
        // 2. 使用默认配置
        loadDefaultConfig();
    }
    
    /**
     * 尝试加载配置文件
     * @return 是否加载成功
     */
    private boolean tryLoadConfigFile() {
        try {
            // 如果未指定配置文件，尝试从系统属性和环境变量获取
            if (configFile == null) {
                configFile = getConfigFile();
            }
            
            // 验证配置文件是否存在且可读
            File file = new File(configFile);
            if (!file.exists()) {
                log.warn("配置文件不存在: {}", configFile);
                return false;
            }

            // 验证配置文件是否可读
            if (!file.canRead()) {
                log.warn("配置文件无法��取: {}", configFile);
                return false;
            }
            
            // 加载配置文件
            MonitorConfig loadedConfig = configLoader.loadConfigFromFile(configFile);

            // 更新配置
            updateConfig(loadedConfig);
            
            // 启用热加载
            enableHotReload();
            
            log.info("已从配置文件加载配置: {}", configFile);
            return true;
            
        } catch (Exception e) {
            log.error("加载配置文件失败: {}", configFile, e);
            return false;
        }
    }
    
    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        // 重置为默认值
        config.initDefaultConfig();
        // 验证默认配置
        config.validate();
        log.info("使用默认配置");
    }
    
    /**
     * 更新配置
     */
    public void updateConfig(MonitorConfig newConfig) {
        synchronized (this) {
            try {
                // 验证新配置
                newConfig.validate();
                
                // 保存旧配置，用于回滚
                MonitorConfig oldConfig = new MonitorConfig();
                copyConfig(this.config, oldConfig);
                
                try {
                    // 更新配置
                    copyConfig(newConfig, this.config);
                    
                    // 通知配置变更
                    notifyConfigChange(this.config);
                    
                    log.info("配置已更新");
                } catch (Exception e) {
                    // 更新失败，回滚配置
                    copyConfig(oldConfig, this.config);
                    throw e;
                }
                
            } catch (Exception e) {
                log.error("更新配置失败", e);
                throw new RuntimeException("更新配置失败", e);
            }
        }
    }
    
    /**
     * 启用配置热加载
     */
    private void enableHotReload() {
        if (configFile != null) {
            configLoader.enableHotReload(configFile);
            log.info("已启用配置热加载");
        }
    }
    
    /**
     * 获取置文件路径
     */
    private String getConfigFile() {
        // 优先从系统属性获取
        String configFile = System.getProperty(CONFIG_FILE_PROP);
        if (configFile != null) {
            return configFile;
        }
        
        // 其次从环境变量获取
        configFile = System.getenv(CONFIG_FILE_ENV);
        if (configFile != null) {
            return configFile;
        }
        
        // 最后使用默认配置文件
        return DEFAULT_CONFIG_FILE;
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        synchronized (this) {  // 添加同步块
            try {
                // 保存旧配置，用于回滚
                MonitorConfig oldConfig = new MonitorConfig();
                copyConfig(this.config, oldConfig);
                
                // 尝试加载新配置
                if (!tryLoadConfigFile()) {
                    // 如果加载失败，回��到旧配置
                    copyConfig(oldConfig, this.config);
                    log.warn("加载新配置失败，已回滚到旧配置");
                }
            } catch (Exception e) {
                log.error("重新加载配置失败", e);
            }
        }
    }
    
    /**
     * 复制配置
     */
    private void copyConfig(MonitorConfig source, MonitorConfig target) {
        target.setDaemon(source.isDaemon());
        target.setSampleInterval(source.getSampleInterval());
        target.setInitialDelay(source.getInitialDelay());
        target.setThread(source.getThread());
        target.setExporter(source.getExporter());
        target.setMetrics(source.getMetrics());
    }
    
    /**
     * 获取当前配置
     */
    public MonitorConfig getConfig() {
        return config;
    }
    
    /**
     * 关闭配置管理器
     */
    @Override
    public void close() throws Exception {
        // 移除所有监听器
        listeners.clear();
        
        // 关闭配置加载器
        if (configLoader != null) {
            configLoader.close();
        }
    }
    
    public void addConfigChangeListener(Consumer<MonitorConfig> listener) {
        listeners.add(listener);
    }
    
    public void removeConfigChangeListener(Consumer<MonitorConfig> listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知配置变更
     */
    protected void notifyConfigChange(MonitorConfig newConfig) {
        for (Consumer<MonitorConfig> listener : listeners) {
            try {
                listener.accept(newConfig);
            } catch (Exception e) {
                log.error("配置变更通知失败", e);
            }
        }
    }
} 