package com.ibm.swg.iseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbNode;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.cache.ConnectionCache;

public class PolicySelectionTest {
	public static final class Flow extends FlowNode {
		MbPolicy selected;

		public Flow() {
			super();
		}

		@Override
		public MbPolicy getPolicy(String type, String name) {
			return selected;
		}
	}

	public static final class Input extends InputNode {
		MbPolicy selected;

		public Input() throws MbException {
			super();
		}

		@Override
		public MbPolicy getPolicy(String type, String name) {
			return selected;
		}
	}

	private static final class TestPolicy extends MbPolicy {
		TestPolicy(Properties properties) throws MbException {
			super("UserDefined", "test");
			for (String name : properties.stringPropertyNames()) {
				setProperty(name, properties.getProperty(name), properties.getProperty(name));
			}
		}
	}

	private static Properties configuration() {
		Properties properties = new Properties();
		properties.setProperty("iSeriesAddress", "localhost");
		properties.setProperty("iSeriesUser", "USER");
		properties.setProperty("iSeriesPassword", "password");
		properties.setProperty("CCSID", "37");
		properties.setProperty("maxConnections", "5");
		properties.setProperty("minConnections", "0");
		properties.setProperty("maxUseTimeout", "1000");
		return properties;
	}

	private static void selectPolicy(MbNode node, MbPolicy policy) {
		if (node instanceof Flow flow) {
			flow.selected = policy;
		} else if (node instanceof Input input) {
			input.selected = policy;
		}
	}

