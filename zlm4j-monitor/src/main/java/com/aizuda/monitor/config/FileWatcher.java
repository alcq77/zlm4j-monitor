package com.aizuda.monitor.config;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件监视器
 * 用于监控配置文件变化
 */
public class FileWatcher implements AutoCloseable {
    private final Path path;
    private final List<FileChangeListener> listeners = new ArrayList<>();
    private volatile boolean running = false;
    private WatchService watchService;
    private static final Logger log = LoggerFactory.getLogger(FileWatcher.class);
    
    public FileWatcher(String filePath) {
        this.path = Paths.get(filePath);
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new RuntimeException("初始化文件监控服务失败", e);
        }
    }
    
    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }
    
    public void start() {
        if (running) {
            return;
        }
        synchronized (this) {
            if (running) {
                return;
            }
            running = true;
            
            Thread watchThread = new Thread(() -> {
                try {
                    Path parent = path.getParent();
                    if (parent != null) {
                        parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                        
                        while (running) {
                            WatchKey key = watchService.take();
                            for (WatchEvent<?> event : key.pollEvents()) {
                                Path changed = (Path) event.context();
                                if (changed.toString().equals(path.getFileName().toString())) {
                                    debounceNotify(new FileChangeEvent(FileChangeType.MODIFY));
                                }
                            }
                            if (!key.reset()) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("文件监控异常", e);
                }
            }, "FileWatcher-Thread");
            
            watchThread.setDaemon(true);
            watchThread.start();
        }
    }
    
    private final long DEBOUNCE_DELAY = 1000L;
    private volatile long lastNotifyTime = 0;
    
    private void debounceNotify(FileChangeEvent event) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNotifyTime > DEBOUNCE_DELAY) {
            notifyListeners(event);
            lastNotifyTime = currentTime;
        }
    }
    
    private void notifyListeners(FileChangeEvent event) {
        List<FileChangeListener> copyListeners = new ArrayList<>(listeners);
        for (FileChangeListener listener : copyListeners) {
            try {
                listener.onChange(event);
            } catch (Exception e) {
                log.error("通知监听器失败", e);
            }
        }
    }
    
    @Override
    public void close() throws Exception {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("关闭文件监控服务失败", e);
                throw e;
            }
        }
    }
} 