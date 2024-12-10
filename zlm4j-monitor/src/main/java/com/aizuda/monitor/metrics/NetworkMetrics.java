package com.aizuda.monitor.metrics;

import com.aizuda.monitor.metrics.enums.MetricsType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 网络指标
 * 包含各种网络协议的连接数、流量、丢包等指标
 * 
 * @author Cursor
 * @since 1.0
 */
public class NetworkMetrics extends AbstractMetrics {
    
    /** 协议映射 */
    public static final Map<String, String> PROTOCOL_MAP = new ConcurrentHashMap<>();
    static {
        PROTOCOL_MAP.put("HTTP", "http");
        PROTOCOL_MAP.put("RTSP", "rtsp");
        PROTOCOL_MAP.put("RTMP", "rtmp");
        PROTOCOL_MAP.put("WS", "ws");
        PROTOCOL_MAP.put("RTC", "rtc");
        PROTOCOL_MAP.put("SRT", "srt");
        PROTOCOL_MAP.put("GB28181", "gb");
        PROTOCOL_MAP.put("WEBRTC", "webrtc");
        PROTOCOL_MAP.put("UDP", "udp");
    }
    
    /** 协议计数器 */
    private final Map<String, AtomicLong> protocolConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> protocolBytes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> protocolPackets = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> protocolErrors = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> protocolLatency = new ConcurrentHashMap<>();
    
    /** 服务端口 */
    private short httpPort;
    private short rtspPort;
    private short rtmpPort;
    private short rtcPort;
    
    /** 服务状态 */
    private boolean httpEnabled;
    private boolean rtspEnabled;
    private boolean rtmpEnabled;
    private boolean rtcEnabled;
    
    /** 默认值常量 */
        private static final int DEFAULT_CONNECTION_COUNT = 0;
        private static final long DEFAULT_BYTES_COUNT = 0L;
    
        public static class MetricNames {
            /** 基础指标 */
            public static final String ACTIVE_CONNECTIONS = "active.connections";
            public static final String BYTES_IN = "bytes.in";
            public static final String BYTES_OUT = "bytes.out";
            public static final String PACKETS_IN = "packets.in";
            public static final String PACKETS_OUT = "packets.out";
            public static final String PACKETS_LOST = "packets.lost";
            
            /** UDP指标 */
            public static final String UDP_CONNECTIONS = "udp.connections";
            public static final String UDP_BYTES_IN = "udp.bytes.in";
            public static final String UDP_BYTES_OUT = "udp.bytes.out";
            public static final String UDP_PACKETS_LOST = "udp.packets.lost";
            public static final String UDP_TOTAL_PACKETS = "udp.total.packets";
            public static final String UDP_PACKET_LOSS_RATE = "udp.packet.loss.rate";
            
            /** HTTP指标 */
            public static final String HTTP_CONNECTIONS = "http.connections";
            public static final String HTTP_BYTES = "http.bytes";
            public static final String HTTP_FLV_CONNECTIONS = "http.flv.connections";
            public static final String HTTP_HLS_CONNECTIONS = "http.hls.connections";
            public static final String HTTP_FMP4_CONNECTIONS = "http.fmp4.connections";
            public static final String HTTP_TS_CONNECTIONS = "http.ts.connections";
            public static final String HTTP_STREAMS = "http.streams";
            
            /** WebSocket指标 */
            public static final String WS_FLV_CONNECTIONS = "ws.flv.connections";
            public static final String WS_HLS_CONNECTIONS = "ws.hls.connections";
            public static final String WS_FMP4_CONNECTIONS = "ws.fmp4.connections";
            
            /** RTMP指标 */
            public static final String RTMP_CONNECTIONS = "rtmp.connections";
            public static final String RTMP_BYTES = "rtmp.bytes";
            public static final String RTMP_STREAMS = "rtmp.streams";
            
            /** RTSP指标 */
            public static final String RTSP_CONNECTIONS = "rtsp.connections";
            public static final String RTSP_BYTES = "rtsp.bytes";
            public static final String RTSP_STREAMS = "rtsp.streams";
            
