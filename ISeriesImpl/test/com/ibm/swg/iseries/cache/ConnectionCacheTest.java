package com.ibm.swg.iseries.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.ibm.broker.plugin.MbUserException;
import org.junit.jupiter.api.Test;

public class ConnectionCacheTest {
	private static Properties configuration() {
		Properties properties = new Properties();
		properties.setProperty("iSeriesAddress", "localhost");
		properties.setProperty("iSeriesUser", "USER");
		properties.setProperty("iSeriesPassword", "password");
		properties.setProperty("CCSID", "1208");
		properties.setProperty("maxConnections", "5");
		properties.setProperty("minConnections", "0");
		properties.setProperty("maxUseTimeout", "-1");
		return properties;
	}

	@Test
	void retainsPoolUntilLastOwnerWithoutOpeningConnections() throws Exception {
		String key = "owner-reference-test";
		ConnectionCache.acquirePolicy("UserDefined", key, configuration());
		ConfigurationData first = ConnectionCache.getConfigurationData("UserDefined", key);
		assertNotNull(first);
		try {
			ConnectionCache.acquirePolicy("UserDefined", key, configuration());
			try {
				assertSame(first, ConnectionCache.getConfigurationData("UserDefined", key));
				assertEquals(0, first.get_iSeriesConnectionPool().getActiveConnectionCount("localhost", "USER"));
				assertEquals(0, first.get_iSeriesConnectionPool().getAvailableConnectionCount("localhost", "USER"));
				assertFalse(first.get_iSeriesConnectionPool().isRunMaintenance());
				assertFalse(first.get_iSeriesConnectionPool().isThreadUsed());
				assertTrue(first.get_iSeriesConnectionPool().getSocketProperties().getLoginTimeout() > 0);
				assertTrue(first.get_iSeriesConnectionPool().getSocketProperties().getSoTimeout() > 0);
			} finally {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
			assertSame(first, ConnectionCache.getConfigurationData("UserDefined", key));
		} finally {
			ConnectionCache.releasePolicy("UserDefined", key);
		}
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
		assertEquals("\0".repeat("password".length()), first.get_iSeriesPassword());
		assertThrows(MbUserException.class, () -> ConnectionCache.getConnection("UserDefined", key));
	}

	@Test
	void separatesHashCollisionsProjectsAndDelimitedKeys() throws Exception {
		String[][] keys = {
			{"UserDefined", "project/Aa"}, {"UserDefined", "project/BB"},
			{"UserDefined", "other/Aa"}, {"type/name", "policy"}, {"type", "name/policy"}
		};
		assertEquals("Aa".hashCode(), "BB".hashCode());
		List<ConfigurationData> configurations = new ArrayList<>();
		try {
			for (String[] key : keys) {
				ConnectionCache.acquirePolicy(key[0], key[1], configuration());
				ConfigurationData current = ConnectionCache.getConfigurationData(key[0], key[1]);
				for (ConfigurationData previous : configurations) {
					assertNotSame(previous, current);
				}
				configurations.add(current);
			}
		} finally {
			for (String[] key : keys) {
				ConnectionCache.releasePolicy(key[0], key[1]);
			}
		}
		for (String[] key : keys) {
			assertNull(ConnectionCache.getConfigurationData(key[0], key[1]));
		}
	}

	@Test
	void differentPoliciesKeepTheirSettingsAndRetiredPolicyReloads() throws Exception {
		String firstKey = "{Production}:shared-name";
		String secondKey = "{Testing}:shared-name";
		Properties updated = configuration();
		updated.setProperty("iSeriesAddress", "new-host.invalid");
		ConnectionCache.acquirePolicy("UserDefined", firstKey, configuration());
		ConfigurationData original = ConnectionCache.getConfigurationData("UserDefined", firstKey);
		assertNotNull(original);
		try {
			ConnectionCache.acquirePolicy("UserDefined", secondKey, updated);
			try {
				assertEquals("localhost", original.get_iSeriesAddress());
				ConfigurationData second = ConnectionCache.getConfigurationData("UserDefined", secondKey);
				assertNotNull(second);
				assertEquals("new-host.invalid", second.get_iSeriesAddress());
			} finally {
				ConnectionCache.releasePolicy("UserDefined", secondKey);
			}
		} finally {
			ConnectionCache.releasePolicy("UserDefined", firstKey);
		}
		ConnectionCache.acquirePolicy("UserDefined", firstKey, updated);
		try {
			ConfigurationData reloaded = ConnectionCache.getConfigurationData("UserDefined", firstKey);
			assertNotNull(reloaded);
			assertNotSame(original, reloaded);
			assertEquals("new-host.invalid", reloaded.get_iSeriesAddress());
		} finally {
			ConnectionCache.releasePolicy("UserDefined", firstKey);
		}
	}

	@Test
	void missingAndBlankCredentialsRejectedForNewAndExistingPools() throws Exception {
		for (String field : new String[] { "iSeriesAddress", "iSeriesUser", "iSeriesPassword" }) {
			for (String value : new String[] { null, "", " \t" }) {
				Properties invalid = configuration();
				if (value == null) {
					invalid.remove(field);
				} else {
					invalid.setProperty(field, value);
				}
				assertRejectedForNewAndExistingPools(invalid);
			}
		}
		assertRejectedForNewAndExistingPools(null);
	}

	@Test
	void missingAndBlankNumbersUseLegacyDefaultsBeforeRangeValidation() throws Exception {
		for (String value : new String[] { null, "", " \t" }) {
			Properties properties = configuration();
			for (String field : new String[] { "maxConnections", "minConnections", "maxUseTimeout", "CCSID" }) {
				if (value == null) {
					properties.remove(field);
				} else {
					properties.setProperty(field, value);
				}
			}
			assertEquals(5, ConnectionCache.setting(properties, "maxConnections", 5, 1, 1024));
			assertEquals(1, ConnectionCache.setting(properties, "minConnections", 1, 0, 5));
			assertEquals(-1, ConnectionCache.setting(properties, "maxUseTimeout", -1, -1, 86_400_000));
			assertEquals(1208, ConnectionCache.setting(properties, "CCSID", 1208, 0, 65535));
			assertThrows(IllegalArgumentException.class,
					() -> ConnectionCache.setting(properties, "minConnections", 1, 0, 0));
			properties.setProperty("minConnections", "0");
			String key = "numeric-defaults-test";
			ConnectionCache.acquirePolicy("UserDefined", key, properties);
			try {
				ConfigurationData current = ConnectionCache.getConfigurationData("UserDefined", key);
				assertNotNull(current);
				assertEquals(5, current.get_maxConnections());
				assertEquals(-1, current.get_maxUseTimeout());
				assertEquals("1208", current.get_CCSID());
				ConnectionCache.acquirePolicy("UserDefined", key, configuration());
				ConnectionCache.releasePolicy("UserDefined", key);
			} finally {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
		}
	}

	@Test
	void malformedAndOutOfRangeNumbersRejectedForNewAndExistingPools() throws Exception {
		String[][] invalidSettings = {
				{ "CCSID", "-1", "65536" },
				{ "maxConnections", "-1", "0", "1025" },
				{ "minConnections", "-1", "6" },
				{ "maxUseTimeout", "-2", "0", "86400001" }
		};
		for (String[] setting : invalidSettings) {
			for (String malformed : new String[] { "no", "1.5", "2147483648", "-2147483649" }) {
				Properties invalid = configuration();
				invalid.setProperty(setting[0], malformed);
				assertRejectedForNewAndExistingPools(invalid);
			}
			for (int i = 1; i < setting.length; i++) {
				Properties invalid = configuration();
				invalid.setProperty(setting[0], setting[i]);
				assertRejectedForNewAndExistingPools(invalid);
			}
		}
	}

	private static void assertRejectedForNewAndExistingPools(Properties invalid) throws MbUserException {
		String key = "invalid-configuration-test";
		assertThrows(MbUserException.class, () -> ConnectionCache.setISeriesPool("UserDefined", key, invalid));
		assertThrows(MbUserException.class, () -> ConnectionCache.acquirePolicy("UserDefined", key, invalid));
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
		ConnectionCache.acquirePolicy("UserDefined", key, configuration());
		ConfigurationData original = ConnectionCache.getConfigurationData("UserDefined", key);
		try {
			assertThrows(MbUserException.class, () -> ConnectionCache.setISeriesPool("UserDefined", key, invalid));
			assertThrows(MbUserException.class, () -> ConnectionCache.acquirePolicy("UserDefined", key, invalid));
			assertSame(original, ConnectionCache.getConfigurationData("UserDefined", key));
		} finally {
			ConnectionCache.releasePolicy("UserDefined", key);
		}
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
	}

	@Test
	void replacingAnyOfSevenSettingsRejectedWithoutChangingActivePool() throws Exception {
		String key = "settings-replacement-test";
		String[][] replacements = {
				{ "iSeriesAddress", "other-host.invalid" }, { "iSeriesUser", "OTHERUSER" },
				{ "iSeriesPassword", "other-password" }, { "CCSID", "37" },
				{ "maxConnections", "6" }, { "minConnections", "1" }, { "maxUseTimeout", "1000" }
		};
		ConnectionCache.acquirePolicy("UserDefined", key, configuration());
		ConfigurationData original = ConnectionCache.getConfigurationData("UserDefined", key);
		try {
			for (String[] replacement : replacements) {
				Properties updated = configuration();
				updated.setProperty(replacement[0], replacement[1]);
				MbUserException failure = assertThrows(MbUserException.class,
						() -> ConnectionCache.setISeriesPool("UserDefined", key, updated));
				assertEquals("Policy configuration failed. [DiagnosticArgumentException, Policy changed while in use; redeploy all sharing flows or use a new policy name.]",
						failure.getTraceText());
				assertThrows(MbUserException.class, () -> ConnectionCache.acquirePolicy("UserDefined", key, updated));
				assertSame(original, ConnectionCache.getConfigurationData("UserDefined", key));
				assertNotNull(original);
				assertEquals("localhost", original.get_iSeriesAddress());
				assertEquals("USER", original.get_iSeriesUser());
				assertEquals("password", original.get_iSeriesPassword());
				assertEquals("1208", original.get_CCSID());
				assertEquals(5, original.get_maxConnections());
				assertEquals(0, original.get_minConnections());
				assertEquals(-1, original.get_maxUseTimeout());
			}
		} finally {
			ConnectionCache.releasePolicy("UserDefined", key);
		}
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
	}

	@Test
	void equivalentNormalizedSettingsShareExistingPool() throws Exception {
		String key = "normalized-settings-test";
		ConnectionCache.acquirePolicy("UserDefined", key, configuration());
		ConfigurationData original = ConnectionCache.getConfigurationData("UserDefined", key);
		try {
			Properties equivalent = configuration();
			equivalent.setProperty("iSeriesAddress", " localhost ");
			equivalent.setProperty("iSeriesUser", " USER ");
			equivalent.setProperty("CCSID", " 01208 ");
			equivalent.setProperty("maxConnections", " 05 ");
			equivalent.setProperty("minConnections", " 00 ");
			equivalent.setProperty("maxUseTimeout", " -01 ");
			ConnectionCache.acquirePolicy("UserDefined", key, equivalent);
			try {
				assertSame(original, ConnectionCache.getConfigurationData("UserDefined", key));
			} finally {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
		} finally {
			ConnectionCache.releasePolicy("UserDefined", key);
		}
	}

	@Test
	void numericBoundsAcceptedWithoutOpeningConnections() throws Exception {
		for (String[] values : new String[][] { { "0", "1", "-1" }, { "65535", "1024", "86400000" } }) {
			String key = "numeric-bounds-test";
			Properties properties = configuration();
			properties.setProperty("CCSID", values[0]);
			properties.setProperty("maxConnections", values[1]);
			properties.setProperty("maxUseTimeout", values[2]);
			ConnectionCache.acquirePolicy("UserDefined", key, properties);
			try {
				ConfigurationData current = ConnectionCache.getConfigurationData("UserDefined", key);
				assertNotNull(current);
				assertEquals(values[0], current.get_CCSID());
				assertEquals(Integer.parseInt(values[1]), current.get_maxConnections());
				assertEquals(Integer.parseInt(values[2]), current.get_maxUseTimeout());
				ConnectionCache.acquirePolicy("UserDefined", key, properties);
				ConnectionCache.releasePolicy("UserDefined", key);
			} finally {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
		}
	}

	@Test
	void concurrentOwnersReleaseLastPool() throws Exception {
		String key = "concurrent-owner-test";
		ExecutorService workers = Executors.newFixedThreadPool(8);
		CountDownLatch ready = new CountDownLatch(8);
		CountDownLatch release = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		ConfigurationData retained;
		try {
			for (int i = 0; i < 8; i++) {
				futures.add(workers.submit(() -> {
					ConnectionCache.acquirePolicy("UserDefined", key, configuration());
					ready.countDown();
					try {
						assertTrue(release.await(10, TimeUnit.SECONDS));
						assertNotNull(ConnectionCache.getConfigurationData("UserDefined", key));
					} finally {
						ConnectionCache.releasePolicy("UserDefined", key);
					}
					return null;
				}));
			}
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			retained = ConnectionCache.getConfigurationData("UserDefined", key);
			assertNotNull(retained);
			release.countDown();
			for (Future<?> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		} finally {
			release.countDown();
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
		}
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
		assertEquals("\0".repeat("password".length()), retained.get_iSeriesPassword());
	}
}
