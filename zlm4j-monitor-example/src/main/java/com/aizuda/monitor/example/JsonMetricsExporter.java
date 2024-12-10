package com.aizuda.monitor.example;

import com.aizuda.monitor.metrics.*;
import com.aizuda.monitor.storage.MetricsExporter;
import com.aizuda.monitor.annotation.SPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON指标导出器
 * 将监控指标导出为JSON文件
 */
@SPI("json")
public class JsonMetricsExporter implements MetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(JsonMetricsExporter.class);
    
    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    /** 输出目录 */
    private static final String OUTPUT_DIR = "metrics";
    
    public JsonMetricsExporter() {
        // 确保输出目录存在
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    @Override
    public String getName() {
        return "json";
    }
    
    @Override
    public <T extends Metrics> void export(Class<T> type, T metrics) throws Exception {
        if (metrics == null) {
            return;
        }
        
        String filename = generateFilename(type.getSimpleName().toLowerCase());
        
        try (FileWriter writer = new FileWriter(filename, true)) {
            // 添加时间戳和指标数据
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            data.put("metrics", metrics);
            
            writer.write(mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(data));
            writer.write("\n");
            writer.flush();
        } catch (IOException e) {
            log.error("导出指标失败: type={}, metrics={}", type.getSimpleName(), metrics, e);
            throw e;
        }
    }
    
    /**
     * 生成导出文件名
     * 
     * @param type 指标类型名称
     * @return 文件名
     */
    private String generateFilename(String type) {
        String date = dateFormat.format(new Date());
        return String.format("%s/%s_metrics_%s.json", 
            OUTPUT_DIR, 
            type, 
            date
        );
    }
    
    @Override
    public void init() throws Exception {
        log.info("初始化JSON指标导出器");
    }
    
    @Override
    public void destroy() throws Exception {
        log.info("销毁JSON指标导出器");
    }
    
    @Override
    public void start() throws Exception {
        log.info("启动JSON指标导出器");
    }
    
    @Override
    public void stop() throws Exception {
        log.info("停止JSON指标导出器");
    }
    
    @Override
    public void reset() throws Exception {
        log.info("重置JSON指标导出器");
        stop();
        start();
    }
    
    @Override
    public void flush() throws Exception {
        log.info("刷新JSON指标导出器缓存");
    }
    
    @Override
    public void exportSystem(SystemMetrics metrics) throws Exception {
        export(SystemMetrics.class, metrics);
    }
    
    @Override
    public void exportStream(StreamMetrics metrics) throws Exception {
        export(StreamMetrics.class, metrics);
    }
    
    @Override
    public void exportNetwork(NetworkMetrics metrics) throws Exception {
        export(NetworkMetrics.class, metrics);
    }
    
    @Override
    public void exportPerformance(PerformanceMetrics metrics) throws Exception {
        export(PerformanceMetrics.class, metrics);
    }
    
    @Override
    public void close() throws Exception {
        log.info("关闭JSON指标导出器");
    }
} 