            /** RTC指标 */
            public static final String RTC_CONNECTIONS = "rtc.connections";
            public static final String RTC_BYTES = "rtc.bytes";
            public static final String RTC_STREAMS = "rtc.streams";
            
            /** TCP指标 */
            public static final String TCP_CONNECTIONS = "tcp.connections";
            
            /** 网络质量指标 */
            public static final String NETWORK_QUALITY = "network.quality";
            public static final String NETWORK_JITTER = "network.jitter";
            public static final String NETWORK_RTT = "network.rtt";
            public static final String NETWORK_LOSS_RATE = "network.loss.rate";
            public static final String NETWORK_BANDWIDTH = "network.bandwidth";
            
            /** 客户端指标 */
            public static final String CLIENT_COUNT = "client.count";
            public static final String CLIENT_ACTIVE = "client.active";
            public static final String CLIENT_PEAK = "client.peak";
            
            /** RTP/RTCP指标 */
            public static final String RTP_PACKETS = "rtp.packets";
            public static final String RTCP_PACKETS = "rtcp.packets";
            public static final String DTLS_HANDSHAKES = "dtls.handshakes";
            public static final String SRTP_PACKETS = "srtp.packets";
            
            /** 带宽指标 */
            public static final String BANDWIDTH_USAGE = "bandwidth.usage";
            public static final String BANDWIDTH_LIMIT = "bandwidth.limit";
            public static final String BANDWIDTH_PEAK = "bandwidth.peak";
            
            /** 视频指标 */
            public static final String VIDEO_WIDTH = "video.width";
            public static final String VIDEO_HEIGHT = "video.height";
            public static final String VIDEO_FPS = "video.fps";
            
            /** 流媒体指标 */
            public static final String STREAM_ORIGIN_TYPE = "stream.origin_type";
            public static final String STREAM_ALIVE_SECONDS = "stream.alive_seconds";
            public static final String STREAM_READER_COUNT = "stream.reader_count";
            public static final String STREAM_TOTAL_READER_COUNT = "stream.total_reader_count";
            public static final String STREAM_BYTES_SPEED = "stream.bytes_speed";
        }
        
        public NetworkMetrics() {
            // 初始化协计数器
            for (String protocol : PROTOCOL_MAP.keySet()) {
                protocolConnections.put(protocol, new AtomicLong());
                protocolBytes.put(protocol, new AtomicLong());
                protocolPackets.put(protocol, new AtomicLong());
                protocolErrors.put(protocol, new AtomicLong());
                protocolLatency.put(protocol, new AtomicLong());
            }
        }
        
        @Override
        public MetricsType getType() {
            return MetricsType.NETWORK;
        }
        
        @Override
        public void reset() {
            super.reset();
            
            // 重置协议计数器
            for (String protocol : PROTOCOL_MAP.keySet()) {
                protocolConnections.get(protocol).set(0);
                protocolBytes.get(protocol).set(0);
                protocolPackets.get(protocol).set(0);
                protocolErrors.get(protocol).set(0);
                protocolLatency.get(protocol).set(0);
            }
        }
        
        /**
         * 增加协议连接数
         */
        public void incrementProtocolConnections(String protocol) {
            AtomicLong counter = protocolConnections.get(protocol);
            if (counter != null) {
                counter.incrementAndGet();
            }
        }
        
        /**
         * 增加协议字节数
         */
        public void addProtocolBytes(String protocol, long bytes) {
            AtomicLong counter = protocolBytes.get(protocol);
            if (counter != null) {
                counter.addAndGet(bytes);
            }
        }
        
        /**
         * 增加协议数据包数
         */
        public void addProtocolPackets(String protocol, long packets) {
            AtomicLong counter = protocolPackets.get(protocol);
            if (counter != null) {
                counter.addAndGet(packets);
            }
        }
        
        /**
         * 增加协议错误数
         */
        public void incrementProtocolErrors(String protocol) {
            AtomicLong counter = protocolErrors.get(protocol);
            if (counter != null) {
                counter.incrementAndGet();
            }
        }
        
