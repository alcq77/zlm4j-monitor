package com.aizuda.monitor.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 对象池
 * 用于复用对象,减少GC
 */
public class ObjectPool<T> {
    private final ArrayBlockingQueue<T> pool;
    private final Supplier<T> factory;
    private final Consumer<T> reset;
    
    public ObjectPool(Supplier<T> factory, Consumer<T> reset, int size) {
        this.factory = factory;
        this.reset = reset;
        this.pool = new ArrayBlockingQueue<>(size);
        // 预创建对象
        for (int i = 0; i < size; i++) {
            pool.offer(factory.get());
        }
    }
    
    public T acquire() {
        T obj = pool.poll();
        return obj != null ? obj : factory.get();
    }
    
    public void release(T obj) {
        if (obj != null) {
            reset.accept(obj);
            pool.offer(obj);
        }
    }
} 