package com.example.customermanagement.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // Dedicated worker pool for bulk imports, separate from normal web requests.
    @Bean(name = "bulkImportTaskExecutor")
    public Executor bulkImportTaskExecutor(@Value("${app.bulk-import.worker-count:2}") int workerCount,
                                           @Value("${app.bulk-import.queue-capacity:10}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("bulk-import-");
        executor.setCorePoolSize(workerCount);
        executor.setMaxPoolSize(workerCount);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }
}