        /**
         * 更新协议延迟
         */
        public void updateProtocolLatency(String protocol, long latency) {
            AtomicLong counter = protocolLatency.get(protocol);
            if (counter != null) {
                counter.set(latency);
            }
        }
        
        /**
         * 更新协议指标
         */
        public void updateProtocolMetrics() {
            for (Map.Entry<String, String> entry : PROTOCOL_MAP.entrySet()) {
                String protocol = entry.getKey();
                String prefix = entry.getValue();
                
                setMetric(prefix + ".connections", protocolConnections.get(protocol).get());
                setMetric(prefix + ".bytes", protocolBytes.get(protocol).get());
                setMetric(prefix + ".packets", protocolPackets.get(protocol).get());
                setMetric(prefix + ".errors", protocolErrors.get(protocol).get());
                setMetric(prefix + ".latency", protocolLatency.get(protocol).get());
            }
        }
        
        /**
         * 更新总体指标
         */
        public void updateTotalMetrics() {
            long totalConnections = 0;
            long totalBytesIn = 0;
            long totalBytesOut = 0;
            long totalPacketsIn = 0;
            long totalPacketsOut = 0;
            long totalPacketsLost = 0;
            
            for (Map.Entry<String, String> entry : PROTOCOL_MAP.entrySet()) {
                String protocol = entry.getKey();
                totalConnections += protocolConnections.get(protocol).get();
                totalBytesIn += protocolBytes.get(protocol).get();
                totalPacketsIn += protocolPackets.get(protocol).get();
            }
            
            setMetric(MetricNames.ACTIVE_CONNECTIONS, totalConnections);
            setMetric(MetricNames.BYTES_IN, totalBytesIn);
            setMetric(MetricNames.BYTES_OUT, totalBytesOut);
            setMetric(MetricNames.PACKETS_IN, totalPacketsIn);
            setMetric(MetricNames.PACKETS_OUT, totalPacketsOut);
            setMetric(MetricNames.PACKETS_LOST, totalPacketsLost);
        }
        
        /**
         * 获取活跃连接数
         */
        public int getActiveConnections() {
            return getMetric(MetricNames.ACTIVE_CONNECTIONS).intValue();
        }
        
        /**
         * 设置活跃连接数
         */
        public void setActiveConnections(int connections) {
            setMetric(MetricNames.ACTIVE_CONNECTIONS, connections);
        }
    
    /**
     * 获取入站流量(字节)
     */
    public long getBytesIn() {
        return getMetric(MetricNames.BYTES_IN).longValue();
    }
    
    /**
     * 设入站流量(字节)
     */
    public void setBytesIn(long bytes) {
        setMetric(MetricNames.BYTES_IN, bytes);
    }
    
    /**
     * 获取出站流量(字节)
     */
    public long getBytesOut() {
        return getMetric(MetricNames.BYTES_OUT).longValue();
    }
    
    /**
     * 设置出站流量(字节)
     */
    public void setBytesOut(long bytes) {
        setMetric(MetricNames.BYTES_OUT, bytes);
    }
    
    /**
     * 获取入站数据包数
     */
    public long getPacketsIn() {
        return getMetric(MetricNames.PACKETS_IN).longValue();
    }
    
    /**
     * 设置入站数据包数
     */
    public void setPacketsIn(long packets) {
        setMetric(MetricNames.PACKETS_IN, packets);
    }
    
    /**
     * 获取出站数据包数
     */
    public long getPacketsOut() {
        return getMetric(MetricNames.PACKETS_OUT).longValue();
    }
    
    /**
     * 设置出站数据包数
     */
    public void setPacketsOut(long packets) {
        setMetric(MetricNames.PACKETS_OUT, packets);
    }
    
    /**
     * 获取丢失数据包数
     */
    public long getPacketsLost() {
        return getMetric(MetricNames.PACKETS_LOST).longValue();
    }
    
    /**
     * 设置失数据包数
     */
    public void setPacketsLost(long packets) {
        setMetric(MetricNames.PACKETS_LOST, packets);
    }
    
    /**
     * 获取UDP丢包率
     */
    public float getUdpPacketLossRate() {
        return getMetric(MetricNames.UDP_PACKET_LOSS_RATE).floatValue();
    }
    
