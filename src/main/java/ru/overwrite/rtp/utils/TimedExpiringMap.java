package ru.overwrite.rtp.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Expiry;

import java.util.concurrent.TimeUnit;

public class TimedExpiringMap<K, V> {

    private final Cache<K, ExpiringValue<V>> cache;
    private final TimeUnit unit;

    public TimedExpiringMap(TimeUnit unit) {
        this.unit = unit;
        this.cache = CaffeineFactory.newBuilder()
                .expireAfter(new Expiry<K, ExpiringValue<V>>() {
                    @Override
                    public long expireAfterCreate(K key, ExpiringValue<V> value, long currentTime) {
                        return value.expiryDuration();
                    }

                    @Override
                    public long expireAfterUpdate(K key, ExpiringValue<V> value, long currentTime, long currentDuration) {
                        return value.expiryDuration();
                    }

                    @Override
                    public long expireAfterRead(K key, ExpiringValue<V> value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    public void put(K key, V value, long duration) {
        long expiryDuration = unit.toNanos(duration);
        this.cache.put(key, new ExpiringValue<>(value, expiryDuration));
    }

    public V get(K key) {
        ExpiringValue<V> expiringValue = this.cache.getIfPresent(key);
        return expiringValue == null ? null : expiringValue.value();
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

    private record ExpiringValue<V>(V value, long expiryDuration) {
    }
}


