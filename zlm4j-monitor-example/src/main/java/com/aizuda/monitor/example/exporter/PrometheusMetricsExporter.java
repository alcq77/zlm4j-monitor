package com.aizuda.monitor.example.exporter;

import com.aizuda.monitor.storage.MetricsExporter;
import com.aizuda.monitor.metrics.*;
import com.aizuda.monitor.annotation.SPI;
import io.prometheus.client.*;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;

/**
 * Prometheus 指标导出器
 * 提供 HTTP 接口供 Prometheus 抓取指标数据
 */
@SPI("prometheus")
public class PrometheusMetricsExporter implements MetricsExporter {
    private static final Logger log = LoggerFactory.getLogger(PrometheusMetricsExporter.class);
    
    private final CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    private HTTPServer server;

    // 系统资源指标
    private final Gauge zlmSystemCpuUsageRatio = Gauge.build()
        .name("zlm_system_cpu_usage_ratio")
        .help("ZLM system CPU usage ratio [0-1]")
        .labelNames("instance", "mode")  // mode: user/system/iowait/idle
        .register();
        
    private final Gauge zlmSystemMemoryBytes = Gauge.build()
        .name("zlm_system_memory_bytes")
        .help("ZLM system memory usage in bytes")
        .labelNames("instance", "type")  // type: total/used/free/cached/buffered
        .register();
        
    private final Gauge zlmSystemDiskBytes = Gauge.build()
        .name("zlm_system_disk_bytes") 
        .help("ZLM system disk usage in bytes")
        .labelNames("instance", "device", "mountpoint", "type")  // type: total/used/free
        .register();

    // 流媒体指标
    private final Counter zlmStreamTotal = Counter.build()
        .name("zlm_stream_total")
        .help("Total number of streams processed")
        .labelNames("instance", "protocol", "vhost", "app", "stream")
        .register();
        
    private final Counter zlmStreamBytesTotal = Counter.build()
        .name("zlm_stream_bytes_total")
        .help("Total bytes transferred by streams")
        .labelNames("instance", "protocol", "vhost", "app", "stream", "direction")
        .register();
        
    private final Gauge zlmStreamBitrate = Gauge.build()
        .name("zlm_stream_bitrate_bytes")
        .help("Current stream bitrate in bytes per second")
        .labelNames("instance", "protocol", "vhost", "app", "stream", "type")  // type: video/audio
        .register();

    // 客户端指标
    private final Gauge zlmClientGauge = Gauge.build()
        .name("zlm_client_current")
        .help("Current number of connected clients")
        .labelNames("instance", "protocol", "vhost", "app", "stream", "type")  // type: publisher/player
        .register();
        
    private final Counter zlmClientTotal = Counter.build()
        .name("zlm_client_total")
        .help("Total number of client connections")
        .labelNames("instance", "protocol", "vhost", "app", "stream", "type")
        .register();

    // 性能指标
    private final Histogram zlmStreamLatencySeconds = Histogram.build()
        .name("zlm_stream_latency_seconds")
        .help("Stream processing latency in seconds")
        .labelNames("instance", "protocol", "vhost", "app", "stream")
        .buckets(0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0)  // 1ms到1s的分布
        .register();

    private final Summary zlmStreamFrameRate = Summary.build()
        .name("zlm_stream_frame_rate")
        .help("Stream frame rate distribution")
        .labelNames("instance", "protocol", "vhost", "app", "stream", "type")
        .quantile(0.5, 0.05)
        .quantile(0.9, 0.01)
        .register();

    // 构建标签
    private Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("instance", getInstanceId());
        labels.put("hostname", getHostname());
        labels.put("version", getVersion());
        return labels;
    }

    // 导出系统指标
    @Override
    public void exportSystem(SystemMetrics metrics) {
        Map<String, String> labels = getLabels();
        
        zlmSystemCpuUsageRatio.labels(labels.values().toArray(new String[0]))
            .set(metrics.getCpuUsage());

        zlmSystemMemoryBytes.labels(labels.values().toArray(new String[0]))
            .set(metrics.getMemoryUsed());
            
        // ... 其他系统指标
    }

    @Override
    public void exportStream(StreamMetrics metrics) {
        Map<String, String> labels = getLabels();
        
        // 流数量
        zlmStreamTotal.labels(labels.get("instance"), "rtmp", "h264")
            .set(metrics.getStreamCount());
            
        // 流量统计
        zlmStreamBytesTotal.labels(labels.get("instance"), "rtmp", "in", "*")
            .inc(metrics.getBytesReceived());
        zlmStreamBytesTotal.labels(labels.get("instance"), "rtmp", "out", "*") 
            .inc(metrics.getBytesSent());
            
        // 客户端数
        zlmClientGauge.labels(labels.get("instance"), "rtmp", "player")
            .set(metrics.getClientCount());
    }

    @Override
    public void exportNetwork(NetworkMetrics metrics) {
        Map<String, String> labels = getLabels();
        
        // 流量统计
        zlmStreamBytesTotal.labels(labels.get("instance"), "*", "in", "*")
            .inc(metrics.getUploadBytes());
        zlmStreamBytesTotal.labels(labels.get("instance"), "*", "out", "*")
            .inc(metrics.getDownloadBytes());
    }

    @Override
    public void exportPerformance(PerformanceMetrics metrics) {
        Map<String, String> labels = getLabels();
        
        // 延迟统计
        zlmStreamLatencySeconds.labels(labels.get("instance"), "*", "*")
            .observe(metrics.getProcessingTime());
            
        // 码率统计
        zlmStreamBitrate.labels(labels.get("instance"), "*", "*")
            .observe(metrics.getBitrate());
    }

    @Override
    public <T extends Metrics> void export(Class<T> type, T metrics) {
        try {
            if (metrics instanceof SystemMetrics) {
                exportSystem((SystemMetrics) metrics);
            } else if (metrics instanceof StreamMetrics) {
                exportStream((StreamMetrics) metrics);
            } else if (metrics instanceof NetworkMetrics) {
                exportNetwork((NetworkMetrics) metrics);
            } else if (metrics instanceof PerformanceMetrics) {
                exportPerformance((PerformanceMetrics) metrics);
            }
        } catch (Exception e) {
            log.error("导出指标失败: type={}", type.getSimpleName(), e);
        }
    }

    private String getInstanceId() {
        return System.getProperty("zlm.instance.id", "default");
    }

    @Override
    public String getName() {
        return "prometheus";
    }

    @Override
    public void init() throws Exception {
        int port = Integer.getInteger("prometheus.port", 9090);
        server = new HTTPServer(port);
        log.info("Prometheus exporter started at :{}/metrics", port);
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void start() throws Exception {
        // 启动导出器
        log.info("Starting Prometheus exporter...");
    }

    @Override
    public void stop() throws Exception {
        // 停止导出器
        log.info("Stopping Prometheus exporter...");
    }

    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getVersion() {
        return System.getProperty("zlm.version", "unknown");
    }
} 