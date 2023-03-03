package xyz.csongyu.mdcwiththreadpools;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class MdcWithThreadPoolsApplication implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdcWithThreadPoolsApplication.class);

    private static final String TRACE_NUMBER = "traceNumber";

    public static void main(final String[] args) {
        SpringApplication.run(MdcWithThreadPoolsApplication.class, args);
    }

    @Override
    public void run(final String... args) throws InterruptedException {
        final String traceNumber = UUID.randomUUID().toString();
        LOGGER.info("traceNumber: " + traceNumber);
        MDC.put(TRACE_NUMBER, traceNumber);

        // ThreadPoolTaskExecutor
        final ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setTaskDecorator(runnable -> {
            final Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    MDC.setContextMap(copyOfContextMap);
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        threadPoolTaskExecutor.initialize();

        final CountDownLatch threadPoolTaskExecutorCountDownLatch = new CountDownLatch(2);
        threadPoolTaskExecutor.submit(() -> {
            threadPoolTaskExecutorCountDownLatch.countDown();
            LOGGER.info("hello ThreadPoolTaskExecutor#submit()");
        });
        threadPoolTaskExecutor.execute(() -> {
            threadPoolTaskExecutorCountDownLatch.countDown();
            LOGGER.info("hello ThreadPoolTaskExecutor#execute()");
        });
        threadPoolTaskExecutorCountDownLatch.await();
        threadPoolTaskExecutor.shutdown();

        // ThreadPoolExecutor
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(Integer.MAX_VALUE)) {
            @Override
            public void execute(final Runnable command) {
                final Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
                super.execute(() -> {
                    try {
                        MDC.setContextMap(copyOfContextMap);
                        command.run();
                    } finally {
                        MDC.clear();
                    }
                });
            }
        };

        final CountDownLatch threadPoolExecutorCountDownLatch = new CountDownLatch(2);
        threadPoolExecutor.submit(() -> {
            threadPoolExecutorCountDownLatch.countDown();
            LOGGER.info("hello ThreadPoolExecutor#submit()");
        });
        threadPoolExecutor.execute(() -> {
            threadPoolExecutorCountDownLatch.countDown();
            LOGGER.info("hello ThreadPoolExecutor#execute()");
        });
        threadPoolExecutorCountDownLatch.await();
        threadPoolExecutor.shutdown();
    }
}
