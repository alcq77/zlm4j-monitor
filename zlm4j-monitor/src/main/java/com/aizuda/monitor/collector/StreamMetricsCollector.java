package com.aizuda.monitor.collector;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.StreamMetrics;
import com.aizuda.monitor.metrics.enums.MetricsType;
import com.aizuda.zlm4j.callback.IMKSourceFindCallBack;
import com.aizuda.zlm4j.core.ZLMApi;
import com.aizuda.zlm4j.structure.MK_MEDIA_SOURCE;
import com.aizuda.zlm4j.structure.MK_TRACK;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 流媒体指标收集器
 * 负责收集ZLM服务器的流媒体相关指标
 */
public class StreamMetricsCollector extends AbstractMetricsCollector<StreamMetrics> {
    private static final Logger log = LoggerFactory.getLogger(StreamMetricsCollector.class);
    
    private final ZLMApi zlmApi;
    
    public StreamMetricsCollector(ZLMApi zlmApi, MonitorConfig config) {
        super(config);  // 调用父类构造函数
        this.zlmApi = zlmApi;
    }
    
    @Override
    protected boolean isCollectorEnabled(MonitorConfig config) {
        return config.getMetrics().getStream().isEnabled();
    }
    
    @Override
    protected void onConfigChange(MonitorConfig newConfig) {
        // 只处理配置变更通知，不修改收集逻辑
        if (!newConfig.getMetrics().getStream().isEnabled()) {
            log.info("流媒体指标收集已禁用");
        }
    }
    
    @Override
    public String getName() {
        return "stream";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.STREAM;
    }
    
    @Override
    protected StreamMetrics createMetrics() {
        return new StreamMetrics();
    }
    
    @Override
    protected void doCollect(StreamMetrics metrics) throws Exception {
        try {
            // 检查是否启用
            if (!config.getMetrics().getStream().isEnabled()) {
                log.debug("流媒体指标收集已禁用");
                return;
            }
            
            // 收集流媒体指标
            final AtomicInteger totalStreams = new AtomicInteger(0);
            final Map<String, AtomicInteger> protocolStreams = new ConcurrentHashMap<>();
            
            zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
                @Override
                public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                    if (mediaSource != null) {
                        try {
                            // 统计总流数
                            totalStreams.incrementAndGet();
                            
                            // 获取流基本信息
                            String app = zlmApi.mk_media_source_get_app(mediaSource);
                            String stream = zlmApi.mk_media_source_get_stream(mediaSource);
                            String schema = zlmApi.mk_media_source_get_schema(mediaSource);
                            int originType = zlmApi.mk_media_source_get_origin_type(mediaSource);
                            long aliveSecond = zlmApi.mk_media_source_get_alive_second(mediaSource);
                            
                            // 获取流状态
                            int readerCount = zlmApi.mk_media_source_get_reader_count(mediaSource);
                            int totalReaderCount = zlmApi.mk_media_source_get_total_reader_count(mediaSource);
                            int bytesSpeed = zlmApi.mk_media_source_get_bytes_speed(mediaSource);
                            
                            // 更新流媒体指标
                            metrics.setStreamInfo(schema, app, stream, originType, aliveSecond);
                            metrics.setStreamStats(readerCount, totalReaderCount, bytesSpeed);
                            
                            // 更新协议统计
                            if (schema != null) {
                                metrics.incrementProtocolStreams(schema);
                                metrics.addProtocolBytes(schema, bytesSpeed);
                                
                                // 按协议类型统计流数
                                protocolStreams.computeIfAbsent(schema.toLowerCase(), k -> new AtomicInteger()).incrementAndGet();
                            }
                            
                            // 收集轨道信息
                            collectTrackInfo(metrics, mediaSource);
                            
                        } catch (Exception e) {
                            log.warn("收集流媒体指标失败: app={}, stream={}", 
                                   zlmApi.mk_media_source_get_app(mediaSource),
                                   zlmApi.mk_media_source_get_stream(mediaSource), e);
                        }
                    }
                }
            }, "", "", "", "");
            
            // 设置总流数
            metrics.setMetric(StreamMetrics.MetricNames.STREAM_COUNT, totalStreams.get());
            
            // 设置各协议流数
            for (Map.Entry<String, AtomicInteger> entry : protocolStreams.entrySet()) {
                String protocol = entry.getKey();
                int count = entry.getValue().get();
                metrics.setMetric(StreamMetrics.MetricNames.PROTOCOL_STREAMS_PREFIX + protocol, count);
            }
            
        } catch (Exception e) {
            log.error("收集流媒体指标失败", e);
            throw e;
        }
    }
    
    private void collectTrackInfo(StreamMetrics metrics, MK_MEDIA_SOURCE mediaSource) {
        int trackCount = zlmApi.mk_media_source_get_track_count(mediaSource);
        for (int i = 0; i < trackCount; i++) {
            MK_TRACK track = zlmApi.mk_media_source_get_track(mediaSource, i);
            if (track != null) {
                try {
                    int isVideo = zlmApi.mk_track_is_video(track);
                    if (isVideo == 1) {
                        // 收集视频轨道信息
                        int width = zlmApi.mk_track_video_width(track);
                        int height = zlmApi.mk_track_video_height(track);
                        int fps = zlmApi.mk_track_video_fps(track);
                        int gopSize = zlmApi.mk_track_video_gop_size(track);
                        int gopInterval = zlmApi.mk_track_video_gop_interval_ms(track);
                        long keyFrames = zlmApi.mk_track_video_key_frames(track);
                        
                        metrics.setVideoInfo(width, height, fps, gopSize, gopInterval, keyFrames);
                    } else {
                        // 收集音频轨道信息
                        int sampleRate = zlmApi.mk_track_audio_sample_rate(track);
                        int channels = zlmApi.mk_track_audio_channel(track);
                        int sampleBit = zlmApi.mk_track_audio_sample_bit(track);
                        
                        metrics.setAudioInfo(sampleRate, channels, sampleBit);
                    }
                } finally {
                    zlmApi.mk_track_unref(track);
                }
            }
        }
    }
    
    @Override
    public void start() throws Exception {
        doStart();
    }
    
    @Override
    public void close() {
        try {
            destroy();
        } catch (Exception e) {
            log.error("关闭流媒体指标收集器失败", e);
        }
    }
    
    @Override
    protected void doInit() throws Exception {
        if (zlmApi == null) {
            throw new IllegalStateException("ZLM API未初始化");
        }
        log.info("流媒体指标收集器初始化完成");
    }
    
    @Override
    protected void doStart() throws Exception {
        log.info("流媒体指标收集器已启动");
    }
    
    @Override
    protected void doStop() throws Exception {
        log.info("流媒体指标收集器已停止");
    }
    
    @Override
    protected void doDestroy() throws Exception {
        log.info("流媒体指标收集器已销毁");
    }
} 