    /**
     * 更新UDP丢包率
     */
    public void updateUdpPacketLossRate(long packetsLost, long totalPackets) {
        if (totalPackets > 0) {
            float rate = (float) packetsLost / totalPackets;
            setMetric(MetricNames.UDP_PACKET_LOSS_RATE, rate);
            setMetric(MetricNames.UDP_PACKETS_LOST, packetsLost);
            setMetric(MetricNames.UDP_TOTAL_PACKETS, totalPackets);
        }
    }
    
    /**
     * 获取UDP连接数
     */
    public int getUdpConnections() {
        return getMetric(MetricNames.UDP_CONNECTIONS).intValue();
    }
    
    /**
     * 设置UDP连接数
     */
    public void setUdpConnections(int connections) {
        setMetric(MetricNames.UDP_CONNECTIONS, connections);
    }
    
    /**
     * 获取UDP入站流量(字节)
     */
    public long getUdpBytesIn() {
        return getMetric(MetricNames.UDP_BYTES_IN).longValue();
    }
    
    /**
     * 设置UDP入站流(字节)
     */
    public void setUdpBytesIn(long bytes) {
        setMetric(MetricNames.UDP_BYTES_IN, bytes);
    }
    
    /**
     * 获取UDP出站流量(字节)
     */
    public long getUdpBytesOut() {
        return getMetric(MetricNames.UDP_BYTES_OUT).longValue();
    }
    
    /**
     * 设置UDP出站流量(字节)
     */
    public void setUdpBytesOut(long bytes) {
        setMetric(MetricNames.UDP_BYTES_OUT, bytes);
    }
    
    /**
     * 获取RTP数据包数
     */
    public long getRtpPackets() {
        return getMetric(MetricNames.RTP_PACKETS).longValue();
    }
    
    /**
     * 设置RTP数据包数
     */
    public void setRtpPackets(long packets) {
        setMetric(MetricNames.RTP_PACKETS, packets);
    }
    
    /**
     * 获取RTCP数据包数
     */
    public long getRtcpPackets() {
        return getMetric(MetricNames.RTCP_PACKETS).longValue();
    }
    
    /**
     * 设RTCP数据包数
     */
    public void setRtcpPackets(long packets) {
        setMetric(MetricNames.RTCP_PACKETS, packets);
    }
    
    /**
     * 获取DTLS握手次数
     */
    public long getDtlsHandshakes() {
        return getMetric(MetricNames.DTLS_HANDSHAKES).longValue();
    }
    
    /**
     * 设DTLS握手次数
     */
    public void setDtlsHandshakes(long handshakes) {
        setMetric(MetricNames.DTLS_HANDSHAKES, handshakes);
    }
    
    /**
     * 获取SRTP数据包数
     */
    public long getSrtpPackets() {
        return getMetric(MetricNames.SRTP_PACKETS).longValue();
    }
    
    /**
     * 设置SRTP数据包数
     */
    public void setSrtpPackets(long packets) {
        setMetric(MetricNames.SRTP_PACKETS, packets);
    }
    
    /**
     * 获取HTTP-FLV连接数
     */
    public int getHttpFlvConnections() {
        return getMetric(MetricNames.HTTP_FLV_CONNECTIONS).intValue();
    }
    
    /**
     * 设置HTTP-FLV连接数
     */
    public void setHttpFlvConnections(int connections) {
        setMetric(MetricNames.HTTP_FLV_CONNECTIONS, connections);
    }
    
    /**
     * 获取HLS连接数
     */
    public int getHlsConnections() {
        return getMetric(MetricNames.HTTP_HLS_CONNECTIONS).intValue();
    }
    
    /**
     * 设置HLS连接数
     */
    public void setHlsConnections(int connections) {
        setMetric(MetricNames.HTTP_HLS_CONNECTIONS, connections);
    }
    
    /**
     * 获取HTTP-FMP4连接数
     */
    public int getHttpFmp4Connections() {
        return getMetric(MetricNames.HTTP_FMP4_CONNECTIONS).intValue();
    }
    
