package com.service.indianfrog.domain.game.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ConcurrencyControlService {
    /* 동시성 제어 클래스*/
    private static final String REDIS_KEY_PREFIX = "gameRequests:";

    private final RedisTemplate<String, Object> redisTemplate;

    public ConcurrencyControlService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquireLock(Long gameRoomId, String userId) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        double score = Instant.now().toEpochMilli();
        String key = REDIS_KEY_PREFIX + gameRoomId;

        // Add the request to the sorted set
        Boolean added = zSetOps.add(key, userId, score);

        if (Boolean.TRUE.equals(added)) {
            // Set an expiration time for the lock to prevent stale locks
            redisTemplate.expire(key, 5, TimeUnit.SECONDS);
        }

        return Boolean.TRUE.equals(added);
    }

    public void releaseLock(Long gameRoomId, String userId) {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        String key = REDIS_KEY_PREFIX + gameRoomId;
        zSetOps.remove(key, userId);
    }
}
