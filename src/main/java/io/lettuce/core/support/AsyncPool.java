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

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * @author Mark Paluch
 */
public interface AsyncPool<T> extends Closeable {

    /**
     * Acquire an object from this {@link AsyncPool}. The returned {@link CompletableFuture} is notified once the acquire is
     * successful and failed otherwise.
     *
     * <strong>Its important that an acquired is always released to the pool again.</strong>
     */
    CompletableFuture<T> acquire();

    /**
     * Release an object back to this {@link AsyncPool}. The returned {@link CompletableFuture} is notified once the release is
     * successful and failed otherwise. When failed the object will automatically disposed.
     */
    CompletableFuture<Void> release(T object);

    @Override
    void close();
}
