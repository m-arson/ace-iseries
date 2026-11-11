package com.ibm.swg.iseries.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400ConnectionPool;
import org.junit.jupiter.api.Test;

class ConnectionAcquisitionTest {
	private static final class Pool extends AS400ConnectionPool {
		private static final long serialVersionUID = 1L;
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch proceed = new CountDownLatch(1);
		final AtomicInteger inside = new AtomicInteger();
		final AtomicInteger maximumInside = new AtomicInteger();
		final AtomicInteger returned = new AtomicInteger();
		volatile boolean closed;

		@Override public AS400 getConnection(String address, String user, char[] password, int service) {
			maximumInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
			entered.countDown();
			try {
				assertTrue(proceed.await(10, TimeUnit.SECONDS));
				return new AS400();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			} finally {
				inside.decrementAndGet();
			}
		}
		@Override public void returnConnectionToPool(AS400 connection) { returned.incrementAndGet(); }
		@Override public void close() { closed = true; }
	}

	private static void install(String name, Pool pool) throws ReflectiveOperationException {
		Class<?> holder = Class.forName(ConnectionCache.class.getName() + "$Holder");
		Field instance = holder.getDeclaredField("INSTANCE");
		instance.setAccessible(true);
		Field pools = ConnectionCache.class.getDeclaredField("pools");
		pools.setAccessible(true);
		Class<?> keyType = Class.forName(ConnectionCache.class.getName() + "$PolicyKey");
		var keyConstructor = keyType.getDeclaredConstructor(String.class, String.class);
		keyConstructor.setAccessible(true);
		Class<?> entryType = Class.forName(ConnectionCache.class.getName() + "$Entry");
		var entryConstructor = entryType.getDeclaredConstructor(ConfigurationData.class);
		entryConstructor.setAccessible(true);
		Object entry = entryConstructor.newInstance(new ConfigurationData("unused", "USER", new char[] { 'p' },
				1, 0, -1, "1208", pool));
		Field owners = entryType.getDeclaredField("owners");
		owners.setAccessible(true);
		owners.setInt(entry, 1);
		synchronized (ConnectionCache.class) {
			Map.class.getMethod("put", Object.class, Object.class).invoke(pools.get(instance.get(null)),
					keyConstructor.newInstance("UserDefined", name), entry);
		}
	}

	@Test
	void serializesColdAcquisitionPerPoolButNotOtherPoliciesOrLeaseLifetime() throws Exception {
		String name = "cold-acquisition";
		String otherName = "independent-acquisition";
		Pool pool = new Pool();
		Pool other = new Pool();
		other.proceed.countDown();
		install(name, pool);
		install(otherName, other);
		ExecutorService workers = Executors.newFixedThreadPool(17);
		List<Future<AS400>> pending = new ArrayList<>();
		List<AS400> leases = new ArrayList<>();
		CountDownLatch ready = new CountDownLatch(16);
		CountDownLatch start = new CountDownLatch(1);
		try {
			for (int i = 0; i < 16; i++) {
				pending.add(workers.submit(() -> {
					ready.countDown();
					assertTrue(start.await(10, TimeUnit.SECONDS));
					return ConnectionCache.getConnection("UserDefined", name);
				}));
			}
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			assertTrue(pool.entered.await(10, TimeUnit.SECONDS));
			Future<AS400> independent = workers.submit(() -> ConnectionCache.getConnection("UserDefined", otherName));
			ConnectionCache.releaseConnection("UserDefined", otherName, independent.get(5, TimeUnit.SECONDS));
			assertFalse(pool.closed);
			pool.proceed.countDown();
			for (Future<AS400> request : pending) {
				leases.add(request.get(10, TimeUnit.SECONDS));
			}
			assertEquals(1, pool.maximumInside.get());
			ConnectionCache.releasePolicy("UserDefined", name);
			assertFalse(pool.closed);
			for (AS400 lease : leases) {
				ConnectionCache.releaseConnection("UserDefined", name, lease);
			}
			assertEquals(16, pool.returned.get());
			assertTrue(pool.closed);
		} finally {
			start.countDown();
			pool.proceed.countDown();
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
			for (Future<AS400> request : pending) {
				if (request.isDone() && !request.isCancelled()) {
					ConnectionCache.releaseConnection("UserDefined", name, request.get());
				}
			}
			ConnectionCache.releasePolicy("UserDefined", name);
			ConnectionCache.releasePolicy("UserDefined", otherName);
		}
	}
}
