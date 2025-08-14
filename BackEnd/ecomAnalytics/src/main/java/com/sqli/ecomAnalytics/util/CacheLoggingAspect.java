package com.sqli.ecomAnalytics.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;


@Aspect
@Component
public class CacheLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(CacheLoggingAspect.class.getName());

    @Around("(@annotation(org.springframework.cache.annotation.Cacheable))")
    public Object logCacheOps(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Cacheable [{}] took {} ms", joinPoint.getSignature().getName(), duration);
        return result;
    }

    @Around("(@annotation(org.springframework.cache.annotation.CacheEvict))")
    public Object logCacheEvictOps(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.info("CacheEvict [{}] took {} ms", joinPoint.getSignature().getName(), duration);
        return result;
    }
}
