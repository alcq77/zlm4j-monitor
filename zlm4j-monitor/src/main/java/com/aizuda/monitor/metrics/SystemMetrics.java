package com.aizuda.monitor.metrics;

import com.aizuda.monitor.metrics.enums.MetricsType;

/**
 * 系统指标
 * 包含系统级别的各项指标数据
 */
public class SystemMetrics extends AbstractMetrics {

    public static class MetricNames {
        /** 进程指标 */
        public static final String PROCESS_ID = "process.id";
        public static final String PROCESS_UPTIME = "process.uptime";
        public static final String PROCESS_THREAD_COUNT = "process.thread.count";
        public static final String PROCESS_CPU_USAGE = "process.cpu.usage";
        public static final String PROCESS_MEMORY_USAGE = "process.memory.usage";
        
        /** 内存指标 */
        public static final String MEMORY_TOTAL = "memory.total";
        public static final String MEMORY_USED = "memory.used";
        public static final String MEMORY_FREE = "memory.free";
        public static final String MEMORY_USAGE = "memory.usage";
        
        /** 线程指标 */
        public static final String THREAD_COUNT = "thread.count";
        public static final String THREAD_PEAK = "thread.peak";
        public static final String THREAD_DAEMON = "thread.daemon";
        
        /** CPU指标 */
        public static final String CPU_CORES = "cpu.cores";
        public static final String CPU_USAGE = "cpu.usage";
        public static final String CPU_SYSTEM = "cpu.system";
        public static final String CPU_USER = "cpu.user";
        public static final String CPU_IDLE = "cpu.idle";
        public static final String CPU_IOWAIT = "cpu.iowait";
        
        /** 磁盘指标 */
        public static final String DISK_TOTAL = "disk.total";
        public static final String DISK_USED = "disk.used";
        public static final String DISK_FREE = "disk.free";
        public static final String DISK_USAGE = "disk.usage";
        public static final String DISK_READ_BYTES = "disk.read.bytes";
        public static final String DISK_WRITE_BYTES = "disk.write.bytes";
        public static final String DISK_READ_IOPS = "disk.read.iops";
        public static final String DISK_WRITE_IOPS = "disk.write.iops";
        
        /** 网络接口指标 */
        public static final String NET_RX_BYTES = "net.rx.bytes";
        public static final String NET_TX_BYTES = "net.tx.bytes";
        public static final String NET_RX_PACKETS = "net.rx.packets";
        public static final String NET_TX_PACKETS = "net.tx.packets";
        public static final String NET_RX_ERRORS = "net.rx.errors";
        public static final String NET_TX_ERRORS = "net.tx.errors";
        
        /** CPU指标 */
        public static final String SYSTEM_CPU_USAGE = "system.cpu.usage";
        
        /** JVM 内存指标 */
        public static final String JVM_HEAP_INIT = "jvm.heap.init";
        public static final String JVM_HEAP_USED = "jvm.heap.used";
        public static final String JVM_HEAP_COMMITTED = "jvm.heap.committed";
        public static final String JVM_HEAP_MAX = "jvm.heap.max";
        
        /** JVM 线程指标 */
        public static final String JVM_THREAD_COUNT = "jvm.thread.count";
        public static final String JVM_THREAD_PEAK = "jvm.thread.peak";
        public static final String JVM_THREAD_DAEMON = "jvm.thread.daemon";
        
        /** 系统内存指标 */
        public static final String SYSTEM_MEMORY_TOTAL = "system.memory.total";
        public static final String SYSTEM_MEMORY_USED = "system.memory.used";
        public static final String SYSTEM_MEMORY_FREE = "system.memory.free";
        
        /** ZLM 连接指标 */
        public static final String TCP_CONNECTIONS = "tcp.connections";
        public static final String UDP_CONNECTIONS = "udp.connections";
    }
    
    @Override
    public MetricsType getType() {
        return MetricsType.SYSTEM;
    }
    
    public void setProcessId(int pid) {
        setMetric(MetricNames.PROCESS_ID, pid);
    }
    
    public void setProcessUptime(long uptime) {
        setMetric(MetricNames.PROCESS_UPTIME, uptime);
    }
    
    public void setProcessThreadCount(int count) {
        setMetric(MetricNames.PROCESS_THREAD_COUNT, count);
    }
    
    public void setMemoryTotal(long total) {
        setMetric(MetricNames.MEMORY_TOTAL, total);
    }
    
    public void setMemoryUsed(long used) {
        setMetric(MetricNames.MEMORY_USED, used);
    }
    
    public void setMemoryFree(long free) {
        setMetric(MetricNames.MEMORY_FREE, free);
    }
    
    public void setMemoryUsage(float usage) {
        setMetric(MetricNames.MEMORY_USAGE, usage);
    }
    
