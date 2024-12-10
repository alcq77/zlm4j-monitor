package com.aizuda.monitor.metrics;

import com.aizuda.monitor.metrics.enums.MetricsType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 流媒体指标
 * 包含流媒体相关的各种指标
 */
public class StreamMetrics extends AbstractMetrics {
    
    /** 协议映射 */
    private static final Map<String, String> PROTOCOL_MAP = new ConcurrentHashMap<>();
    static {
        PROTOCOL_MAP.put("RTMP", "rtmp");
        PROTOCOL_MAP.put("RTSP", "rtsp");
        PROTOCOL_MAP.put("HTTP", "http");
        PROTOCOL_MAP.put("WS", "ws");
        PROTOCOL_MAP.put("RTC", "rtc");
        PROTOCOL_MAP.put("SRT", "srt");
        PROTOCOL_MAP.put("GB28181", "gb");
    }
    
    /** 协议计数器 */
    private final Map<String, AtomicLong> protocolStreams = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> protocolBytes = new ConcurrentHashMap<>();
    
    public static class MetricNames {
        /** 基础指标 */
        public static final String STREAM_COUNT = "stream.count";
        public static final String STREAM_BYTES = "stream.bytes";
        public static final String STREAM_ORIGIN_TYPE = "stream.origin_type";
        public static final String STREAM_ALIVE_SECONDS = "stream.alive_seconds";
        public static final String STREAM_READER_COUNT = "stream.reader_count";
        public static final String STREAM_TOTAL_READER_COUNT = "stream.total_reader_count";
        public static final String STREAM_BYTES_SPEED = "stream.bytes_speed";
        
        /** 协议指标前缀 */
        public static final String PROTOCOL_STREAMS_PREFIX = "protocol.streams.";
        public static final String PROTOCOL_BYTES_PREFIX = "protocol.bytes.";
        
        /** 视频轨道相关指标 */
        public static final String VIDEO_WIDTH = "video.width";
        public static final String VIDEO_HEIGHT = "video.height";
        public static final String VIDEO_FPS = "video.fps";
        public static final String VIDEO_GOP_SIZE = "video.gop_size";
        public static final String VIDEO_GOP_INTERVAL = "video.gop_interval";
        public static final String VIDEO_KEY_FRAMES = "video.key_frames";
        
        /** 音频轨道相关指标 */
        public static final String AUDIO_SAMPLE_RATE = "audio.sample_rate";
        public static final String AUDIO_CHANNEL = "audio.channel";
        public static final String AUDIO_SAMPLE_BIT = "audio.sample_bit";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.STREAM;
    }
    
    /**
     * 设置流媒体基本信息
     */
    public void setStreamInfo(String schema, String app, String stream, int originType, long aliveSecond) {
        setMetric(MetricNames.STREAM_ORIGIN_TYPE, originType);
        setMetric(MetricNames.STREAM_ALIVE_SECONDS, aliveSecond);
    }
    
    /**
     * 设置流媒体统计信息
     */
    public void setStreamStats(int readerCount, int totalReaderCount, int bytesSpeed) {
        setMetric(MetricNames.STREAM_READER_COUNT, readerCount);
        setMetric(MetricNames.STREAM_TOTAL_READER_COUNT, totalReaderCount);
        setMetric(MetricNames.STREAM_BYTES_SPEED, bytesSpeed);
    }
    
    /**
     * 增加协议流数量
     */
    public void incrementProtocolStreams(String protocol) {
        String normalizedProtocol = normalizeProtocol(protocol);
        protocolStreams.computeIfAbsent(normalizedProtocol, k -> new AtomicLong()).incrementAndGet();
        setMetric(MetricNames.PROTOCOL_STREAMS_PREFIX + normalizedProtocol, 
                 protocolStreams.get(normalizedProtocol).get());
    }
    
    /**
     * 添加协议流量
     */
    public void addProtocolBytes(String protocol, long bytes) {
        String normalizedProtocol = normalizeProtocol(protocol);
        protocolBytes.computeIfAbsent(normalizedProtocol, k -> new AtomicLong()).addAndGet(bytes);
        setMetric(MetricNames.PROTOCOL_BYTES_PREFIX + normalizedProtocol, 
                 protocolBytes.get(normalizedProtocol).get());
    }
    
    /**
     * 标准化协议名称
     */
    private String normalizeProtocol(String protocol) {
        if (protocol == null) {
            return "unknown";
        }
        String normalized = PROTOCOL_MAP.get(protocol.toUpperCase());
        return normalized != null ? normalized : protocol.toLowerCase();
    }
    
    /** 视频轨道相关指标 */
    public void setVideoInfo(int width, int height, int fps, int gopSize, int gopInterval, long keyFrames) {
        setMetric(MetricNames.VIDEO_WIDTH, width);
        setMetric(MetricNames.VIDEO_HEIGHT, height);
        setMetric(MetricNames.VIDEO_FPS, fps);
        setMetric(MetricNames.VIDEO_GOP_SIZE, gopSize);
        setMetric(MetricNames.VIDEO_GOP_INTERVAL, gopInterval);
        setMetric(MetricNames.VIDEO_KEY_FRAMES, keyFrames);
    }
    
    /** 音频轨道相关指标 */
    public void setAudioInfo(int sampleRate, int channels, int sampleBit) {
        setMetric(MetricNames.AUDIO_SAMPLE_RATE, sampleRate);
        setMetric(MetricNames.AUDIO_CHANNEL, channels);
        setMetric(MetricNames.AUDIO_SAMPLE_BIT, sampleBit);
    }
} 