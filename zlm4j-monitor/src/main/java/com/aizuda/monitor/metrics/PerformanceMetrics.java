package com.aizuda.monitor.metrics;

import com.aizuda.monitor.metrics.enums.MetricsType;
import java.util.Map;

/**
 * 性能指标
 * 包含编解码、转码、推流、播放等性能指标
 */
public class PerformanceMetrics extends AbstractMetrics {
    
    public static class MetricNames {
        /** 编码器指标 */
        public static final String ENCODER_COUNT = "encoder.count";
        public static final String ENCODER_TIME = "encoder.time";
        public static final String ENCODER_FPS = "encoder.fps";
        public static final String ENCODER_BPS = "encoder.bps";
        
        /** 解码器指标 */
        public static final String DECODER_COUNT = "decoder.count";
        public static final String DECODER_TIME = "decoder.time";
        public static final String DECODER_FPS = "decoder.fps";
        public static final String DECODER_BPS = "decoder.bps";
        
        /** 转码器指标 */
        public static final String TRANSCODER_COUNT = "transcoder.count";
        public static final String TRANSCODER_TIME = "transcoder.time";
        public static final String TRANSCODER_FPS = "transcoder.fps";
        public static final String TRANSCODER_BPS = "transcoder.bps";
        
        /** 推流指标 */
        public static final String PUBLISH_COUNT = "publish.count";
        public static final String PUBLISH_TIME = "publish.time";
        public static final String PUBLISH_BYTES = "publish.bytes";
        public static final String PUBLISH_SPEED = "publish.speed";
        public static final String TOTAL_PUBLISH_COUNT = "publish.total_count";
        
        /** 播放指标 */
        public static final String PLAY_COUNT = "play.count";
        public static final String PLAY_TIME = "play.time";
        public static final String PLAY_BYTES = "play.bytes";
        public static final String PLAY_SPEED = "play.speed";
        
        /** 线程池指标 */
        public static final String THREAD_POOL_PREFIX = "thread.pool.";
        
        /** Track 相关指标 */
        public static final String TRACK_CODEC_ID = "track.codec_id";
        public static final String TRACK_CODEC_NAME = "track.codec_name";
        public static final String TRACK_BIT_RATE = "track.bit_rate";
        public static final String TRACK_READY = "track.ready";
        public static final String TRACK_DURATION = "track.duration";
        public static final String TRACK_FRAMES = "track.frames";
        public static final String TRACK_LOSS = "track.loss";
        public static final String TRACK_TOTAL_READER = "track.total_reader";
        public static final String TRACK_READER = "track.reader";
        
        /** 视频轨道指标 */
        public static final String VIDEO_WIDTH = "video.width";
        public static final String VIDEO_HEIGHT = "video.height";
        public static final String VIDEO_FPS = "video.fps";
        public static final String VIDEO_GOP_SIZE = "video.gop_size";
        public static final String VIDEO_GOP_INTERVAL = "video.gop_interval";
        public static final String VIDEO_KEY_FRAMES = "video.key_frames";
        
        /** 音频轨道指标 */
        public static final String AUDIO_SAMPLE_RATE = "audio.sample_rate";
        public static final String AUDIO_CHANNEL = "audio.channel";
        public static final String AUDIO_SAMPLE_BIT = "audio.sample_bit";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.PERFORMANCE;
    }
    
    // 编码器相关方法
    public void setEncoderCount(int count) {
        setMetric(MetricNames.ENCODER_COUNT, count);
    }
    
    public void setEncoderTime(long time) {
        setMetric(MetricNames.ENCODER_TIME, time);
    }
    
    public void setEncoderFps(float fps) {
        setMetric(MetricNames.ENCODER_FPS, fps);
    }
    
    public void setEncoderBps(long bps) {
        setMetric(MetricNames.ENCODER_BPS, bps);
    }
    
    public void setEncoderBitRate(int bitRate) {
        setMetric(MetricNames.TRACK_BIT_RATE, bitRate);
    }
    
    public void setEncoderFrames(long frames) {
        setMetric(MetricNames.TRACK_FRAMES, frames);
    }
    
    public void setEncoderLoss(float loss) {
        setMetric(MetricNames.TRACK_LOSS, loss);
    }
    
    // 解码器相关方法
    public void setDecoderCount(int count) {
        setMetric(MetricNames.DECODER_COUNT, count);
    }
    
    public void setDecoderTime(long time) {
        setMetric(MetricNames.DECODER_TIME, time);
    }
    
    public void setDecoderFps(float fps) {
        setMetric(MetricNames.DECODER_FPS, fps);
    }
    
    public void setDecoderBps(long bps) {
        setMetric(MetricNames.DECODER_BPS, bps);
    }
    
    public void setDecoderBitRate(int bitRate) {
        setMetric(MetricNames.DECODER_BPS, bitRate);
    }
    
    public void setDecoderFrames(long frames) {
        setMetric(MetricNames.DECODER_COUNT, frames);
    }
    
