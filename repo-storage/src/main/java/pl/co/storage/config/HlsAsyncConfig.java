package pl.co.storage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class HlsAsyncConfig {

    @Bean("hlsExecutor")
    public TaskExecutor hlsExecutor(
            @Value("${storage.hls.executor.core-pool-size:2}") int corePoolSize,
            @Value("${storage.hls.executor.max-pool-size:4}") int maxPoolSize,
            @Value("${storage.hls.executor.queue-capacity:50}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("hls-worker-");
        executor.initialize();
        return executor;
    }
}
