package org.example.trafficbilling.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private KafkaProducerService kafkaProducer;

    private static final int WINDOW_SIZE = 60; // 时间窗口，单位：秒
    @Value("${threshold-per-minute}")
    private int limit_per_minute;

    /**
     * 检查请求是否允许，并异步记录请求
     * @param userId 用户ID
     * @param apiId API ID
     * @return 是否允许请求
     */
    public boolean isAllowed(String userId, String apiId) {
        long currentTimeMillis = System.currentTimeMillis();
        String key = String.format("rate:%s:%s", userId, apiId);
        long windowStart = currentTimeMillis - WINDOW_SIZE * 1000;

        // 移除窗口外的请求
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // 当前窗口内请求数
        Long currentCount = redisTemplate.opsForZSet().zCard(key);
        if (currentCount != null && currentCount >= limit_per_minute) {
            return false;
        }

        // 记录当前请求
        redisTemplate.opsForZSet().add(key, String.valueOf(currentTimeMillis), currentTimeMillis);
        // 设置过期时间
        redisTemplate.expire(key, WINDOW_SIZE + 1, TimeUnit.SECONDS);

        kafkaProducer.sendLog(userId, apiId);

        return true;
    }

    /**
     * 异步记录请求日志
     * @param userId 用户ID
     * @param apiId API ID
     */
    /*@Async("trafficExecutor")
    public void logRequestAsync(String userId, String apiId) {
        // 可选：集成 Kafka 或其他流式处理工具记录日志
        // 例如，将日志发送到 Kafka 主题
        // kafkaTemplate.send("traffic-log", String.format("User: %s, API: %s, Timestamp: %d", userId, apiId, System.currentTimeMillis()));
    }*/

}
