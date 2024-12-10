package com.aizuda.monitor.collector;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.PerformanceMetrics;
import com.aizuda.monitor.metrics.enums.MetricsType;
import com.aizuda.zlm4j.callback.IMKSourceFindCallBack;
import com.aizuda.zlm4j.core.ZLMApi;
import com.aizuda.zlm4j.structure.MK_MEDIA_SOURCE;
import com.aizuda.zlm4j.structure.MK_TRACK;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 性能指标收集器
 * 负责收集ZLM服务器的性能相关指标
 */
public class PerformanceMetricsCollector extends AbstractMetricsCollector<PerformanceMetrics> {
    private static final Logger log = LoggerFactory.getLogger(PerformanceMetricsCollector.class);
    
    private final ZLMApi zlmApi;
    
    public PerformanceMetricsCollector(ZLMApi zlmApi, MonitorConfig config) {
        super(config);  // 调用父类构造函数
        this.zlmApi = zlmApi;
    }
    
    @Override
    protected boolean isCollectorEnabled(MonitorConfig config) {
        return config.getMetrics().getPerformance().isEnabled();
    }
    
    @Override
    protected void onConfigChange(MonitorConfig newConfig) {
        // 只处理配置变更通知，不修改收集逻辑
        if (!newConfig.getMetrics().getPerformance().isEnabled()) {
            log.info("性能指标收集已禁用");
        }
    }
    
    @Override
    public String getName() {
        return "performance";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.PERFORMANCE;
    }
    
    @Override
    protected PerformanceMetrics createMetrics() {
        return new PerformanceMetrics();
    }
    
    @Override
    protected void doCollect(PerformanceMetrics metrics) throws Exception {
        try {
            // 检查是否启用
            if (!config.getMetrics().getPerformance().isEnabled()) {
                log.debug("性能指标收集已禁用");
                return;
            }
            
            // 1. 收集编码性能指标
            collectEncoderMetrics(metrics);
            
            // 2. 收集解码性能指标
            collectDecoderMetrics(metrics);
            
            // 3. 收集转码性能指标
            collectTranscoderMetrics(metrics);
            
            // 4. 收集推流性能指标
            collectPublishMetrics(metrics);
            
            // 5. 收集播放性能指标
            collectPlayMetrics(metrics);
            
        } catch (Exception e) {
            log.error("收集性能指标失败", e);
            throw e;
        }
    }
    
