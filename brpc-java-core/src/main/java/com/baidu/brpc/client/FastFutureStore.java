/*
 * Copyright (c) 2018 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.brpc.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lock-free datastructure faster than #{@link java.util.concurrent.ConcurrentHashMap} in certain circumstance.
 * When the underlying array runs out of space, FastFutureStore will downgrade to #{@link ConcurrentHashMap}
 * <p>
 * Created by wanghongfei on 2018/11/19.
 */
public class FastFutureStore {
    private static final Logger LOG = LoggerFactory.getLogger(FastFutureStore.class);

    /**
     * Default capacity of the internal array
     */
    private static final int DEFAULT_ARRAY_CAP = 10000;

    private static volatile FastFutureStore singletonInstance;

    /**
     * Atomic array used to store #{@link RpcFuture}
     */
    private AtomicReferenceArray<RpcFuture> futArray;

    /**
     * A counter used to calculate array index
     */
    private AtomicLong slotCounter = new AtomicLong(0);

    /**
     * Capacity of the internal array
     */
    private int cap;

    /**
     * Downgrade to ConcurrentHashMap when array is exhausted
     */
    private Map<Long, RpcFuture> backupMap = new ConcurrentHashMap<Long, RpcFuture>();

    /**
     * The boundary between array-stored id and map-stored id.
     * Binary format: 0100 0000 0000 0000 ... ... 0000(64 bits).
     * <p>
     * Ids less than(01xx xxxx ... xxx) this value should be stored in array.
     * Ids greater than(01xx xxxx ... xxx) this value should be stored in map.
     */
    private static long COUNTER_VALUE_BOUNDARY = (long) 0x1 << 62;

    /**
     * @param cap capacity of the internal array
     */
    public FastFutureStore(int cap) {
        if (cap < 1) {
            cap = DEFAULT_ARRAY_CAP;
        }

        this.cap = cap;
        this.futArray = new AtomicReferenceArray<RpcFuture>(cap);
    }

    /**
     * Obtain singleton object
     *
     * @param cap Capacity of the internal array
     *
     * @return The singleton instance.
     */
    public static FastFutureStore getInstance(int cap) {
        if (null == singletonInstance) {
            synchronized(FastFutureStore.class) {
                if (null == singletonInstance) {
                    singletonInstance = new FastFutureStore(cap);
                }
            }
        }

        return singletonInstance;
    }

    /**
     * Add an object.
     *
     * @return Identifier of the added object
     */
    public long put(RpcFuture fut) {
        int loopCount = 0;

        // loop until finding an empty slot
        while (true) {
            long currentCounter = slotCounter.getAndIncrement();

            if (currentCounter >= COUNTER_VALUE_BOUNDARY) {
                slotCounter.getAndSet(0);
                continue;
            }

            int slot = mapSlot(currentCounter);

            // try to put object into current slot
            boolean success = futArray.compareAndSet(slot, null, fut);
            if (success) {
                fut.setLogId(currentCounter);
                return currentCounter;
            }

            // loopCount is bigger than capacity indicating FastFutureStore has ran out of space
            if (++loopCount >= cap) {
                LOG.debug("FutureStore exhausted, current=%d, store id in map", cap);
                return storeInMap(currentCounter, fut);
            }
        }
    }

    /**
     * Retrieve object identified by id
     *
     * @param id Identifier returned by #{@link #put(RpcFuture)}
     */
    public RpcFuture get(long id) {
        if (isStoredInMap(id)) {
            return getFromMap(id);
        }

        return getFromArray(id);
    }

    /**
     * Retrieve and remove object identified by id
     *
     * @param id Identifier returned by #{@link #put(RpcFuture)}
     *
     * @return null if nothing found
     */
    public RpcFuture getAndRemove(long id) {
        if (isStoredInMap(id)) {
            return getAndRemoveMap(id);
        }

        return getAndRemoveArray(id);
    }

    /**
     * Return the count of the objects.
     */
    public int size() {
        int sum = 0;
        for (int ix = 0; ix < cap; ++ix) {
            if (null != futArray.get(ix)) {
                ++sum;
            }
        }

        return sum + backupMap.size();
    }

    /**
     * Traverse and invoke #{@link StoreWalker} on every element.
     *
     * @param walker Define the action needed to be performed for elements
     */
    public void traverse(StoreWalker walker) {
        if (null == walker) {
            throw new NullPointerException("walker cannot be null");
        }

        // do not traverse if no elements inside
        if (!hasElements()) {
            return;
        }

        // traverse array
        for (int ix = 0; ix < cap; ++ix) {
            RpcFuture fut = futArray.get(ix);
            if (null == fut) {
                // skip empty slot
                continue;
            }

            boolean keep = walker.visitElement(fut);
            if (!keep) {
                // delete element
                futArray.set(ix, null);
                // invoke hook action
                walker.actionAfterDelete(fut);
            }
        }

        //  traverse map
        for (Map.Entry<Long, RpcFuture> pair : backupMap.entrySet()) {
            boolean keep = walker.visitElement(pair.getValue());
            if (!keep) {
                backupMap.remove(pair.getKey());
                walker.actionAfterDelete(pair.getValue());
            }
        }
    }

    private RpcFuture getAndRemoveMap(long id) {
        return backupMap.remove(id);
    }

    private RpcFuture getAndRemoveArray(long id) {
        int slot = mapSlot(id);
        if (!rangeCheck(slot)) {
            return null;
        }

        // get the old value
        RpcFuture prev = futArray.get(slot);
        // remove only when RpcFuture.logId is equal with current id
        if (null != prev && prev.getLogId() == id) {
            futArray.set(slot, null);
            return prev;
        }

        return null;

    }

    private RpcFuture getFromMap(long id) {
        return backupMap.get(id);
    }

    private RpcFuture getFromArray(long id) {
        int slot = mapSlot(id);
        if (!rangeCheck(slot)) {
            return null;
        }

        return futArray.get(slot);
    }

    private long storeInMap(long id, RpcFuture fut) {
        long mapId = markMapBit(id);
        backupMap.put(mapId, fut);

        return mapId;
    }

    private long markMapBit(long id) {
        return id | COUNTER_VALUE_BOUNDARY;
    }

    private boolean isStoredInMap(long id) {
        return id >= COUNTER_VALUE_BOUNDARY;
    }

    private boolean hasElements() {
        boolean arrayNotEmpty = false;
        for (int ix = 0; ix < cap; ++ix) {
            if (null != futArray.get(ix)) {
                arrayNotEmpty = true;
                break;
            }
        }

        boolean mapNotEmpty = !backupMap.isEmpty();

        return arrayNotEmpty || mapNotEmpty;
    }

    private int mapSlot(long id) {
        return (int) (id % cap);
    }

    private boolean rangeCheck(int index) {
        return index < cap && index >= 0;
    }

    public interface StoreWalker {
        /**
         * Action on a none-null element.
         *
         * @param fut The current element, may not be null
         *
         * @return Indicate whether this element should be deleted. False: delete, True: reserve
         */
        boolean visitElement(RpcFuture fut);

        /**
         * Action performed after a deletion of the element.
         *
         * @param fut The deleted object.
         */
        void actionAfterDelete(RpcFuture fut);
    }
}
