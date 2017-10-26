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

import static io.lettuce.core.support.AsyncPoolImpl.failed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.core.RedisException;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncPoolWithValidationTest {

    @Mock
    AsyncObjectFactory<String> factory;

    @Before
    public void before() throws Exception {
        when(factory.destroyObject(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    public void mockCreation() {

        AtomicInteger counter = new AtomicInteger();
        when(factory.makeObject()).then(invocation -> CompletableFuture.completedFuture("" + counter.incrementAndGet()));
    }

    @Test
    public void objectCreationShouldFail() {

        when(factory.makeObject()).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.create(), factory);

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(acquire).isCompletedExceptionally();
    }

    @Test
    public void shouldCreateObjectWithTestOnBorrowFailExceptionally() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnCreate().build(), factory);

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(acquire).isCompletedExceptionally();
    }

    @Test
    public void shouldCreateObjectWithTestOnBorrowSuccess() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnCreate().build(), factory);

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(acquire).isCompletedWithValue("1");
    }

    @Test
    public void shouldCreateObjectWithTestOnBorrowFailState() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(CompletableFuture.completedFuture(false));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnCreate().build(), factory);

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(acquire).isCompletedExceptionally();
    }

    @Test
    public void shouldCreateFailedObjectWithTestOnBorrowFail() {

        when(factory.makeObject()).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnCreate().build(), factory);

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(acquire).isCompletedExceptionally();
    }

    @Test
    public void shouldTestObjectOnBorrowSuccessfully() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnAcquire().build(), factory);

        pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(acquire).isCompletedWithValue("1");
    }

    @Test
    public void shouldTestObjectOnBorrowFailState() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnAcquire().build(), factory);

        pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(acquire).isCompletedWithValue("2");

        assertThat(pool.getIdle()).isEqualTo(0);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();
    }

    @Test
    public void shouldTestObjectOnBorrowFailExceptionally() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnAcquire().build(), factory);

        pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(acquire).isCompletedWithValue("2");

        assertThat(pool.getIdle()).isEqualTo(0);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();
    }

    @Test
    public void shouldTestObjectOnReturnSuccessfully() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnRelease().build(), factory);

        CompletableFuture<Void> release = pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(1);
        assertThat(pool.getCreationInProgress()).isZero();

        CompletableFuture<String> acquire = pool.acquire();

        assertThat(acquire).isCompletedWithValue("1");
    }

    @Test
    public void shouldTestObjectOnReturnFailState() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnRelease().build(), factory);

        CompletableFuture<Void> release = pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(release).isCompletedWithValue(null);
    }

    @Test
    public void shouldTestObjectOnReturnFailExceptionally() {

        mockCreation();
        when(factory.validateObject(any())).thenReturn(failed(new RedisException("foo")));

        AsyncPoolImpl<String> pool = new AsyncPoolImpl<>(PoolConfig.builder().testOnRelease().build(), factory);

        CompletableFuture<Void> release = pool.release(pool.acquire().join());

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();

        assertThat(release).isCompletedWithValue(null);
    }
}