    /**
     * 设置HTTP-FMP4连接数
     */
    public void setHttpFmp4Connections(int connections) {
        setMetric(MetricNames.HTTP_FMP4_CONNECTIONS, connections);
    }
    
    /**
     * 获取HTTP-TS连接数
     */
    public int getHttpTsConnections() {
        return getMetric(MetricNames.HTTP_TS_CONNECTIONS).intValue();
    }
    
    /**
     * 设置HTTP-TS连接数
     */
    public void setHttpTsConnections(int connections) {
        setMetric(MetricNames.HTTP_TS_CONNECTIONS, connections);
    }
    
    /**
     * 获取WebSocket-FLV连接数
     */
    public int getWsFlvConnections() {
        return getMetric(MetricNames.WS_FLV_CONNECTIONS).intValue();
    }
    
    /**
     * 设置WebSocket-FLV连接数
     */
    public void setWsFlvConnections(int connections) {
        setMetric(MetricNames.WS_FLV_CONNECTIONS, connections);
    }
    
    /**
     * 获取WebSocket-HLS连接数
     */
    public int getWsHlsConnections() {
        return getMetric(MetricNames.WS_HLS_CONNECTIONS).intValue();
    }
    
    /**
     * 设置WebSocket-HLS连接数
     */
    public void setWsHlsConnections(int connections) {
        setMetric(MetricNames.WS_HLS_CONNECTIONS, connections);
    }
    
    /**
     * 获取WebSocket-FMP4连接数
     */
    public int getWsFmp4Connections() {
        return getMetric(MetricNames.WS_FMP4_CONNECTIONS).intValue();
    }
    
    /**
     * 设置WebSocket-FMP4连接数
     */
    public void setWsFmp4Connections(int connections) {
        setMetric(MetricNames.WS_FMP4_CONNECTIONS, connections);
    }
    
    /**
     * 获取RTMP连接数
     */
    public int getRtmpConnections() {
        Number value = getMetric(MetricNames.RTMP_CONNECTIONS);
        return value != null ? value.intValue() : DEFAULT_CONNECTION_COUNT;
    }
    
    /**
     * 设置RTMP连接数
     */
    public void setRtmpConnections(int connections) {
        validateConnectionCount(connections);
        setMetric(MetricNames.RTMP_CONNECTIONS, connections);
    }
    
    /**
     * 获取RTMP流量(字节)
     */
    public long getRtmpBytes() {
        Number value = getMetric(MetricNames.RTMP_BYTES);
        return value != null ? value.longValue() : DEFAULT_BYTES_COUNT;
    }
    
    /**
     * 设置RTMP流量(字节)
     */
    public void setRtmpBytes(long bytes) {
        validateBytesCount(bytes);
        setMetric(MetricNames.RTMP_BYTES, bytes);
    }
    
    /**
     * 获取RTSP连接数
     */
    public int getRtspConnections() {
        Number value = getMetric(MetricNames.RTSP_CONNECTIONS);
        return value != null ? value.intValue() : DEFAULT_CONNECTION_COUNT;
    }
    
    /**
     * 设置RTSP连接数
     */
    public void setRtspConnections(int connections) {
        validateConnectionCount(connections);
        setMetric(MetricNames.RTSP_CONNECTIONS, connections);
    }
    
    /**
     * 取RTSP流量(字节)
     */
    public long getRtspBytes() {
        Number value = getMetric(MetricNames.RTSP_BYTES);
        return value != null ? value.longValue() : DEFAULT_BYTES_COUNT;
    }
    
    /**
     * 设置RTSP流量(字节)
     */
    public void setRtspBytes(long bytes) {
        validateBytesCount(bytes);
        setMetric(MetricNames.RTSP_BYTES, bytes);
    }
    
    /**
     * 获取RTC连接数
     */
    public int getRtcConnections() {
        return getMetric(MetricNames.RTC_CONNECTIONS).intValue();
    }
    
    /**
     * 设置RTC连接数
     */
    public void setRtcConnections(int connections) {
        setMetric(MetricNames.RTC_CONNECTIONS, connections);
    }
    
