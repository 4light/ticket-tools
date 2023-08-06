package test.ticket.tickettools.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@Slf4j
public class TaskExecutorConfig {
    @Bean
    public ThreadPoolTaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(10);//核心线程数
        pool.setMaxPoolSize(10);//最大线程
        pool.setQueueCapacity(10);//线程队列
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());//拒绝策略
        pool.initialize();//线程初始化
        return pool;
    }
}