	static Stream<MbNode> nodes() throws MbException {
		return Stream.of(new Flow(), new Input());
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void toolkitPropertySupportsLegacyAndQualifiedSelection(MbNode node) throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("configurableService", node.getClass());
		assertEquals("myISeries", property.getReadMethod().invoke(node));
		assertEquals("{IAM7}:myISeries", node.getClass().getMethod("getPolicyCacheKey").invoke(node));
		node.onPreSetupValidation();
		property.getWriteMethod().invoke(node, "oldPolicy");
		assertEquals("oldPolicy", property.getReadMethod().invoke(node));
		assertEquals("{IAM7}:oldPolicy", node.getClass().getMethod("getPolicyCacheKey").invoke(node));
		property.getWriteMethod().invoke(node, "{Production}:newPolicy");
		assertEquals("{Production}:newPolicy", property.getReadMethod().invoke(node));
		property.getWriteMethod().invoke(node, "anotherPolicy");
		assertEquals("anotherPolicy", property.getReadMethod().invoke(node));
		assertEquals("{Production}:anotherPolicy", node.getClass().getMethod("getPolicyCacheKey").invoke(node));
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void invalidSelectionLeavesPreviousPolicyIntact(MbNode node) throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("configurableService", node.getClass());
		property.getWriteMethod().invoke(node, "{Production}:selected");
		for (String invalid : new String[] { null, "", " ", "{}:name", "{project}:",
				"{project}:bad:name", "{ }:name", "{project}: ", "{project}:bad name", "{bad project}:name" }) {
			InvocationTargetException failure = assertThrows(InvocationTargetException.class,
					() -> property.getWriteMethod().invoke(node, invalid));
			assertInstanceOf(IllegalArgumentException.class, failure.getCause());
			assertEquals("{Production}:selected", property.getReadMethod().invoke(node));
		}
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void legacySettersUseDefaultProjectForNullAndBlank(MbNode node) throws Exception {
		node.getClass().getMethod("setPolicy", String.class).invoke(node, "selected");
		assertEquals("selected", node.getClass().getMethod("getConfigurableService").invoke(node));
		for (String project : new String[] { null, "", " \t" }) {
			node.getClass().getMethod("setPolicyProject", String.class).invoke(node, project);
			assertEquals(project, node.getClass().getMethod("getPolicyProject").invoke(node));
			assertEquals("{IAM7}:selected", node.getClass().getMethod("getPolicyCacheKey").invoke(node));
		}
		node.getClass().getMethod("setPolicyProject", String.class).invoke(node, "Production");
		assertEquals("{Production}:selected", node.getClass().getMethod("getPolicyCacheKey").invoke(node));
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void bothPolicyReadersRequireCredentialsAndValidateSuppliedNumbers(MbNode node) throws Exception {
		String key = "{Tests}:reader-" + UUID.randomUUID();
		node.getClass().getMethod("setConfigurableService", String.class).invoke(node, key);
		selectPolicy(node, null);
		InvocationTargetException absent = assertThrows(InvocationTargetException.class,
				() -> node.getClass().getMethod("loadPolicy").invoke(node));
		assertInstanceOf(MbUserException.class, absent.getCause());
		for (String field : new String[] { "iSeriesAddress", "iSeriesUser", "iSeriesPassword" }) {
			for (String value : new String[] { null, "", " \t" }) {
				Properties invalid = configuration();
				if (value == null) {
					invalid.remove(field);
				} else {
					invalid.setProperty(field, value);
				}
				selectPolicy(node, new TestPolicy(invalid));
				InvocationTargetException failure = assertThrows(InvocationTargetException.class,
						() -> node.getClass().getMethod("loadPolicy").invoke(node));
				assertInstanceOf(MbUserException.class, failure.getCause());
				assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
			}
		}
		for (String[] setting : new String[][] { { "CCSID", "-1", "65536" },
				{ "maxConnections", "0", "1025" }, { "minConnections", "-1", "6" },
				{ "maxUseTimeout", "-2", "0", "86400001" } }) {
			List<String> invalidValues = new ArrayList<>(List.of("bad", "1.5", "2147483648"));
			invalidValues.addAll(Arrays.asList(setting).subList(1, setting.length));
			for (String value : invalidValues) {
				Properties invalid = configuration();
				invalid.setProperty(setting[0], value);
				selectPolicy(node, new TestPolicy(invalid));
				InvocationTargetException failure = assertThrows(InvocationTargetException.class,
						() -> node.getClass().getMethod("loadPolicy").invoke(node));
				assertInstanceOf(MbUserException.class, failure.getCause());
				assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
			}
		}
		selectPolicy(node, new TestPolicy(configuration()));
		node.getClass().getMethod("loadPolicy").invoke(node);
		try {
			var config = ConnectionCache.getConfigurationData("UserDefined", key);
			assertNotNull(config);
			assertEquals("37", config.get_CCSID());
		} finally {
			node.onTearDown();
		}
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void bothPolicyReadersAllowMissingAndBlankOptionalNumbers(MbNode node) throws Exception {
		for (String value : new String[] { null, "", " \t" }) {
			String key = "{Tests}:optional-" + UUID.randomUUID();
			Properties properties = configuration();
			for (String field : new String[] { "maxConnections", "maxUseTimeout", "CCSID" }) {
				if (value == null) {
					properties.remove(field);
				} else {
					properties.setProperty(field, value);
				}
			}
			PropertyDescriptor selection = new PropertyDescriptor("configurableService", node.getClass());
			selection.getWriteMethod().invoke(node, key);
			selectPolicy(node, new TestPolicy(properties));
			java.lang.reflect.Method reader = node.getClass().getSuperclass().getDeclaredMethod("readPolicy", String.class);
			reader.setAccessible(true);
			Properties read = (Properties) reader.invoke(node, key);
			ConnectionCache.acquirePolicy("UserDefined", key, read);
			try {
				var config = ConnectionCache.getConfigurationData("UserDefined", key);
				assertNotNull(config);
				assertEquals(5, config.get_maxConnections());
				assertEquals(-1, config.get_maxUseTimeout());
				assertEquals("1208", config.get_CCSID());
			} finally {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
			if (value == null) {
				properties.remove("minConnections");
			} else {
				properties.setProperty("minConnections", value);
			}
			selectPolicy(node, new TestPolicy(properties));
			read = (Properties) reader.invoke(node, key);
			assertEquals(value, read.getProperty("minConnections"));
		}
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void initializedAndTornDownNodesCannotSwitchPolicies(MbNode node) throws Exception {
		String key = "{Tests}:lifecycle-" + UUID.randomUUID();
		PropertyDescriptor property = new PropertyDescriptor("configurableService", node.getClass());
		property.getWriteMethod().invoke(node, key);
		selectPolicy(node, new TestPolicy(configuration()));
		node.getClass().getMethod("loadPolicy").invoke(node);
		try {
			property.getWriteMethod().invoke(node, key);
			node.getClass().getMethod("loadPolicy").invoke(node);
			for (String replacement : new String[] { "{Other}:selected", "{Tests}:other" }) {
				InvocationTargetException failure = assertThrows(InvocationTargetException.class,
						() -> property.getWriteMethod().invoke(node, replacement));
				assertInstanceOf(IllegalStateException.class, failure.getCause());
				assertEquals(key, property.getReadMethod().invoke(node));
			}
		} finally {
			node.onTearDown();
		}
		assertNull(ConnectionCache.getConfigurationData("UserDefined", key));
		InvocationTargetException switched = assertThrows(InvocationTargetException.class,
				() -> property.getWriteMethod().invoke(node, "{Other}:selected"));
		assertInstanceOf(IllegalStateException.class, switched.getCause());
		InvocationTargetException reloaded = assertThrows(InvocationTargetException.class,
				() -> node.getClass().getMethod("loadPolicy").invoke(node));
		assertInstanceOf(IllegalStateException.class, reloaded.getCause());
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void concurrentConfigurationNeverExposesMixedPolicyIdentity(MbNode node) throws Exception {
		PropertyDescriptor property = new PropertyDescriptor("configurableService", node.getClass());
		property.getWriteMethod().invoke(node, "{Project0}:policy0");
		ExecutorService workers = Executors.newFixedThreadPool(4);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < 4; i++) {
				String selection = "{Project" + i + "}:policy" + i;
				futures.add(workers.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					for (int j = 0; j < 100; j++) {
						property.getWriteMethod().invoke(node, selection);
						String current = (String) property.getReadMethod().invoke(node);
						assertTrue(current.matches("\\{Project([0-3])}:policy\\1"), current);
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(10, TimeUnit.SECONDS);
			}
		} finally {
			start.countDown();
			workers.shutdownNow();
			assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
		}
	}
}
