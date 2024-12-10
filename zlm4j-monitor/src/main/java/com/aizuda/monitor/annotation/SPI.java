package com.aizuda.monitor.annotation;

import java.lang.annotation.*;

/**
 * SPI注解
 * 用于标记可扩展的接口实现类
 * 
 * @author Cursor
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
    
    /**
     * 默认实现名称
     */
    String value() default "";
} 