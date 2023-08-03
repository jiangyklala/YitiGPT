package com.jxm.yitiGPT.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Set;

@Slf4j
@Service
public class ApiKeyService {

    private final String REDIS_YT_KEY = "yt:key";

    public String addKey(String key) {
        try (Jedis jedis = GPTService.jedisPool.getResource()) {
            jedis.sadd(REDIS_YT_KEY, key);
        } catch (RuntimeException e) {
            return "添加失败";
        }

        return "添加成功";

    }

    public String srandKey() {
        String key = null;

        try (Jedis jedis = GPTService.jedisPool.getResource()) {
            key = jedis.srandmember(REDIS_YT_KEY);
        }

        return key;
    }

    public String allKeys() {
        Set<String> allKeys = null;
        try (Jedis jedis = GPTService.jedisPool.getResource()) {
            allKeys = jedis.smembers(REDIS_YT_KEY);
        }

        if (allKeys == null) {
            return "没有 KEY 呦";
        }

        return allKeys.toString();
    }

    public String delAKey(String key) {
        long res = 0;
        try (Jedis jedis = GPTService.jedisPool.getResource()) {
            res = jedis.srem(REDIS_YT_KEY, key);
        }

        return "删除 " + res + " 个 Key : " + key;
    }
}
