package com.recargapay.wallet.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisTestController {

    private final StringRedisTemplate redisTemplate;

    public RedisTestController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> testRedis() {
        String key = "test-key";
        String value = "hello-from-redis";

        // Set
        redisTemplate.opsForValue().set(key, value);

        // Get
        String result = redisTemplate.opsForValue().get(key);

        if (value.equals(result)) {
            return ResponseEntity.ok("Redis is working! Value: " + result);
        } else {
            return ResponseEntity.status(500).body("Redis test failed");
        }
    }
}
