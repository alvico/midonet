/*
 * Copyright 2012 Midokura Europe SARL
 */

package org.midonet.sdn.state;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.core.Clock;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FlowStateTableTest {
    static final Duration IDLE_EXPIRATION = new FiniteDuration(60, TimeUnit.SECONDS);

    static class TestKey implements IdleExpiration  {
        private final String key;

        public TestKey(String key) {
            this.key = key;
        }

        @Override
        public Duration expiresAfter() {
            return IDLE_EXPIRATION;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestKey testKey = (TestKey) o;
            return key.equals(testKey.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    private static TestKey key(String k) {
        return new TestKey(k);
    }

    private ShardedFlowStateTable<TestKey, Integer> global;
    private List<FlowStateLifecycle<TestKey, Integer>> shards = new ArrayList<>();

    private final int SHARDS = 4;

    private final TestKey[] keys =  { key("A"), key("B"), key("C"),
                                      key("D"), key("E"), key("F") };
    private final Integer[] vals = {100, 200, 300, 400, 500, 600};
    private final MockClock clock = new MockClock();


    @Before
    @SuppressWarnings("unchecked")
    public void before() {
        global = new ShardedFlowStateTable<>(clock);
        for (int i = 0; i < SHARDS; i++)
            shards.add((FlowStateLifecycle) global.addShard());
    }

    @Test
    public void testSetGetSingleShard() {
        FlowStateTable<TestKey, Integer> shard = shards.get(0);
        assertThat(shard, notNullValue());

        for (int i = 0; i < keys.length; i++) {
            assertThat(shard.get(keys[i]), nullValue());
            shard.putAndRef(keys[i], vals[i]);
            assertThat(shard.get(keys[i]), equalTo(vals[i]));
        }

        for (int i = 0; i < keys.length; i++)
            assertThat(shard.get(keys[i]), equalTo(vals[i]));

        shard.putAndRef(keys[0], 9595);
        assertThat(shard.get(keys[0]), equalTo(9595));
    }

    @Test
    public void testSetGetMultiShard() {
        for (int i = 0; i < keys.length; i++) {
            assertThat(global.get(keys[i]), nullValue());
            shards.get(i%SHARDS).putAndRef(keys[i], vals[i]);
        }

        for (int i = 0; i < keys.length; i++) {
            for (int shard = 0; shard < SHARDS; shard++) {
                assertThat(shards.get(shard).get(keys[i]), equalTo(vals[i]));
            }
            assertThat(global.get(keys[i]), equalTo(vals[i]));
        }

        shards.get(0).putAndRef(keys[0], 9595);
        for (int shard = 0; shard < SHARDS; shard++) {
            assertThat(shards.get(shard).get(keys[0]), equalTo(9595));
        }
    }

    @Test
    public void testTransactionSetGet() {
        FlowStateTable<TestKey, Integer> shard = shards.get(0);
        FlowStateTransaction<TestKey, Integer> tx =
            new FlowStateTransaction<>(shards.get(0));

        tx.putAndRef(key("foo"), 1);
        assertThat(tx.get(key("foo")), equalTo(1));
        assertThat(shard.get(key("foo")), nullValue());

        tx.putAndRef(key("foo"), 2);
        assertThat(tx.get(key("foo")), equalTo(2));
        assertThat(shard.get(key("foo")), nullValue());

        tx.putAndRef(key("bar"), 1);
        assertThat(tx.get(key("bar")), equalTo(1));
        assertThat(shard.get(key("bar")), nullValue());
    }

    @Test
    public void testTransactionFlush() {
        FlowStateTable<TestKey, Integer> shard = shards.get(0);
        FlowStateTransaction<TestKey, Integer> tx =
                new FlowStateTransaction<>(shards.get(0));

        tx.putAndRef(key("foo"), 1);
        tx.putAndRef(key("bar"), 2);

        tx.flush();

        assertThat(tx.get(key("foo")), nullValue());
        assertThat(tx.get(key("bar")), nullValue());
        assertThat(shard.get(key("foo")), nullValue());
        assertThat(shard.get(key("bar")), nullValue());
    }

    @Test
    public void testTransactionCommit() {
        FlowStateTable<TestKey, Integer> shard = shards.get(0);
        FlowStateTransaction<TestKey, Integer> tx =
                new FlowStateTransaction<>(shards.get(0));
        tx.putAndRef(key("foo"), 1);
        tx.putAndRef(key("bar"), 2);

        tx.commit();

        assertThat(shard.get(key("foo")), equalTo(1));
        assertThat(shard.get(key("bar")), equalTo(2));
    }

    @Test
    public void testTxRefCount() {
        FlowStateLifecycle<TestKey, Integer> shard = shards.get(0);
        FlowStateTransaction<TestKey, Integer> tx =
                new FlowStateTransaction<>(shards.get(0));

        shard.putAndRef(key("foo"), 1);
        shard.putAndRef(key("bar"), 1);
        shard.unref(key("foo"));
        shard.unref(key("bar"));

        clock.time = IDLE_EXPIRATION.toNanos() + 1;

        tx.ref(key("foo"));
        tx.commit();

        shard.expireIdleEntries();

        assertThat(shard.get(key("foo")), equalTo(1));
        assertThat(shard.get(key("bar")), nullValue());
    }

    private void foldTest(FlowStateTable<TestKey, Integer> cs) {
        Set<TestKey> txKeys = cs.fold(new HashSet<TestKey>(), new KeyReducer());
        Set<TestKey> expectedKeys = new HashSet<>();
        expectedKeys.addAll(Arrays.asList(keys));
        assertThat(txKeys, equalTo(expectedKeys));

        Set<Integer> txVals = cs.fold(new HashSet<Integer>(), new ValueReducer());
        Set<Integer> expectedVals = new HashSet<>();
        expectedVals.addAll(Arrays.asList(vals));
        assertThat(txVals, equalTo(expectedVals));
    }

    @Test
    public void testTransactionFold() {
        FlowStateTable<TestKey, Integer> tx = new FlowStateTransaction<>(shards.get(0));
        for (int i = 0; i < keys.length; i++)
            tx.putAndRef(keys[i], vals[i]);
        foldTest(tx);
    }

    @Test
    public void testSingleShardFold() {
        for (int i = 0; i < keys.length; i++)
            shards.get(0).putAndRef(keys[i], vals[i]);
        foldTest(shards.get(0));
    }

    @Test
    public void testShardFold() {
        for (int i = 0; i < keys.length; i++)
            shards.get(0).putAndRef(keys[i], vals[i]);
        foldTest(global);
    }


    class KeyReducer implements FlowStateTable.Reducer<TestKey, Integer, Set<TestKey>> {
        @Override
        public Set<TestKey> apply(Set<TestKey> seed, TestKey key, Integer value) {
            seed.add(key);
            return seed;
        }
    }

    class ValueReducer implements FlowStateTable.Reducer<TestKey, Integer, Set<Integer>> {
        @Override
        public Set<Integer> apply(Set<Integer> seed, TestKey key, Integer value) {
            seed.add(value);
            return seed;
        }
    }

    class MockClock extends Clock {
        long time = 0;

        @Override
        public long tick() {
            return time;
        }
    }

    @Test
    public void testRefCountSingleShard() {
        for (int i = 0; i < keys.length; i++) {
            shards.get(0).putAndRef(keys[i], vals[i]);
        }
        refCountTest(shards.get(0));
    }

    @Test
    public void testRefCountMultiShard() {
        for (int i = 0; i < keys.length; i++) {
            shards.get(i % shards.size()).putAndRef(keys[i], vals[i]);
        }
        refCountTest(global);
    }

    private void refCountTest(FlowStateLifecycle<TestKey, Integer> cs) {
        for (TestKey key : keys) {
            cs.unref(key);
            cs.ref(key);
        }

        clock.time = IDLE_EXPIRATION.toNanos() * 2;
        cs.expireIdleEntries();

        for (int i = 0; i < keys.length; i++)
            assertThat(cs.get(keys[i]), equalTo(vals[i]));

        long baseTime = clock.time;
        for (TestKey key : keys) {
            clock.time += IDLE_EXPIRATION.toNanos();
            cs.unref(key);
            cs.ref(key);
            clock.time += IDLE_EXPIRATION.toNanos();
            cs.unref(key);
        }

        for (int i = 0; i < keys.length; i++) {
            clock.time = baseTime + ((i+1) * IDLE_EXPIRATION.toNanos() * 2);
            clock.time += IDLE_EXPIRATION.toNanos() + 1;
            cs.expireIdleEntries();

            for (int j = 0; j < keys.length; j++) {
                if (j <= i)
                    assertThat(cs.get(keys[j]), nullValue());
                else
                    assertThat(cs.get(keys[j]), equalTo(vals[j]));
            }
        }

        for (TestKey key : keys)
            assertThat(cs.get(key), nullValue());
    }
}
