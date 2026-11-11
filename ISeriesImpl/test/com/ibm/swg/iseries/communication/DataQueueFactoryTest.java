package com.ibm.swg.iseries.communication;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.swg.iseries.cache.DataQueueCache;
import com.ibm.swg.iseries.RuntimeFailure;

class DataQueueFactoryTest {
	@Test
	void keysUseEncodedByteLengthAndRejectTruncation() throws Exception {
		assertArrayEquals(new byte[] { (byte) 0xC1, 0x40, 0x40 }, KeyedDataQueueFactory.keyBytes("A", 3, "37", null));
		assertArrayEquals(new byte[] { (byte) 0xC3, (byte) 0xA9, 0x20 },
				KeyedDataQueueFactory.keyBytes("é", 3, "1208", null));
		assertThrows(IllegalArgumentException.class, () -> KeyedDataQueueFactory.keyBytes("é", 1, "1208", null));
		assertThrows(java.nio.charset.CharacterCodingException.class,
				() -> KeyedDataQueueFactory.keyBytes("日本", 8, "37", null));
	}

	@Test
	void wrongCcsidReportsSafeKeyLengthsWithoutKeyContents() {
		IllegalArgumentException cause = assertThrows(IllegalArgumentException.class,
				() -> KeyedDataQueueFactory.keyBytes("0123456789abcdef0123456789abcdef", 32, "1200", null));
		String text = RuntimeFailure.create("Test", "key", "Key encoding failed.", cause).getTraceText();
		assertTrue(text.contains("resolvedCCSID=1200"), text);
		assertTrue(text.contains("encodedBytes=64"), text);
		assertTrue(text.contains("queueKeyBytes=32"), text);
		assertFalse(text.contains("0123456789abcdef"), text);
	}

	@Test
	void invalidQueueCcsidReportsConfigurationStage() {
		DataQueue queue = new DataQueue(new AS400(), "/QSYS.LIB/LIB.LIB/QUEUE.DTAQ");
		var failure = assertThrows(com.ibm.broker.plugin.MbUserException.class,
				() -> BaseDataQueueFactory.configure(queue, "2222"));
		assertTrue(failure.getTraceText().contains("CCSID configuration failed: configuredCCSID=2222"));
		assertTrue(failure.getTraceText().contains("returnCode="));
		var unavailable = assertThrows(com.ibm.broker.plugin.MbUserException.class,
				() -> BaseDataQueueFactory.configure(queue, "1201"));
		assertTrue(unavailable.getTraceText().contains("configuredCCSID=1201"));
		assertTrue(unavailable.getTraceText().contains("UnsupportedCharsetException"));
		var malformed = assertThrows(IllegalArgumentException.class,
				() -> BaseDataQueueFactory.configure(queue, "password=secret"));
		assertFalse(RuntimeFailure.create("Test", "configure", "CCSID validation failed.", malformed)
				.getTraceText().contains("secret"));
	}

	@Test
	void zeroCcsidUsesEachLeasedSystemInsteadOfIbm037() throws Exception {
		AS400 utf8 = new AS400();
		utf8.setCcsid(1208);
		AS400 ebcdic = new AS400();
		ebcdic.setCcsid(37);
		assertArrayEquals(new byte[] { (byte) 0xC3, (byte) 0xA9, 0x20 },
				KeyedDataQueueFactory.keyBytes("é", 3, "0", utf8));
		assertArrayEquals(new byte[] { (byte) 0xC1, 0x40, 0x40 },
				KeyedDataQueueFactory.keyBytes("A", 3, "0", ebcdic));
		assertThrows(IllegalArgumentException.class,
				() -> KeyedDataQueueFactory.keyBytes("é", 1, "0", utf8));
		assertThrows(NullPointerException.class,
				() -> KeyedDataQueueFactory.keyBytes("A", 1, "0", null));
	}

	@Test
	void parallelRequestsNeverReuseAnotherLeaseOrQueueType() throws Exception {
		DataQueueCache cache = new DataQueueCache();
		ExecutorService workers = Executors.newFixedThreadPool(16);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Void>> results = new ArrayList<>();
		try {
			for (int index = 0; index < 128; index++) {
				boolean keyed = index % 2 == 0;
				results.add(workers.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					AS400 system = new AS400();
					BaseDataQueueFactory factory = BaseDataQueueFactory.getFactory(keyed, cache);
					BaseDataQueue queue = factory.getDataQueue(system, "LIB", "QUEUE", "DTAQ");
					assertSame(system, queue.getSystem());
					assertNotSame(queue, factory.getDataQueue(system, "LIB", "QUEUE", "DTAQ"));
					if (keyed) {
						assertInstanceOf(KeyedDataQueue.class, queue);
					} else {
						assertInstanceOf(DataQueue.class, queue);
					}
					for (int service = 0; service <= 7; service++) {
						assertFalse(system.isConnected(service));
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<Void> result : results) {
				result.get(20, TimeUnit.SECONDS);
			}
		} finally {
			start.countDown();
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
		}
	}
}
