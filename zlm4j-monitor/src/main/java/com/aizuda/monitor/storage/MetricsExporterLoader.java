package com.aizuda.monitor.storage;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aizuda.monitor.annotation.SPI;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指标导出器加载器
 * 使用SPI机制按需加载指标导出器实现
 */
public class MetricsExporterLoader {
    private static final Logger log = LoggerFactory.getLogger(MetricsExporterLoader.class);
    
    private static final String ZLMMONITOR_DIRECTORY = "META-INF/zlmmonitor/";
    private static final Map<String, Class<? extends MetricsExporter>> EXPORTER_CLASSES = new ConcurrentHashMap<>();
    private static final Map<String, MetricsExporter> EXPORTER_INSTANCES = new ConcurrentHashMap<>();
    
    static {
        // 加载所有导出器实现
        loadExporters();
    }
    
    /**
     * 加载所有可用的指标导出器
     */
    public static List<MetricsExporter> loadExporters() {
        // 如果已经加载过，直接返回缓存的实例
        if (!EXPORTER_INSTANCES.isEmpty()) {
            return new ArrayList<>(EXPORTER_INSTANCES.values());
        }
        
        synchronized (EXPORTER_INSTANCES) {
            if (!EXPORTER_INSTANCES.isEmpty()) {
                return new ArrayList<>(EXPORTER_INSTANCES.values());
            }
            
            // 加载SPI配置文件
            loadFromSpi();
            
            // 如果没有找到任何导出器，使用默认实现
            if (EXPORTER_CLASSES.isEmpty()) {
                log.warn("未找到任何指标导出器实现，使用默认实现");
                registerExporter("default", DefaultMetricsExporter.class);
            }
            
            // 实例化所有导出器
            for (Map.Entry<String, Class<? extends MetricsExporter>> entry : EXPORTER_CLASSES.entrySet()) {
                try {
                    MetricsExporter exporter = entry.getValue().newInstance();
                    EXPORTER_INSTANCES.put(entry.getKey(), exporter);
                } catch (Exception e) {
                    log.error("实例化导出器失败: " + entry.getValue().getName(), e);
                }
            }
            
            return new ArrayList<>(EXPORTER_INSTANCES.values());
        }
    }
    
    /**
     * 加载SPI配置文件中的导出器实现
     */
    private static void loadFromSpi() {
        String fileName = ZLMMONITOR_DIRECTORY + MetricsExporter.class.getName();
        try {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    loadResource(url);
                }
            }
        } catch (Throwable t) {
            log.error("加载导出器配置文件失败: " + fileName, t);
        }
    }
    
    /**
     * 加载资源文件
     */
    private static void loadResource(URL url) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 去除注释
                    final int ci = line.indexOf('#');
                    // 如果包含注释，则去除注释
                    if (ci >= 0) {
                        line = line.substring(0, ci);
                    }

                    // 去除前后空白字符
                    line = line.trim();

                    // 如果为空，则跳过
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        // 解析导出器定义
                        String name = null;

                        // 查找等号
                        int i = line.indexOf('=');
                        if (i > 0) {
                            // 解析导出器名称和类名
                            name = line.substring(0, i).trim();
                            // 获取类名
                            line = line.substring(i + 1).trim();
                        }
                        if (line.isEmpty()) {
                            continue;
                        }
                        
                        // 加载导出器类
                        Class<?> clazz = Class.forName(line, true, Thread.currentThread().getContextClassLoader());
                        if (!MetricsExporter.class.isAssignableFrom(clazz)) {
                            throw new IllegalStateException("导出器类 " + clazz.getName() + " 未实现 MetricsExporter 接口");
                        }
                        
                        // 获取导出器名称
                        if (name == null || name.isEmpty()) {
                            name = findExporterName(clazz);
                        }
                        
                        // 注册导出器
                        @SuppressWarnings("unchecked")
                        Class<? extends MetricsExporter> exporterClass = (Class<? extends MetricsExporter>) clazz;
                        registerExporter(name, exporterClass);
                    } catch (Throwable t) {
                        log.error("加载导出器类失败: " + line, t);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("加载导出器资源失败: " + url, t);
        }
    }
    
    /**
     * 查找导出器名称
     */
    private static String findExporterName(Class<?> clazz) {
        // 获取类名
        String className = clazz.getSimpleName();
        if (className.endsWith("MetricsExporter")) {
            // 如果类名以 MetricsExporter 结尾，则去除结尾部分
            className = className.substring(0, className.length() - "MetricsExporter".length());
        }
        // 将类名转换为小写
        return className.toLowerCase();
    }
    
    /**
     * 注册导出器
     */
    public static void registerExporter(String name, Class<? extends MetricsExporter> clazz) {
        EXPORTER_CLASSES.put(name, clazz);
    }
    
    /**
     * 获取默认导出器
     */
    public static MetricsExporter getDefaultExporter() throws Exception {
        // 从SPI注解中获取默认导出器名称
        String defaultName = MetricsExporter.class.getAnnotation(SPI.class).value();
        
        // 尝试获取默认导出器
        MetricsExporter exporter = EXPORTER_INSTANCES.get(defaultName);
        if (exporter != null) {
            return exporter;
        }
        
        // 如果没有找到默认导出器，创建一个新的实例
        Class<? extends MetricsExporter> exporterClass = EXPORTER_CLASSES.get(defaultName);
        if (exporterClass != null) {
            exporter = exporterClass.newInstance();
            EXPORTER_INSTANCES.put(defaultName, exporter);
            return exporter;
        }
        
        // 如果还是找不到，返回内置的默认实现
        log.warn("未找到默认导出器: {}, 使用内置默认实现", defaultName);
        return new DefaultMetricsExporter();
    }
    
    /**
     * 获取指定名称的导出器
     */
    public static MetricsExporter getExporter(String name) throws Exception {
        // 使用SPI机制加载导出器
        ServiceLoader<MetricsExporter> loader = ServiceLoader.load(MetricsExporter.class);

        // 遍历所有导出器
        for (MetricsExporter exporter : loader) {
            // 如果导出器名称匹配，则返回导出器实例
            if (exporter.getName().equals(name)) {
                return exporter;
            }
        }
        throw new IllegalArgumentException("未找到导出器: " + name);
    }
} 