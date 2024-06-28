// Original - https://github.com/LuckPerms/LuckPerms/blob/master/common/src/main/java/me/lucko/luckperms/common/util/CaffeineFactory.java

package ru.overwrite.rtp.utils;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public final class CaffeineFactory {
    private CaffeineFactory() {
    }

    private static final ForkJoinPool loaderPool = new ForkJoinPool();

    public static Caffeine<Object, Object> newBuilder() {
        return Caffeine.newBuilder().executor(loaderPool);
    }

    public static Executor executor() {
        return loaderPool;
    }

}
