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

/**
 * Configuration for asynchronous pooling. Instances can be created through a {@link #builder()}.
 * 
 * @author Mark Paluch
 * @since 5.1
 */
public class PoolConfig {

    /**
     * The default value for the {@code testOnCreate} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_CREATE = false;

    /**
     * The default value for the {@code testOnAcquire} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_ACQUIRE = false;

    /**
     * The default value for the {@code testOnRelease} configuration attribute.
     */
    public static final boolean DEFAULT_TEST_ON_RELEASE = false;

    /**
     * The default value for the {@code maxTotal} configuration attribute.
     */
    public static final int DEFAULT_MAX_TOTAL = 8;

    /**
     * The default value for the {@code maxIdle} configuration attribute.
     */
    public static final int DEFAULT_MAX_IDLE = 8;

    /**
     * The default value for the {@code minIdle} configuration attribute.
     */
    public static final int DEFAULT_MIN_IDLE = 0;

    private final boolean testOnCreate;
    private final boolean testOnAcquire;
    private final boolean testOnRelease;

    private final int maxTotal;
    private final int maxIdle;
    private final int minIdle;

    private PoolConfig(boolean testOnCreate, boolean testOnAcquire, boolean testOnRelease, int maxTotal, int maxIdle,
            int minIdle) {

        this.testOnCreate = testOnCreate;
        this.testOnAcquire = testOnAcquire;
        this.testOnRelease = testOnRelease;
        this.maxTotal = maxTotal;
        this.maxIdle = maxIdle;
        this.minIdle = minIdle;
    }

    /**
     * Create a new {@link Builder} for {@link PoolConfig}.
     * 
     * @return a new {@link Builder} for {@link PoolConfig}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static PoolConfig create() {
        return builder().build();
    }

    /**
     * Get the value for the {@code testOnCreate} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnCreate} for this configuration instance.
     */
    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    /**
     * Get the value for the {@code testOnAcquire} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnAcquire} for this configuration instance.
     */
    public boolean isTestOnAcquire() {
        return testOnAcquire;
    }

    /**
     * Get the value for the {@code testOnRelease} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code testOnRelease} for this configuration instance.
     */
    public boolean isTestOnRelease() {
        return testOnRelease;
    }

    /**
     * Get the value for the {@code maxTotal} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code maxTotal} for this configuration instance.
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Get the value for the {@code maxIdle} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code maxIdle} for this configuration instance.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Get the value for the {@code minIdle} configuration attribute for pools created with this configuration instance.
     *
     * @return the current setting of {@code minIdle} for this configuration instance.
     */
    public int getMinIdle() {
        return minIdle;
    }

    public static class Builder {

        private boolean testOnCreate = DEFAULT_TEST_ON_CREATE;
        private boolean testOnAcquire = DEFAULT_TEST_ON_ACQUIRE;
        private boolean testOnRelease = DEFAULT_TEST_ON_RELEASE;

        private int maxTotal = DEFAULT_MAX_TOTAL;
        private int maxIdle = DEFAULT_MAX_IDLE;
        private int minIdle = DEFAULT_MIN_IDLE;

        protected Builder() {
        }

        public Builder testOnCreate() {
            return testOnCreate(true);
        }

        public Builder testOnCreate(boolean testOnCreate) {

            this.testOnCreate = testOnCreate;
            return this;
        }

        public Builder testOnAcquire() {
            return testOnAcquire(true);
        }

        public Builder testOnAcquire(boolean testOnAcquire) {

            this.testOnAcquire = testOnAcquire;
            return this;
        }

        public Builder testOnRelease() {
            return testOnRelease(true);
        }

        public Builder testOnRelease(boolean testOnRelease) {

            this.testOnRelease = testOnRelease;
            return this;
        }

        public Builder maxIdle(int maxIdle) {

            this.maxIdle = maxIdle;
            return this;
        }

        public Builder minIdle(int minIdle) {

            this.minIdle = minIdle;
            return this;
        }

        public Builder maxTotal(int maxTotal) {

            this.maxTotal = maxTotal;
            return this;
        }

        public PoolConfig build() {

            return new PoolConfig(testOnCreate, testOnAcquire, testOnRelease, maxTotal, maxIdle, minIdle);
        }
    }
}
