package com.example.orchestrator.service;

public interface RedisMutex {

  /**
   * Helps acquire a lock on a resource
   * @param key the key
   * @param lockType the type of lock that helps in making the key unique across operations
   * @param ttl the time after which the key expires
   * @return Returns true if the lock was acquired, false otherwise
   */
  boolean lock(String key, String lockType, long ttl);

  /**
   * Helps unlock the lock acquires on a resource
   * @param key the key
   * @param lockType the type of lock that helps in making the key unique across operations
   * @return Returns true if unlocking was successful, false otherwise
   */
  boolean releaseLock(String key, String lockType);

}
