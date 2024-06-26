package com.toddwu.toj_judge.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    @Bean
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池创建的时候初始化的线程数
        executor.setCorePoolSize(10);
        // 最大线程数：线程池最大的线程数，只有缓冲队列满了之后才会申请超过核心线程数的线程
        executor.setMaxPoolSize(100);
        // 缓冲队列：用来缓冲执行任务的队列
        executor.setQueueCapacity(50);
        // 线程池关闭：等待所有任务都完成再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间：等待5秒后强制停止
        executor.setAwaitTerminationSeconds(5);
        // 允许空闲时间：超过核心线程之外的线程到达60秒后会被销毁
        executor.setKeepAliveSeconds(60);
        // 线程名称前缀
        executor.setThreadNamePrefix("judge-Async-");
        // 初始化线程
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