    /**
     * 获取RTC流量(字节)
     */
    public long getRtcBytes() {
        return getMetric(MetricNames.RTC_BYTES).longValue();
    }
    
    /**
     * 设置RTC流量(字节)
     */
    public void setRtcBytes(long bytes) {
        setMetric(MetricNames.RTC_BYTES, bytes);
    }
    
    /**
     * 设置连接数
     */
    public void setConnectionCount(int count) {
        setActiveConnections(count);
    }

    /**
     * 添加协议错误数
     */
    public void addProtocolErrors(String protocol, long errors) {
        AtomicLong counter = protocolErrors.get(protocol);
        if (counter != null) {
            counter.addAndGet(errors);
        }
    }

    /**
     * 设置总字节数
     */
    public void setTotalBytes(long bytes) {
        long bytesIn = bytes / 2;  // 假设入站和出站流量大致相等
        long bytesOut = bytes - bytesIn;
        setBytesIn(bytesIn);
        setBytesOut(bytesOut);
    }

    /**
     * 设置总错误数
     */
    public void setTotalErrors(long errors) {
        // 将错误数平均分配给各个协议
        long errorsPerProtocol = errors / PROTOCOL_MAP.size();
        for (String protocol : PROTOCOL_MAP.keySet()) {
            addProtocolErrors(protocol, errorsPerProtocol);
        }
    }
    
    /**
     * 新网络质量指标
     */
    public void updateNetworkQuality(float quality, float jitter, float rtt, float lossRate, long bandwidth) {
        setMetric(MetricNames.NETWORK_QUALITY, quality);
        setMetric(MetricNames.NETWORK_JITTER, jitter);
        setMetric(MetricNames.NETWORK_RTT, rtt);
        setMetric(MetricNames.NETWORK_LOSS_RATE, lossRate);
        setMetric(MetricNames.NETWORK_BANDWIDTH, bandwidth);
    }
    
    /**
     * 更新客户端指标
     */
    public void updateClientMetrics(int count, int active, int peak) {
        setMetric(MetricNames.CLIENT_COUNT, count);
        setMetric(MetricNames.CLIENT_ACTIVE, active);
        setMetric(MetricNames.CLIENT_PEAK, peak);
    }
    
    /**
     * 更新带宽指标
     */
    public void updateBandwidthMetrics(float usage, long limit, long peak) {
        setMetric(MetricNames.BANDWIDTH_USAGE, usage);
        setMetric(MetricNames.BANDWIDTH_LIMIT, limit);
        setMetric(MetricNames.BANDWIDTH_PEAK, peak);
    }
    
    /**
     * 批量更新连接数指标
     */
    public void updateConnectionMetrics(int rtmpConn, int rtspConn, int httpConn, int rtcConn) {
        validateConnectionCount(rtmpConn);
        validateConnectionCount(rtspConn);
        validateConnectionCount(httpConn);
        validateConnectionCount(rtcConn);
        
        setMetric(MetricNames.RTMP_CONNECTIONS, rtmpConn);
        setMetric(MetricNames.RTSP_CONNECTIONS, rtspConn);
        setMetric(MetricNames.HTTP_CONNECTIONS, httpConn);
        setMetric(MetricNames.RTC_CONNECTIONS, rtcConn);
    }
    
    /**
     * 批量更新流量指
     */
    public void updateBytesMetrics(long rtmpBytes, long rtspBytes, long httpBytes, long rtcBytes) {
        validateBytesCount(rtmpBytes);
        validateBytesCount(rtspBytes);
        validateBytesCount(httpBytes);
        validateBytesCount(rtcBytes);
        
        setMetric(MetricNames.RTMP_BYTES, rtmpBytes);
        setMetric(MetricNames.RTSP_BYTES, rtspBytes);
        setMetric(MetricNames.HTTP_BYTES, httpBytes);
        setMetric(MetricNames.RTC_BYTES, rtcBytes);
    }
    
