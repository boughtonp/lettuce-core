/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mark Paluch
 * @since 5.1
 */
public class AsyncPoolImpl<T> extends BasePool implements AsyncPool<T> {

    private static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);

    private final AsyncObjectFactory<T> factory;

    private final Queue<T> cache;
    private final Queue<T> all;

    private final AtomicInteger objectCount = new AtomicInteger();
    private final AtomicInteger objectsInCreationCount = new AtomicInteger();
    private final AtomicInteger idleCount = new AtomicInteger();

    private volatile State state = State.ACTIVE;

    public AsyncPoolImpl(PoolConfig poolConfig, AsyncObjectFactory<T> factory) {

        super(poolConfig);
        this.factory = factory;

        this.cache = new ConcurrentLinkedQueue<>();
        this.all = new ConcurrentLinkedQueue<>();

        createIdle();
    }

    private void createIdle() {

        int potentialIdle = getMinIdle() - getIdle();
        if (potentialIdle <= 0) {
            return;
        }

        int totalLimit = getAvailableCapacity();
        int toCreate = Math.min(Math.max(0, totalLimit), potentialIdle);

        for (int i = 0; i < toCreate; i++) {

            if (getAvailableCapacity() <= 0) {
                break;
            }

            CompletableFuture<T> future = new CompletableFuture<>();
            makeObject0(future);

            future.thenAccept(it -> {

                idleCount.incrementAndGet();
                cache.add(it);
            });
        }
    }

    private int getAvailableCapacity() {
        return getMaxTotal() - (getCreationInProgress() + getObjectCount());
    }

    @Override
    public CompletableFuture<T> acquire() {

        T object = cache.poll();

        CompletableFuture<T> res = new CompletableFuture<>();
        acquire0(object, res);

        return res;
    }

    private void acquire0(T object, CompletableFuture<T> res) {

        if (object != null) {

            idleCount.decrementAndGet();

            if (isTestOnAcquire()) {

                factory.validateObject(object).whenComplete((state, throwable) -> {

                    if (state != null && state) {
                        res.complete(object);
                        return;
                    }

                    destroy0(object);
                    makeObject0(res);
                });

                return;
            }

            res.complete(object);

            createIdle();
            return;
        }

        long objects = getObjectCount() + getCreationInProgress();

        if ((long) getMaxTotal() >= (objects + 1)) {
            makeObject0(res);
            return;
        }

        res.completeExceptionally(new NoSuchElementException("Pool exhausted"));
    }

    private void makeObject0(CompletableFuture<T> res) {

        objectsInCreationCount.incrementAndGet();

        factory.makeObject().whenComplete(
                (o, nested) -> {

                    if (nested != null) {
                        objectsInCreationCount.decrementAndGet();
                        res.completeExceptionally(new IllegalStateException("Cannot allocate object", nested));
                        return;
                    }

                    if (isTestOnCreate()) {

                        factory.validateObject(o).whenComplete(
                                (state, throwable) -> {

                                    try {

                                        if (state != null && state) {

                                            objectCount.incrementAndGet();
                                            all.add(o);

                                            res.complete(o);
                                            return;
                                        }

                                        factory.destroyObject(o).whenComplete(
                                                (v, t) -> res.completeExceptionally(new IllegalStateException(
                                                        "Cannot allocate object: Validation failed", throwable)));
                                    } catch (Exception e) {
                                        factory.destroyObject(o).whenComplete(
                                                (v, t) -> res.completeExceptionally(new IllegalStateException(
                                                        "Cannot allocate object: Validation failed", throwable)));
                                    } finally {
                                        objectsInCreationCount.decrementAndGet();
                                    }
                                });

                        return;
                    }

                    try {

                        objectCount.incrementAndGet();
                        all.add(o);

                        res.complete(o);
                    } catch (Exception e) {

                        objectCount.decrementAndGet();
                        all.remove(o);

                        factory.destroyObject(o).whenComplete((v, t) -> res.completeExceptionally(e));
                    } finally {
                        objectsInCreationCount.decrementAndGet();
                    }
                });
    }

    @Override
    public CompletableFuture<Void> release(T object) {

        if (!all.contains(object)) {
            return failed(new IllegalStateException("Returned object not currently part of this pool"));
        }

        if (idleCount.get() >= getMaxIdle()) {
            return destroy0(object);
        }

        if (isTestOnRelease()) {

            CompletableFuture<Boolean> valid = factory.validateObject(object);
            CompletableFuture<Void> res = new CompletableFuture<>();

            valid.whenComplete((state1, throwable) -> {

                if (state1 != null && state1) {
                    return0(object).whenComplete((x, y) -> res.complete(null));
                } else {
                    destroy0(object).whenComplete((x, y) -> res.complete(null));
                }
            });

            return res;
        }

        return0(object);

        return COMPLETED;
    }

    private CompletableFuture<Void> return0(T object) {

        int idleCount = this.idleCount.incrementAndGet();

        if (idleCount > getMaxIdle()) {

            this.idleCount.decrementAndGet();
            return destroy0(object);
        }

        cache.add(object);

        return COMPLETED;
    }

    private CompletableFuture<Void> destroy0(T object) {

        objectCount.decrementAndGet();
        all.remove(object);
        return factory.destroyObject(object);
    }

    @Override
    public void close() {

        if (state == State.SHUTDOWN) {
            return;
        }

        state = State.SHUTDOWN;

        T cached;

        List<CompletableFuture<Void>> futures = new ArrayList<>(all.size());

        while ((cached = all.poll()) != null) {
            futures.add(factory.destroyObject(cached));
        }

        CompletableFuture[] completableFutures = futures.toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(completableFutures).join();
    }

    static <T> CompletableFuture<T> failed(Throwable t) {

        CompletableFuture<T> res = new CompletableFuture<>();
        res.completeExceptionally(t);
        return res;
    }

    public int getIdle() {
        return idleCount.get();
    }

    public int getObjectCount() {
        return objectCount.get();
    }

    public int getCreationInProgress() {
        return objectsInCreationCount.get();
    }

    enum State {
        ACTIVE, SHUTDOWN;
    }
}
