package com.example.orchestrator.service;

import com.example.orchestrator.service.RedisMutex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Component
public class RedisMutexImpl implements RedisMutex {

  @Autowired
  private final RedisTemplate<String, String> redisTemplate;

  public RedisMutexImpl(RedisTemplate<String, String> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean lock(String key, String lockType, long ttl) {
    try {
      return redisTemplate.opsForValue().setIfAbsent(key, lockType, Duration.ofSeconds(ttl));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public boolean releaseLock(String key, String lockType) {
    String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    DefaultRedisScript<Boolean> defaultRedisScript = new DefaultRedisScript<>();
    defaultRedisScript.setResultType(Boolean.class);
    defaultRedisScript.setScriptText(luaScript);
    Boolean result = redisTemplate.execute(defaultRedisScript,
            Arrays.asList(key), lockType);
    return result != null && result;
  }
}
