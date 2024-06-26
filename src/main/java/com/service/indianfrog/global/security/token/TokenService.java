package com.service.indianfrog.global.security.token;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long REFRESH_TOKEN_EXPIRATION = 60 * 60 * 24;

    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void saveTokenInfo(String email, String refreshToken) {
        String refreshTokenKey = "refreshToken:" + email;
        setTokenWithExpiration(refreshTokenKey, refreshToken, REFRESH_TOKEN_EXPIRATION);
    }

    private void setTokenWithExpiration(String key, String token, long expiration) {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set(key, token, expiration, TimeUnit.SECONDS);
    }
}