    // 参数验证方法
    private void validateConnectionCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("连接数不能为负数: " + count);
        }
    }
    
    private void validateBytesCount(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("流量不能为负数: " + bytes);
        }
    }
    
    /**
     * 获取网络质量
     */
    public float getNetworkQuality() {
        return getMetric(MetricNames.NETWORK_QUALITY).floatValue();
    }
    
    /**
     * 获取网络抖动
     */
    public float getNetworkJitter() {
        return getMetric(MetricNames.NETWORK_JITTER).floatValue();
    }
    
    /**
     * 获取网络往返延迟时间(RTT)
     */
    public float getNetworkRTT() {
        return getMetric(MetricNames.NETWORK_RTT).floatValue();
    }
    
    /**
     * 获取包率
     */
    public float getPacketLossRate() {
        return getMetric(MetricNames.NETWORK_LOSS_RATE).floatValue();
    }
    
    /**
     * 获取带宽使用率
     */
    public float getBandwidthUsage() {
        return getMetric(MetricNames.BANDWIDTH_USAGE).floatValue();
    }
    
    /**
     * 获取带宽限制
     */
    public long getBandwidthLimit() {
        return getMetric(MetricNames.BANDWIDTH_LIMIT).longValue();
    }
    
    /**
     * 获取带宽峰值
     */
    public long getBandwidthPeak() {
        return getMetric(MetricNames.BANDWIDTH_PEAK).longValue();
    }
    
    /**
     * 获取特定协议的连接数
     * 
     * @param protocol 协议名称(如 "rtmp", "rtsp", "http")
     * @return 该协议的连接数
     */
    public int getProtocolConnections(String protocol) {
        return getMetric(protocol + ".connections").intValue();
    }
    
    /**
     * 获取特定协议的流量
     * 
     * @param protocol 协议名称(如 "rtmp", "rtsp", "http")
     * @return 该协议的流量
     */
    public long getProtocolBytes(String protocol) {
        return getMetric(protocol + ".bytes").longValue();
    }
    
    /**
     * 获取特定协议的错误数
     * 
     * @param protocol 协议名称(如 "rtmp", "rtsp", "http")
     * @return 该协议的误数
     */
    public long getProtocolErrors(String protocol) {
        return getMetric(protocol + ".errors").longValue();
    }
    
    /**
     * 获取特定协议的延迟
     * 
     * @param protocol 协议名称(如 "rtmp", "rtsp", "http")
     * @return 该协议的延迟
     */
    public long getProtocolLatency(String protocol) {
        return getMetric(protocol + ".latency").longValue();
    }
    
    /**
     * 获取服务端口
     */
    public short getHttpPort() {
        return httpPort;
    }
    
    /**
     * 设置服务端口
     */
    public void setHttpPort(short httpPort) {
        this.httpPort = httpPort;
    }
    
    /**
     * 获取服务端口
     */
    public short getRtspPort() {
        return rtspPort;
    }
    
    /**
     * 设置服务端口
     */
    public void setRtspPort(short rtspPort) {
        this.rtspPort = rtspPort;
    }
    
    /**
     * 获取服务端口
     */
    public short getRtmpPort() {
        return rtmpPort;
    }
    
    /**
     * 设置服务端口
     */
    public void setRtmpPort(short rtmpPort) {
        this.rtmpPort = rtmpPort;
    }
    
    /**
     * 获取服务端口
     */
    public short getRtcPort() {
        return rtcPort;
    }
    
    /**
     * 设置服务端口
     */
    public void setRtcPort(short rtcPort) {
        this.rtcPort = rtcPort;
    }
    
    /**
     * 获取服务状态
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }
    
    /**
     * 设置服务状态
     */
    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }
    
    /**
     * 获取服务状态
     */
    public boolean isRtspEnabled() {
        return rtspEnabled;
    }
    
    /**
     * 设置服务状态
     */
    public void setRtspEnabled(boolean rtspEnabled) {
        this.rtspEnabled = rtspEnabled;
    }
    
    /**
     * 获取服务状态
     */
    public boolean isRtmpEnabled() {
        return rtmpEnabled;
    }
    
    /**
     * 设置服务状态
     */
    public void setRtmpEnabled(boolean rtmpEnabled) {
        this.rtmpEnabled = rtmpEnabled;
    }
    
    /**
     * 获取服务状态
     */
    public boolean isRtcEnabled() {
        return rtcEnabled;
    }
    
    /**
     * 设置服务状态
     */
    public void setRtcEnabled(boolean rtcEnabled) {
        this.rtcEnabled = rtcEnabled;
    }

    public void setRtmpStreams(int count) {
        validateConnectionCount(count);
        setMetric(MetricNames.RTMP_STREAMS, count);
    }

    public void setRtspStreams(int count) {
        validateConnectionCount(count);
        setMetric(MetricNames.RTSP_STREAMS, count);
    }

    public void setHttpStreams(int count) {
        validateConnectionCount(count);
        setMetric(MetricNames.HTTP_STREAMS, count);
    }

    public void setRtcStreams(int count) {
        validateConnectionCount(count);
        setMetric(MetricNames.RTC_STREAMS, count);
    }

    public void setHttpBytes(long bytes) {
        validateBytesCount(bytes);
        setMetric(MetricNames.HTTP_BYTES, bytes);
    }

    public void setTcpConnections(int connections) {
        validateConnectionCount(connections);
        setMetric(MetricNames.TCP_CONNECTIONS, connections);
    }
    
    /**
     * 更新视频指标
     * 
     * @param width 视频宽度
     * @param height 视频高度
     * @param fps 视频帧率
     */
    public void updateVideoMetrics(int width, int height, int fps) {
        setMetric(MetricNames.VIDEO_WIDTH, width);
        setMetric(MetricNames.VIDEO_HEIGHT, height);
        setMetric(MetricNames.VIDEO_FPS, fps);
    }
    
    /**
     * 获取视频宽度
     */
    public int getVideoWidth() {
        return getMetric(MetricNames.VIDEO_WIDTH).intValue();
    }
    
    /**
     * 获取视频高度
     */
    public int getVideoHeight() {
        return getMetric(MetricNames.VIDEO_HEIGHT).intValue();
    }
    
    /**
     * 获取视频帧率
     */
    public int getVideoFps() {
        return getMetric(MetricNames.VIDEO_FPS).intValue();
    }
    
    /**
     * 设置流媒体基本信息
     * 
     * @param schema 协议类型
     * @param app 应用名
     * @param stream 流ID
     * @param originType 流来源类型
     * @param aliveSecond 存活时间(秒)
     */
    public void setStreamInfo(String schema, String app, String stream, int originType, long aliveSecond) {
        setMetric(MetricNames.STREAM_ORIGIN_TYPE, originType);
        setMetric(MetricNames.STREAM_ALIVE_SECONDS, aliveSecond);
        // 可以添加更多流信息指标
    }
    
    /**
     * 设置流媒体统计信息
     * 
     * @param readerCount 当前读取者数量
     * @param totalReaderCount 总读取者数量
     * @param bytesSpeed 流量速度(节/秒)
     */
    public void setStreamStats(int readerCount, int totalReaderCount, int bytesSpeed) {
        setMetric(MetricNames.STREAM_READER_COUNT, readerCount);
        setMetric(MetricNames.STREAM_TOTAL_READER_COUNT, totalReaderCount);
        setMetric(MetricNames.STREAM_BYTES_SPEED, bytesSpeed);
    }
    
    /**
     * 获取流来源类型
     */
    public int getStreamOriginType() {
        return getMetric(MetricNames.STREAM_ORIGIN_TYPE).intValue();
    }
    
    /**
     * 获取流存活时间(秒)
     */
    public long getStreamAliveSeconds() {
        return getMetric(MetricNames.STREAM_ALIVE_SECONDS).longValue();
    }
    
    /**
     * 获取当前读取者数量
     */
    public int getStreamReaderCount() {
        return getMetric(MetricNames.STREAM_READER_COUNT).intValue();
    }
    
    /**
     * 获取总读取者数量
     */
    public int getStreamTotalReaderCount() {
        return getMetric(MetricNames.STREAM_TOTAL_READER_COUNT).intValue();
    }
    
    /**
     * 获取流量速度(字节/秒)
     */
    public int getStreamBytesSpeed() {
        return getMetric(MetricNames.STREAM_BYTES_SPEED).intValue();
    }
} 