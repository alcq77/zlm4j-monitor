package com.aizuda.monitor.collector;

import com.aizuda.monitor.config.MonitorConfig;
import com.aizuda.monitor.metrics.enums.MetricsType;
import com.aizuda.monitor.metrics.SystemMetrics;
import com.aizuda.zlm4j.core.ZLMApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

import com.aizuda.monitor.util.SystemInfoUtil;
import com.aizuda.zlm4j.callback.IMKGetStatisticCallBack;
import com.aizuda.zlm4j.structure.MK_INI;
import com.sun.jna.Pointer;

/**
 * 系统指标收集器
 * 负责收集CPU、内存、磁盘等系统指标
 */
public class SystemMetricsCollector extends AbstractMetricsCollector<SystemMetrics> {
    private static final Logger log = LoggerFactory.getLogger(SystemMetricsCollector.class);

    private final ZLMApi zlmApi;

    public SystemMetricsCollector(ZLMApi zlmApi, MonitorConfig config) {
        super(config);
        this.zlmApi = zlmApi;
    }

    @Override
    public String getName() {
        return "system";
    }

    @Override
    public MetricsType getType() {
        return MetricsType.SYSTEM;
    }

    @Override
    protected SystemMetrics createMetrics() {
        return new SystemMetrics();
    }

    @Override
    protected void doCollect(SystemMetrics metrics) throws Exception {
        try {
            // 只获取需要的配置
            MonitorConfig config = getConfig();
            if (!config.getMetrics().getSystem().isEnabled()) {
                log.debug("系统指标收集已禁用");
                return;
            }
            // 1. 收集 ZLM 进程指标
                collectZLMProcessMetrics(metrics);

                // 2. 收集 JVM 运行时指标
                collectJvmMetrics(metrics);

                // 3. 收集操作系统指标
                collectOSMetrics(metrics);

        } catch (Exception e) {
            log.error("收集系统指标失败", e);
            throw e;
        }
    }

    @Override
    protected boolean isCollectorEnabled(MonitorConfig config) {
        return config.getMetrics().getSystem().isEnabled();
    }

    @Override
    protected void onConfigChange(MonitorConfig newConfig) {
        // 只处理配置变更通知，不修改收集逻辑
        if (!newConfig.getMetrics().getSystem().isEnabled()) {
            log.info("系统指标收集已禁用");
        }
    }

    private void collectZLMProcessMetrics(SystemMetrics metrics) {
        // 获取进程信息
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        int pid = Integer.parseInt(name.split("@")[0]);
        long uptime = runtime.getUptime();

        metrics.setProcessId(pid);
        metrics.setProcessUptime(uptime);

        // 获取线程信息
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        metrics.setProcessThreadCount(threadCount);

        // 获取内存使用
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();

        long totalMemory = heap.getCommitted();
        long usedMemory = heap.getUsed();
        float memoryUsage = (float) usedMemory / totalMemory;

        metrics.setMemoryTotal(totalMemory);
        metrics.setMemoryUsed(usedMemory);
        metrics.setMemoryUsage(memoryUsage);

        // 获取 ZLM 统计信息
        zlmApi.mk_get_statistic(new IMKGetStatisticCallBack() {
            @Override
            public void invoke(Pointer user_data, MK_INI ini) {
                try {
                    // 获取 ZLM 的一些统计信息
                    String tcpSession = zlmApi.mk_ini_get_option(ini, "object.TcpSession");
                    String udpSession = zlmApi.mk_ini_get_option(ini, "object.UdpSession");

                    if (tcpSession != null) {
                        metrics.setTcpConnections(Integer.parseInt(tcpSession));
                    }
                    if (udpSession != null) {
                        metrics.setUdpConnections(Integer.parseInt(udpSession));
                    }
                } catch (Exception e) {
                    log.warn("获取 ZLM 统计信息失败", e);
                }
            }
        }, null, null);
    }

    private void collectJvmMetrics(SystemMetrics metrics) {
        // JVM 内存指标
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();

        metrics.setJvmHeapInit(heap.getInit());
        metrics.setJvmHeapUsed(heap.getUsed());
        metrics.setJvmHeapCommitted(heap.getCommitted());
        metrics.setJvmHeapMax(heap.getMax());

        // JVM 线程指标
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        metrics.setJvmThreadCount(threadBean.getThreadCount());
        metrics.setJvmThreadPeakCount(threadBean.getPeakThreadCount());
        metrics.setJvmDaemonThreadCount(threadBean.getDaemonThreadCount());
    }

    private void collectOSMetrics(SystemMetrics metrics) {
        // 使用 SystemInfoUtil 获取系统指标
        float cpuUsage = SystemInfoUtil.getCpuUsage();
        long[] memInfo = SystemInfoUtil.getMemoryInfo();
        long[] diskIO = SystemInfoUtil.getDiskIOStats();
        long[] netStats = SystemInfoUtil.getNetworkStats();

        // CPU 指标
        metrics.setCpuCores(Runtime.getRuntime().availableProcessors());
        metrics.setCpuUsage(cpuUsage);

        // 内存指标
        metrics.setSystemMemoryTotal(memInfo[0]);
        metrics.setSystemMemoryUsed(memInfo[1]);
        metrics.setSystemMemoryFree(memInfo[2]);

        // 磁盘 IO 指标
        metrics.setDiskReadBytes(diskIO[0]);
        metrics.setDiskWriteBytes(diskIO[1]);
        metrics.setDiskReadIops(diskIO[2]);
        metrics.setDiskWriteIops(diskIO[3]);

        // 网络接口指标
        metrics.setNetRxBytes(netStats[0]);
        metrics.setNetTxBytes(netStats[1]);
        metrics.setNetRxPackets(netStats[2]);
        metrics.setNetTxPackets(netStats[3]);
        metrics.setNetRxErrors(netStats[4]);
        metrics.setNetTxErrors(netStats[5]);
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
            log.error("关闭系统指标收集器失败", e);
        }
    }

    @Override
    protected void doInit() throws Exception {
        if (zlmApi == null) {
            throw new IllegalStateException("ZLM API未初始化");
        }
        log.info("系统指标收集器初始化完成");
    }

    @Override
    protected void doStart() throws Exception {
        log.info("系统指标收集器已启动");
    }

    @Override
    protected void doStop() throws Exception {
        log.info("系统指标收集器已停止");
    }

    @Override
    protected void doDestroy() throws Exception {
        log.info("系统指标收集器已销毁");
    }
}