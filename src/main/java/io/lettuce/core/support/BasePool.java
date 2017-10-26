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
 * Basic implementation of a pool configured through {@link PoolConfig}.
 * 
 * @author Mark Paluch
 * @since 5.1
 */
public abstract class BasePool {

    private final boolean testOnCreate;
    private final boolean testOnAcquire;
    private final boolean testOnRelease;
    private final int maxTotal;
    private final int maxIdle;
    private final int minIdle;

    protected BasePool(PoolConfig poolConfig) {

        this.testOnCreate = poolConfig.isTestOnCreate();
        this.testOnAcquire = poolConfig.isTestOnAcquire();
        this.testOnRelease = poolConfig.isTestOnRelease();
        this.maxTotal = poolConfig.getMaxTotal();
        this.maxIdle = poolConfig.getMaxIdle();
        this.minIdle = poolConfig.getMinIdle();
    }

    /**
     * Returns whether objects created for the pool will be validated before being returned from the acquire method. Validation
     * is performed by the {@link AsyncObjectFactory#validateObject(Object)} method of the factory associated with the pool. If
     * the object fails to validate, then acquire will fail.
     *
     * @return {@literal true} if newly created objects are validated before being returned from the acquire method.
     */
    public boolean isTestOnCreate() {
        return testOnCreate;
    }

    /**
     * Returns whether objects acquired from the pool will be validated before being returned from the acquire method.
     * Validation is performed by the {@link AsyncObjectFactory#validateObject(Object)} method of the factory associated with
     * the pool. If the object fails to validate, it will be removed from the pool and destroyed, and a new attempt will be made
     * to borrow an object from the pool.
     *
     * @return {@literal true} if objects are validated before being returned from the acquire method.
     */
    public boolean isTestOnAcquire() {
        return testOnAcquire;
    }

    /**
     * Returns whether objects borrowed from the pool will be validated when they are returned to the pool via the release
     * method. Validation is performed by the {@link AsyncObjectFactory#validateObject(Object)} method of the factory associated
     * with the pool. Returning objects that fail validation are destroyed rather then being returned the pool.
     *
     * @return {@literal true} if objects are validated on return to the pool via the release method.
     */
    public boolean isTestOnRelease() {
        return testOnRelease;
    }

    /**
     * Returns the maximum number of objects that can be allocated by the pool (checked out to clients, or idle awaiting
     * checkout) at a given time. When negative, there is no limit to the number of objects that can be managed by the pool at
     * one time.
     *
     * @return the cap on the total number of object instances managed by the pool.
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool. If maxIdle is set too low on heavily loaded systems it is
     * possible you will see objects being destroyed and almost immediately new objects being created. This is a result of the
     * active threads momentarily returning objects faster than they are requesting them them, causing the number of idle
     * objects to rise above maxIdle. The best value for maxIdle for heavily loaded system will vary but the default is a good
     * starting point.
     *
     * @return the maximum number of "idle" instances that can be held in the pool.
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Returns the target for the minimum number of idle objects to maintain in the pool. If this is the case, an attempt is
     * made to ensure that the pool has the required minimum number of instances during idle object eviction runs.
     * <p>
     * If the configured value of minIdle is greater than the configured value for maxIdle then the value of maxIdle will be
     * used instead.
     *
     * @return The minimum number of objects.
     */
    public int getMinIdle() {
        int maxIdleSave = getMaxIdle();
        if (this.minIdle > maxIdleSave) {
            return maxIdleSave;
        } else {
            return minIdle;
        }
    }
}
