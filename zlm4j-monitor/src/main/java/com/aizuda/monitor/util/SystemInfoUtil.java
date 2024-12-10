package com.aizuda.monitor.util;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinBase.MEMORYSTATUSEX;
import com.sun.jna.platform.win32.WinDef.DWORD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SystemInfoUtil {
    private static final Logger log = LoggerFactory.getLogger(SystemInfoUtil.class);
    
    /**
     * 获取系统 CPU 使用率
     */
    public static float getCpuUsage() {
        if (Platform.isWindows()) {
            return getWindowsCpuUsage();
        } else if (Platform.isLinux()) {
            return getLinuxCpuUsage();
        }
        return 0.0f;
    }
    
    /**
     * 获取系统内存使用情况
     */
    public static long[] getMemoryInfo() {
        if (Platform.isWindows()) {
            return getWindowsMemoryInfo();
        } else if (Platform.isLinux()) {
            return getLinuxMemoryInfo();
        }
        return new long[]{0L, 0L, 0L}; // total, used, free
    }
    
    /**
     * 获取磁盘IO统计
     */
    public static long[] getDiskIOStats() {
        if (Platform.isWindows()) {
            return getWindowsDiskIOStats();
        } else if (Platform.isLinux()) {
            return getLinuxDiskIOStats();
        }
        return new long[]{0L, 0L, 0L, 0L}; // read bytes, write bytes, read ops, write ops
    }
    
    /**
     * 获取网络接口统计
     */
    public static long[] getNetworkStats() {
        if (Platform.isWindows()) {
            return getWindowsNetworkStats();
        } else if (Platform.isLinux()) {
            return getLinuxNetworkStats();
        }
        return new long[]{0L, 0L, 0L, 0L, 0L, 0L}; // rx bytes, tx bytes, rx packets, tx packets, rx errors, tx errors
    }
    
    // Windows 相关方法
    private static float getWindowsCpuUsage() {
        try {
            Kernel32 kernel32 = Kernel32.INSTANCE;            
            // 获取系统 CPU 时间
            FILETIME idleTime = new FILETIME();
            FILETIME kernelTime = new FILETIME();
            FILETIME userTime = new FILETIME();
            kernel32.GetSystemTimes(idleTime, kernelTime, userTime);
            
            // 计算 CPU 使用率
            long idle = filetime2Long(idleTime);
            long kernel = filetime2Long(kernelTime);
            long user = filetime2Long(userTime);
            
            long totalCpu = kernel + user - idle;
            float cpuUsage = totalCpu * 100.0f / (kernel + user);
            
            return cpuUsage;
        } catch (Exception e) {
            log.warn("获取Windows CPU使用率失败", e);
            return 0.0f;
        }
    }
    
    private static long[] getWindowsMemoryInfo() {
        try {
            Kernel32 kernel32 = Kernel32.INSTANCE;
            MEMORYSTATUSEX status = new MEMORYSTATUSEX();
            status.dwLength = new DWORD(status.size());
            
            kernel32.GlobalMemoryStatusEx(status);
            
            long total = status.ullTotalPhys.longValue();
            long available = status.ullAvailPhys.longValue();
            long used = total - available;
            
            return new long[]{total, used, available};
        } catch (Exception e) {
            log.warn("获取Windows内存信息失败", e);
            return new long[]{0L, 0L, 0L};
        }
    }
    
    // Windows 磁盘IO统计
    private static long[] getWindowsDiskIOStats() {
        // TODO: 实现 Windows 磁盘IO统计
        // 目前返回默认值: read bytes, write bytes, read ops, write ops
        log.warn("Windows磁盘IO统计功能尚未实现");
        return new long[]{0L, 0L, 0L, 0L};
    }
    
    // Linux 相关方法
    private static float getLinuxCpuUsage() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String line = reader.readLine();
            if (line != null) {
                String[] fields = line.split("\\s+");
                if (fields.length >= 8) {
                    long user = Long.parseLong(fields[1]);
                    long nice = Long.parseLong(fields[2]);
                    long system = Long.parseLong(fields[3]);
                    long idle = Long.parseLong(fields[4]);
                    long iowait = Long.parseLong(fields[5]);
                    long irq = Long.parseLong(fields[6]);
                    long softirq = Long.parseLong(fields[7]);
                    
                    long totalCpu = user + nice + system + idle + iowait + irq + softirq;
                    float cpuUsage = (totalCpu - idle) * 100.0f / totalCpu;
                    
                    return cpuUsage;
                }
            }
            return 0.0f;
        } catch (IOException e) {
            log.warn("获取Linux CPU使用率失败", e);
            return 0.0f;
        }
    }
    
    private static long[] getLinuxMemoryInfo() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            long total = 0;
            long free = 0;
            long buffers = 0;
            long cached = 0;
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\s+");
                if (fields.length >= 2) {
                    long value = Long.parseLong(fields[1]) * 1024; // 转换为字节
                    switch (fields[0]) {
                        case "MemTotal:":
                            total = value;
                            break;
                        case "MemFree:":
                            free = value;
                            break;
                        case "Buffers:":
                            buffers = value;
                            break;
                        case "Cached:":
                            cached = value;
                            break;
                    }
                }
            }
            
            long available = free + buffers + cached;
            long used = total - available;
            
            return new long[]{total, used, available};
        } catch (IOException e) {
            log.warn("获取Linux内存信息失败", e);
            return new long[]{0L, 0L, 0L};
        }
    }
    
    // Linux 磁盘IO统计
    private static long[] getLinuxDiskIOStats() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/diskstats"))) {
            String line;
            long readBytes = 0;
            long writeBytes = 0;
            long readOps = 0;
            long writeOps = 0;
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.trim().split("\\s+");
                if (fields.length >= 14) {
                    readOps += Long.parseLong(fields[3]);
                    writeOps += Long.parseLong(fields[7]);
                    readBytes += Long.parseLong(fields[5]) * 512; // 扇区数 * 512字节
                    writeBytes += Long.parseLong(fields[9]) * 512;
                }
            }
            
            return new long[]{readBytes, writeBytes, readOps, writeOps};
        } catch (Exception e) {
            log.warn("获取Linux磁盘IO统计失败", e);
            return new long[]{0L, 0L, 0L, 0L};
        }
    }
    
    // Windows 网络统计
    private static long[] getWindowsNetworkStats() {
        // TODO: 实现 Windows 网络统计
        // 目前返回默认值: rx bytes, tx bytes, rx packets, tx packets, rx errors, tx errors
        log.warn("Windows网络统计功能尚未实现");
        return new long[]{0L, 0L, 0L, 0L, 0L, 0L};
    }
    
    // Linux 网络统计
    private static long[] getLinuxNetworkStats() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/net/dev"))) {
            String line;
            long rxBytes = 0;
            long txBytes = 0;
            long rxPackets = 0;
            long txPackets = 0;
            long rxErrors = 0;
            long txErrors = 0;
            
            // 跳过前两行头部
            reader.readLine();
            reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                String[] fields = line.trim().split(":\\s+");
                if (fields.length == 2) {
                    String[] stats = fields[1].trim().split("\\s+");
                    if (stats.length >= 16) {
                        rxBytes += Long.parseLong(stats[0]);
                        rxPackets += Long.parseLong(stats[1]);
                        rxErrors += Long.parseLong(stats[2]);
                        txBytes += Long.parseLong(stats[8]);
                        txPackets += Long.parseLong(stats[9]);
                        txErrors += Long.parseLong(stats[10]);
                    }
                }
            }
            
            return new long[]{rxBytes, txBytes, rxPackets, txPackets, rxErrors, txErrors};
        } catch (Exception e) {
            log.warn("获取Linux网络统计失败", e);
            return new long[]{0L, 0L, 0L, 0L, 0L, 0L};
        }
    }
    
    // 辅助方法：将 FILETIME 转换为 long
    private static long filetime2Long(FILETIME ft) {
        // 直接使用 int 值进行位运算
        return ((long)ft.dwHighDateTime << 32) | (ft.dwLowDateTime & 0xFFFFFFFFL);
    }

} 