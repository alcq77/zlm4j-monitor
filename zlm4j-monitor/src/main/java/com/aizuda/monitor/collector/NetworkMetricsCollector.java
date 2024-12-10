package com.aizuda.monitor.collector;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.NetworkMetrics;
import com.aizuda.monitor.metrics.enums.MetricsType;
import com.aizuda.zlm4j.core.ZLMApi;
import com.aizuda.zlm4j.structure.MK_EVENTS;
import com.aizuda.zlm4j.callback.IMKStreamChangeCallBack;
import com.aizuda.zlm4j.callback.IMKFlowReportCallBack;
import com.aizuda.zlm4j.structure.MK_MEDIA_SOURCE;
import com.aizuda.zlm4j.structure.MK_MEDIA_INFO;
import com.aizuda.zlm4j.structure.MK_SOCK_INFO;
import com.aizuda.zlm4j.callback.IMKSourceFindCallBack;
import com.aizuda.zlm4j.callback.IMKGetStatisticCallBack;
import com.aizuda.zlm4j.callback.IMKFreeUserDataCallBack;
import com.aizuda.zlm4j.structure.MK_INI;
import com.aizuda.zlm4j.structure.MK_TRACK;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 网络指集器
 * 负责收集ZLM服务器的网络相关指标
 */
public class NetworkMetricsCollector extends AbstractMetricsCollector<NetworkMetrics> {
    private static final Logger log = LoggerFactory.getLogger(NetworkMetricsCollector.class);
    private final ZLMApi zlmApi;
    private final MK_EVENTS events;
    private final Map<String, AtomicLong> flowStats = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> streamStats = new ConcurrentHashMap<>();
    
    public NetworkMetricsCollector(ZLMApi zlmApi, MonitorConfig config) {
        super(config);
        this.zlmApi = zlmApi;
        this.events = new MK_EVENTS();
        initEvents();
    }
    
    private void initEvents() {
        // 流变更回调
        events.on_mk_media_changed = new IMKStreamChangeCallBack() {
            @Override
            public void invoke(int regist, MK_MEDIA_SOURCE mediaSource) {
                if (mediaSource == null) {
                    return;
                }
                String schema = zlmApi.mk_media_source_get_schema(mediaSource);
                if (schema != null) {
                    if (regist != 0) {
                        streamStats.computeIfAbsent(schema, k -> new AtomicInteger()).incrementAndGet();
                    } else {
                        AtomicInteger count = streamStats.get(schema);
                        if (count != null) {
                            count.decrementAndGet();
                        }
                    }
                }
            }
        };
        
        // 流量统计回调
        events.on_mk_flow_report = new IMKFlowReportCallBack() {
            @Override
            public void invoke(MK_MEDIA_INFO mediaInfo, long totalBytes, long speed, int isPlayer, MK_SOCK_INFO sockInfo) {
                if (mediaInfo == null) {
                    return;
                }
                String schema = zlmApi.mk_media_info_get_schema(mediaInfo);
                String vhost = zlmApi.mk_media_info_get_vhost(mediaInfo);
                String app = zlmApi.mk_media_info_get_app(mediaInfo);
                String stream = zlmApi.mk_media_info_get_stream(mediaInfo);
                
                if (schema != null) {
                    String key = schema + "://" + vhost + "/" + app + "/" + stream;
                    flowStats.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(speed);
                }
            }
        };
        
        zlmApi.mk_events_listen(events);
    }
    
