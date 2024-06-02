package ru.overwrite.rtp.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * A simple expiring map implementation using Caffeine caches
 *
 * @param <K> key type
 * @param <V> value type
 */
public class ExpiringMap<K, V> {
    private final Cache<K, V> cache;

    public ExpiringMap(long duration, TimeUnit unit) {
        this.cache = Caffeine.newBuilder().expireAfterWrite(duration, unit).build();
    }

    public void put(K key, V value) {
        this.cache.put(key, value);
    }

    public V get(K key) {
        return this.cache.getIfPresent(key);
    }

    public boolean containsKey(K key) {
        return this.cache.getIfPresent(key) != null;
    }

    public void remove(K key) {
        this.cache.invalidate(key);
    }

    public long size() {
        return this.cache.estimatedSize();
    }

    public void clear() {
        this.cache.invalidateAll();
    }
}