    public void setThreadCount(int count) {
        setMetric(MetricNames.THREAD_COUNT, count);
    }
    
    public void setPeakThreadCount(int count) {
        setMetric(MetricNames.THREAD_PEAK, count);
    }
    
    public void setDaemonThreadCount(int count) {
        setMetric(MetricNames.THREAD_DAEMON, count);
    }
    
    private String osName;
    private String osVersion;
    private String osArch;
    
    public void setOsName(String name) {
        this.osName = name;
    }
    
    public String getOsName() {
        return osName;
    }
    
    public void setOsVersion(String version) {
        this.osVersion = version;
    }
    
    public String getOsVersion() {
        return osVersion;
    }
    
    public void setOsArch(String arch) {
        this.osArch = arch;
    }
    
    public String getOsArch() {
        return osArch;
    }
    
    // CPU 相关方法
    public void setCpuCores(int cores) {
        setMetric(MetricNames.CPU_CORES, cores);
    }

    public void setCpuUsage(float usage) {
        setMetric(MetricNames.CPU_USAGE, usage);
    }

    public void setProcessCpuUsage(float usage) {
        setMetric(MetricNames.PROCESS_CPU_USAGE, usage);
    }

    // 磁盘相关方法
    public void setDiskTotal(long total) {
        setMetric(MetricNames.DISK_TOTAL, total);
    }

    public void setDiskUsed(long used) {
        setMetric(MetricNames.DISK_USED, used);
    }

    public void setDiskFree(long free) {
        setMetric(MetricNames.DISK_FREE, free);
    }

    public void setDiskUsage(float usage) {
        setMetric(MetricNames.DISK_USAGE, usage);
    }

    // 网络相关方法
    public void setNetRxBytes(long bytes) {
        setMetric(MetricNames.NET_RX_BYTES, bytes);
    }

    public void setNetTxBytes(long bytes) {
        setMetric(MetricNames.NET_TX_BYTES, bytes);
    }

    public void setNetRxPackets(long packets) {
        setMetric(MetricNames.NET_RX_PACKETS, packets);
    }

    public void setNetTxPackets(long packets) {
        setMetric(MetricNames.NET_TX_PACKETS, packets);
    }

    public void setNetRxErrors(long errors) {
        setMetric(MetricNames.NET_RX_ERRORS, errors);
    }

    public void setNetTxErrors(long errors) {
        setMetric(MetricNames.NET_TX_ERRORS, errors);
    }
    
    public void setSystemCpuUsage(float usage) {
        setMetric(MetricNames.SYSTEM_CPU_USAGE, usage);
    }
    
    public void setDiskReadBytes(long bytes) {
        setMetric(MetricNames.DISK_READ_BYTES, bytes);
    }
    
    public void setDiskWriteBytes(long bytes) {
        setMetric(MetricNames.DISK_WRITE_BYTES, bytes);
    }
    
    public void setDiskReadIops(long iops) {
        setMetric(MetricNames.DISK_READ_IOPS, iops);
    }
    
    public void setDiskWriteIops(long iops) {
        setMetric(MetricNames.DISK_WRITE_IOPS, iops);
    }
    
    // JVM 内存相关方法
    public void setJvmHeapInit(long init) {
        setMetric(MetricNames.JVM_HEAP_INIT, init);
    }
    
    public void setJvmHeapUsed(long used) {
        setMetric(MetricNames.JVM_HEAP_USED, used);
    }
    
    public void setJvmHeapCommitted(long committed) {
        setMetric(MetricNames.JVM_HEAP_COMMITTED, committed);
    }
    
    public void setJvmHeapMax(long max) {
        setMetric(MetricNames.JVM_HEAP_MAX, max);
    }
    
    // JVM 线程相关方法
    public void setJvmThreadCount(int count) {
        setMetric(MetricNames.JVM_THREAD_COUNT, count);
    }
    
    public void setJvmThreadPeakCount(int count) {
        setMetric(MetricNames.JVM_THREAD_PEAK, count);
    }
    
    public void setJvmDaemonThreadCount(int count) {
        setMetric(MetricNames.JVM_THREAD_DAEMON, count);
    }
    
    // 系统内存相关方法
    public void setSystemMemoryTotal(long total) {
        setMetric(MetricNames.SYSTEM_MEMORY_TOTAL, total);
    }
    
    public void setSystemMemoryUsed(long used) {
        setMetric(MetricNames.SYSTEM_MEMORY_USED, used);
    }
    
    public void setSystemMemoryFree(long free) {
        setMetric(MetricNames.SYSTEM_MEMORY_FREE, free);
    }
    
    public void setTcpConnections(int count) {
        setMetric(MetricNames.TCP_CONNECTIONS, count);
    }
    
    public void setUdpConnections(int count) {
        setMetric(MetricNames.UDP_CONNECTIONS, count);
    }
} 