    @Override
    public String getName() {
        return "network";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.NETWORK;
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
            log.error("关闭收集器失败", e);
        }
    }
    
    @Override
    protected void doInit() throws Exception {
        if (zlmApi == null) {
            throw new IllegalStateException("ZLM API未初始化");
        }
        log.info("网络指标收集器初始化完成");
    }
    
    @Override
    protected void doStart() throws Exception {
        log.info("网络指标收集器已启动");
    }
    
    @Override
    protected void doStop() throws Exception {
        log.info("网络指标收集器已停止");
    }
    
    @Override
    protected void doDestroy() throws Exception {
        log.info("网络指标收集器已销毁");
    }
    
    @Override
    protected NetworkMetrics createMetrics() {
        return new NetworkMetrics();
    }
    
    @Override
    protected void doCollect(NetworkMetrics metrics) throws Exception {
        try {
            // 检查是否启用
            if (!config.getMetrics().getNetwork().isEnabled()) {
                log.debug("网络指标收集已禁用");
                return;
            }
            
            // 1. 收集服务器状态指标
            collectServerMetrics(metrics);
            
            // 2. 收集流媒体指标
            collectMediaMetrics(metrics);
            
            // 3. 收集协议指标
            collectProtocolMetrics(metrics);
            
        } catch (Exception e) {
            log.error("收集网络指标失败", e);
            throw e;
        }
    }
    
    private void collectServerMetrics(NetworkMetrics metrics) {
        MK_INI ini = zlmApi.mk_ini_default();
        try {
            // 获取服务器端口信息
            String httpPort = zlmApi.mk_ini_get_option(ini, "http.port");
            String rtspPort = zlmApi.mk_ini_get_option(ini, "rtsp.port");
            String rtmpPort = zlmApi.mk_ini_get_option(ini, "rtmp.port");
            String rtcPort = zlmApi.mk_ini_get_option(ini, "webrtc.port");
            
            metrics.setHttpPort(Short.parseShort(httpPort));
            metrics.setRtspPort(Short.parseShort(rtspPort));
            metrics.setRtmpPort(Short.parseShort(rtmpPort));
            metrics.setRtcPort(Short.parseShort(rtcPort));
            
            // 获取服务器状态
            String httpEnabled = zlmApi.mk_ini_get_option(ini, "protocol.enable_http");
            String rtspEnabled = zlmApi.mk_ini_get_option(ini, "protocol.enable_rtsp");
            String rtmpEnabled = zlmApi.mk_ini_get_option(ini, "protocol.enable_rtmp");
            String rtcEnabled = zlmApi.mk_ini_get_option(ini, "protocol.enable_rtc");
            
            metrics.setHttpEnabled("1".equals(httpEnabled));
            metrics.setRtspEnabled("1".equals(rtspEnabled));
            metrics.setRtmpEnabled("1".equals(rtmpEnabled));
            metrics.setRtcEnabled("1".equals(rtcEnabled));
            
        } finally {
            // 确保资源被释放
            zlmApi.mk_ini_release(ini);
        }
    }
    
    private void collectMediaMetrics(NetworkMetrics metrics) {
        // 遍历所有媒体源
        zlmApi.mk_media_source_for_each(Pointer.NULL, new IMKSourceFindCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_MEDIA_SOURCE mediaSource) {
                // 获取基本信息
                String app = zlmApi.mk_media_source_get_app(mediaSource);
                String stream = zlmApi.mk_media_source_get_stream(mediaSource);
                String schema = zlmApi.mk_media_source_get_schema(mediaSource);
                
                // 获取流状态
                int readerCount = zlmApi.mk_media_source_get_reader_count(mediaSource);
                int totalReaderCount = zlmApi.mk_media_source_get_total_reader_count(mediaSource);
                int bytesSpeed = zlmApi.mk_media_source_get_bytes_speed(mediaSource);
                int originType = zlmApi.mk_media_source_get_origin_type(mediaSource);
                long aliveSecond = zlmApi.mk_media_source_get_alive_second(mediaSource);
                
                // 更新流媒体指标
                metrics.setStreamInfo(schema, app, stream, originType, aliveSecond);
                metrics.setStreamStats(readerCount, totalReaderCount, bytesSpeed);
                
                // 获取轨道信息
                int trackCount = zlmApi.mk_media_source_get_track_count(mediaSource);
                for (int i = 0; i < trackCount; i++) {
                    MK_TRACK track = zlmApi.mk_media_source_get_track(mediaSource, i);
                    try {
                        int isVideo = zlmApi.mk_track_is_video(track);
                        if (isVideo == 1) {
                            int width = zlmApi.mk_track_video_width(track);
                            int height = zlmApi.mk_track_video_height(track);
                            int fps = zlmApi.mk_track_video_fps(track);
                            metrics.updateVideoMetrics(width, height, fps);
                        }
                    } finally {
                        zlmApi.mk_track_unref(track);
                    }
                }
                
                // 更新协议统计
                if (schema != null) {
                    switch (schema.toLowerCase()) {
                        case "rtmp":
                            metrics.setRtmpConnections(readerCount);
                            metrics.setRtmpBytes(bytesSpeed);
                            break;
                        case "rtsp":
                            metrics.setRtspConnections(readerCount);
                            metrics.setRtspBytes(bytesSpeed);
                            break;
                        case "http":
                            metrics.updateConnectionMetrics(0, 0, readerCount, 0);
                            metrics.setHttpBytes(bytesSpeed);
                            break;
                        case "webrtc":  // 修改为 webrtc
                            metrics.setRtcConnections(readerCount);
                            metrics.setRtcBytes(bytesSpeed);
                            break;
                    }
                    
                    // 更新流的其他信息
                    metrics.addProtocolBytes(schema, bytesSpeed);
                    metrics.incrementProtocolConnections(schema);
                }
                
                // 更新总体统计
                metrics.setActiveConnections(totalReaderCount);
            }
        }, "", "", "", "");
    }
    
    private void collectProtocolMetrics(NetworkMetrics metrics) {
        BlockingQueue<Boolean> queue = new ArrayBlockingQueue<>(1);
        zlmApi.mk_get_statistic(new IMKGetStatisticCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_INI ini) {
                String tcpSession = zlmApi.mk_ini_get_option(ini, "object.TcpSession");
                String udpSession = zlmApi.mk_ini_get_option(ini, "object.UdpSession");
                
                metrics.setTcpConnections(Integer.parseInt(tcpSession));
                metrics.setUdpConnections(Integer.parseInt(udpSession));
                
                queue.offer(true);
            }
        }, Pointer.NULL, new IMKFreeUserDataCallBack() {
            @Override
            public void invoke(Pointer user_data) {
                // 清理资源
                queue.offer(true);
            }
        });
        
        try {
            queue.poll(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("获取统计信息超时", e);
        }
    }
    
    @Override
    protected boolean isCollectorEnabled(MonitorConfig config) {
        return config.getMetrics().getNetwork().isEnabled();
    }
    
    @Override
    protected void onConfigChange(MonitorConfig newConfig) {
        // 只处理配置变更通知，不修改收集逻辑
        if (!newConfig.getMetrics().getNetwork().isEnabled()) {
            log.info("网络指标收集已禁用");
        }
    }
} 