    private void collectEncoderMetrics(PerformanceMetrics metrics) {
        // 获取所有媒体源
        zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource != null) {
                    // 获取每个 track 的性能指标
                    int trackCount = zlmApi.mk_media_source_get_track_count(mediaSource);
                    for (int i = 0; i < trackCount; i++) {
                        MK_TRACK track = zlmApi.mk_media_source_get_track(mediaSource, i);
                        if (track != null) {
                            try {
                                collectTrackMetrics(metrics, mediaSource, track);
                            } finally {
                                zlmApi.mk_track_unref(track);
                            }
                        }
                    }
                }
            }
        }, "", "", "", "");
    }
    
    private void collectDecoderMetrics(PerformanceMetrics metrics) {
        // 获取所有媒体源
        zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource != null) {
                    // 获取每个 track 的性能指标
                    int trackCount = zlmApi.mk_media_source_get_track_count(mediaSource);
                    for (int i = 0; i < trackCount; i++) {
                        MK_TRACK track = zlmApi.mk_media_source_get_track(mediaSource, i);
                        if (track != null) {
                            try {
                                collectTrackMetrics(metrics, mediaSource, track);
                            } finally {
                                // 释放 track 引用
                                zlmApi.mk_track_unref(track);
                            }
                        }
                    }
                }
            }
        }, "", "", "", "");
    }
    
    private void collectTranscoderMetrics(PerformanceMetrics metrics) {
        // 获取所有媒体源
        zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource != null) {
                    // 获取基本指标
                    int bytesSpeed = zlmApi.mk_media_source_get_bytes_speed(mediaSource);
                    long aliveSecond = zlmApi.mk_media_source_get_alive_second(mediaSource);
                    
                    metrics.setTranscoderBps(bytesSpeed);
                    metrics.setTranscoderTime(aliveSecond);
                }
            }
        }, "", "", "", "");
    }
    
    private void collectPublishMetrics(PerformanceMetrics metrics) {
        // 获取所有媒体源
        zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource != null) {
                    // 获取基本指标
                    int readerCount = zlmApi.mk_media_source_get_reader_count(mediaSource);
                    int totalReaderCount = zlmApi.mk_media_source_get_total_reader_count(mediaSource);
                    int bytesSpeed = zlmApi.mk_media_source_get_bytes_speed(mediaSource);
                    long aliveSecond = zlmApi.mk_media_source_get_alive_second(mediaSource);
                    
                    metrics.setPublishCount(readerCount);
                    metrics.setTotalPublishCount(totalReaderCount);
                    metrics.setPublishTime(aliveSecond);
                    metrics.setPublishBytes(bytesSpeed);
                }
            }
        }, "", "", "", "");
    }
    
    private void collectPlayMetrics(PerformanceMetrics metrics) {
        // 获取所有媒体源
        zlmApi.mk_media_source_for_each(null, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource != null) {
                    // 获取基本指标
                    int readerCount = zlmApi.mk_media_source_get_reader_count(mediaSource);
                    int totalReaderCount = zlmApi.mk_media_source_get_total_reader_count(mediaSource);
                    int bytesSpeed = zlmApi.mk_media_source_get_bytes_speed(mediaSource);
                    long aliveSecond = zlmApi.mk_media_source_get_alive_second(mediaSource);
                    
                    metrics.setPlayCount(readerCount);
                    metrics.setTotalPlayCount(totalReaderCount);
                    metrics.setPlayTime(aliveSecond);
                    metrics.setPlayBytes(bytesSpeed);
                }
            }
        }, "", "", "", "");
    }
    
    private void collectTrackMetrics(PerformanceMetrics metrics, MK_MEDIA_SOURCE mediaSource, MK_TRACK track) {
        try {
            // 获取基本指标
            int codecId = zlmApi.mk_track_codec_id(track);
            String codecName = zlmApi.mk_track_codec_name(track);
            int bitRate = zlmApi.mk_track_bit_rate(track);
            int isVideo = zlmApi.mk_track_is_video(track);
            int isReady = zlmApi.mk_track_ready(track);
            long duration = zlmApi.mk_track_duration(track);
            long frames = zlmApi.mk_track_frames(track);
            float loss = zlmApi.mk_media_source_get_track_loss(mediaSource, track);
            
            // 如果是视频轨道
            if (isVideo == 1) {
                int width = zlmApi.mk_track_video_width(track);
                int height = zlmApi.mk_track_video_height(track);
                int fps = zlmApi.mk_track_video_fps(track);
                int gopSize = zlmApi.mk_track_video_gop_size(track);
                int gopIntervalMs = zlmApi.mk_track_video_gop_interval_ms(track);
                long keyFrames = zlmApi.mk_track_video_key_frames(track);
                
                // 更新视频性能指标
                metrics.setVideoWidth(width);
                metrics.setVideoHeight(height);
                metrics.setVideoFps(fps);
                metrics.setVideoGopSize(gopSize);
                metrics.setVideoGopInterval(gopIntervalMs);
                metrics.setVideoKeyFrames(keyFrames);
            } else {
                // 音频轨道
                int sampleRate = zlmApi.mk_track_audio_sample_rate(track);
                int audioChannel = zlmApi.mk_track_audio_channel(track);
                int audioSampleBit = zlmApi.mk_track_audio_sample_bit(track);
                
                // 更新音频性能指标
                metrics.setAudioSampleRate(sampleRate);
                metrics.setAudioChannel(audioChannel);
                metrics.setAudioSampleBit(audioSampleBit);
            }
            
            // 更新通用指标
            metrics.setTrackCodecId(codecId);
            metrics.setTrackCodecName(codecName);
            metrics.setTrackBitRate(bitRate);
            metrics.setTrackReady(isReady == 1);
            metrics.setTrackDuration(duration);
            metrics.setTrackFrames(frames);
            metrics.setTrackLoss(loss);
            
        } catch (Exception e) {
            log.warn("获取 track 性能指标失败", e);
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
            log.error("关闭性能指标收集器失败", e);
        }
    }
    
    @Override
    protected void doInit() throws Exception {
        if (zlmApi == null) {
            throw new IllegalStateException("ZLM API未初始化");
        }
        log.info("性能指标收集器初始化完成");
    }
    
    @Override
    protected void doStart() throws Exception {
        log.info("性能指标收集器已启动");
    }
    
    @Override
    protected void doStop() throws Exception {
        log.info("性能指标收集器已停止");
    }
    
    @Override
    protected void doDestroy() throws Exception {
        log.info("性能指标收集器已销毁");
    }
} 