    public void setDecoderLoss(float loss) {
        setMetric(MetricNames.DECODER_TIME, loss);
    }
    
    // 转码器相关方法
    public void setTranscoderCount(int count) {
        setMetric(MetricNames.TRANSCODER_COUNT, count);
    }
    
    public void setTranscoderTime(long time) {
        setMetric(MetricNames.TRANSCODER_TIME, time);
    }
    
    public void setTranscoderFps(float fps) {
        setMetric(MetricNames.TRANSCODER_FPS, fps);
    }
    
    public void setTranscoderBps(long bps) {
        setMetric(MetricNames.TRANSCODER_BPS, bps);
    }
    
    // 推流相关方法
    public void setPublishCount(int count) {
        setMetric(MetricNames.PUBLISH_COUNT, count);
    }
    
    public void setTotalPublishCount(int count) {
        setMetric(MetricNames.TOTAL_PUBLISH_COUNT, count);
    }
    
    public void setPublishTime(long time) {
        setMetric(MetricNames.PUBLISH_TIME, time);
    }
    
    public void setPublishBytes(long bytes) {
        setMetric(MetricNames.PUBLISH_BYTES, bytes);
    }
    
    public void setPublishSpeed(long speed) {
        setMetric(MetricNames.PUBLISH_SPEED, speed);
    }
    
    // 播放相关方法
    public void setPlayCount(int count) {
        setMetric(MetricNames.PLAY_COUNT, count);
    }
    
    public void setPlayTime(long time) {
        setMetric(MetricNames.PLAY_TIME, time);
    }
    
    public void setPlayBytes(long bytes) {
        setMetric(MetricNames.PLAY_BYTES, bytes);
    }
    
    public void setPlaySpeed(long speed) {
        setMetric(MetricNames.PLAY_SPEED, speed);
    }
    
    public void setTotalPlayCount(int count) {
        setMetric(MetricNames.TRACK_TOTAL_READER, count);
    }
    
    /**
     * 设置线程池指标
     * @param poolMetrics 线程池指标Map，key为线程池名称，value为指标值
     */
    public void setThreadPoolMetrics(Map<String, Number> poolMetrics) {
        if (poolMetrics == null) {
            return;
        }
        
        for (Map.Entry<String, Number> entry : poolMetrics.entrySet()) {
            String name = entry.getKey();
            Number value = entry.getValue();
            setMetric(MetricNames.THREAD_POOL_PREFIX + name, value);
        }
    }
    
    // 视频轨道相关方法
    public void setVideoWidth(int width) {
        setMetric(MetricNames.VIDEO_WIDTH, width);
    }
    
    public void setVideoHeight(int height) {
        setMetric(MetricNames.VIDEO_HEIGHT, height);
    }
    
    public void setVideoFps(int fps) {
        setMetric(MetricNames.VIDEO_FPS, fps);
    }
    
    public void setVideoGopSize(int gopSize) {
        setMetric(MetricNames.VIDEO_GOP_SIZE, gopSize);
    }
    
    public void setVideoGopInterval(int gopIntervalMs) {
        setMetric(MetricNames.VIDEO_GOP_INTERVAL, gopIntervalMs);
    }
    
    public void setVideoKeyFrames(long keyFrames) {
        setMetric(MetricNames.VIDEO_KEY_FRAMES, keyFrames);
    }
    
    // 音频轨道相关方法
    public void setAudioSampleRate(int sampleRate) {
        setMetric(MetricNames.AUDIO_SAMPLE_RATE, sampleRate);
    }
    
    public void setAudioChannel(int channel) {
        setMetric(MetricNames.AUDIO_CHANNEL, channel);
    }
    
    public void setAudioSampleBit(int sampleBit) {
        setMetric(MetricNames.AUDIO_SAMPLE_BIT, sampleBit);
    }
    
    // Track 相关方法
    // Track 通用指标相关方法
    public void setTrackCodecId(int codecId) {
        setMetric(MetricNames.TRACK_CODEC_ID, codecId);
    }
    
    public void setTrackBitRate(int bitRate) {
        setMetric(MetricNames.TRACK_BIT_RATE, bitRate);
    }
    
    public void setTrackReady(boolean ready) {
        setMetric(MetricNames.TRACK_READY, ready ? 1 : 0);
    }
    
    public void setTrackDuration(long duration) {
        setMetric(MetricNames.TRACK_DURATION, duration);
    }
    
    public void setTrackFrames(long frames) {
        setMetric(MetricNames.TRACK_FRAMES, frames);
    }
    
    public void setTrackLoss(float loss) {
        setMetric(MetricNames.TRACK_LOSS, loss);
    }
    
    public void setTrackCodecName(String codecName) {
        // 将 codec name 转换为 hash code 作为数字指标
        setMetric(MetricNames.TRACK_CODEC_NAME, codecName != null ? codecName.hashCode() : 0);
    }
} 