package com.ibm.swg.iseries;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputTerminal;
import com.ibm.broker.plugin.MbNode;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.swg.iseries.dataqueue.DataQueueGetNode;
import com.ibm.swg.iseries.pcml.ISeriesPcmlCallNode;

public class NodePropertiesTest {
	public static final class QueueFlow extends DataQueueFlowNode {
		public QueueFlow() {
			super();
		}
	}

	public static final class QueueInput extends DataQueueInNode {
		public QueueInput() throws MbException {
			super();
		}
	}

	public static final class QueueGet extends DataQueueGetNode {
		public QueueGet() throws MbException {
			super();
		}

		@Override
		public MbInputTerminal createInputTerminal(String name) {
			return null;
		}

		@Override
		public MbOutputTerminal createOutputTerminal(String name) {
			return null;
		}
	}

	public static final class Pcml extends ISeriesPcmlCallNode {
		public Pcml() throws MbException {
			super();
		}

		@Override
		public MbInputTerminal createInputTerminal(String name) {
			return null;
		}

		@Override
		public MbOutputTerminal createOutputTerminal(String name) {
			return null;
		}
	}

	static Stream<MbNode> nodes() throws MbException {
		return Stream.of(new QueueFlow(), new QueueInput(), new QueueGet(), new Pcml());
	}

	static Stream<MbNode> queues() throws MbException {
		return Stream.of(new QueueFlow(), new QueueInput(), new QueueGet());
	}

	static Stream<MbNode> waitingQueues() throws MbException {
		return Stream.of(new QueueInput(), new QueueGet());
	}

	private static Object read(MbNode node, String name) throws Exception {
		return new PropertyDescriptor(name, node.getClass()).getReadMethod().invoke(node);
	}

	private static void write(MbNode node, String name, Object value) throws Exception {
		new PropertyDescriptor(name, node.getClass()).getWriteMethod().invoke(node, value);
	}

	private static void roundTrip(MbNode node, String name, Object value) throws Exception {
		write(node, name, value);
		assertEquals(value, read(node, name));
	}

	private static void rejects(MbNode node, String name, Object value) throws Exception {
		Object original = read(node, name);
		InvocationTargetException failure = assertThrows(InvocationTargetException.class,
				() -> write(node, name, value));
		assertInstanceOf(IllegalArgumentException.class, failure.getCause());
		assertEquals(original, read(node, name));
	}

	@ParameterizedTest
	@MethodSource("nodes")
	void sharedPropertiesRoundTripAndRejectMalformedBooleans(MbNode node) throws Exception {
		assertEquals("myISeries", read(node, "configurableService"));
		assertEquals("myISeries", read(node, "policy"));
		assertEquals("IAM7", read(node, "policyProject"));
		assertEquals("", read(node, "directory"));
		assertEquals("false", read(node, "enable"));
		roundTrip(node, "policyProject", "Tests");
		roundTrip(node, "policy", "selected");
		assertEquals("selected", read(node, "configurableService"));
		roundTrip(node, "configurableService", "{Other}:explicit");
		roundTrip(node, "directory", "/tmp/opencode/traces");
		write(node, "enable", "TRUE");
		assertEquals("true", read(node, "enable"));
		for (String invalid : new String[] { null, "", "yes", " false " }) {
			rejects(node, "enable", invalid);
		}
		roundTrip(node, "enable", "false");
		for (String name : new String[] { "policy" }) {
			for (String invalid : new String[] { null, "", " ", "bad:name", "{bad}", "bad name" }) {
				rejects(node, name, invalid);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("queues")
	void queuePropertiesRoundTripAndKeepSafeDefaults(MbNode node) throws Exception {
		assertNull(read(node, "library"));
		assertNull(read(node, "queueName"));
		assertNull(read(node, "key"));
		assertEquals("DTAQ", read(node, "type"));
		assertEquals("false", read(node, "keyed"));
		assertEquals("EQ", read(node, "searchType"));
		assertEquals("0", read(node, "keylength"));
		roundTrip(node, "library", "MYLIB");
		roundTrip(node, "queueName", "MYQUEUE");
		roundTrip(node, "type", "DTAQ");
		roundTrip(node, "key", "ABC");
		roundTrip(node, "searchType", "GE");
		write(node, "keyed", "TRUE");
		assertEquals("true", read(node, "keyed"));
		for (String invalid : new String[] { null, "", "yes", " false " }) {
			rejects(node, "keyed", invalid);
		}
		roundTrip(node, "keyed", "false");
		for (String value : new String[] { "0", "1", "256" }) {
			roundTrip(node, "keylength", value);
		}
		for (String invalid : new String[] { null, "", "bad", "-1", "257" }) {
			rejects(node, "keylength", invalid);
		}
	}

	@ParameterizedTest
	@MethodSource("waitingQueues")
	void queueWaitsStayBounded(MbNode node) throws Exception {
		assertEquals(node instanceof QueueInput ? "1" : "-1", read(node, "waitInterval"));
		roundTrip(node, "waitInterval", "-1");
		roundTrip(node, "waitInterval", "30");
		roundTrip(node, "waitInterval", "1");
		roundTrip(node, "waitInterval", "0");
		for (String invalid : new String[] { null, "", "bad", "1.5", "-2", "31", "2147483648" }) {
			rejects(node, "waitInterval", invalid);
		}
	}

	@Test
	void legacyInfiniteQueueWaitUsesBoundedPoll() {
		assertEquals(30, com.ibm.swg.iseries.communication.BaseDataQueueFactory.boundedTimeout(-1));
		assertEquals(0, com.ibm.swg.iseries.communication.BaseDataQueueFactory.boundedTimeout(0));
		assertEquals(30, com.ibm.swg.iseries.communication.BaseDataQueueFactory.boundedTimeout(30));
	}

	@Test
	void pcmlRequestCcsidDoesNotMutateConfiguredValue() throws Exception {
		Pcml node = new Pcml();
		node.setCcsid(37);
		java.lang.reflect.Method request = ISeriesPcmlCallNode.class.getDeclaredMethod("requestCcsid", String.class);
		request.setAccessible(true);
		assertEquals(1208, request.invoke(node, "1208"));
		assertEquals(37, node.getCcsid());
		assertEquals(37, request.invoke(node, (String) null));
		for (String invalid : new String[] { "", "bad", "-1", "65536", "2147483648" }) {
			InvocationTargetException failure = assertThrows(InvocationTargetException.class,
					() -> request.invoke(node, invalid));
			assertInstanceOf(MbException.class, failure.getCause());
			assertEquals(37, node.getCcsid());
		}
	}

	@Test
	void pcmlPropertiesKeepLegacyStylesheetsAndNodeCcsid() throws Exception {
		Pcml node = new Pcml();
		for (String name : new String[] { "pcmlCall", "libraryList" }) {
			assertNull(read(node, name));
		}
		assertEquals("false", read(node, "isXPcml"));
		assertEquals("transformToPcml.xsl", node.getToPcml());
		assertEquals("transformToXPcml.xsl", node.getToXPcml());
		assertEquals("transformFromXPcml.xsl", node.getFromXPcml());
		assertEquals(1208, node.getCcsid());
		roundTrip(node, "ccsid", 37);
		for (int invalid : new int[] { -1, 65536 }) {
			rejects(node, "ccsid", invalid);
		}
		roundTrip(node, "pcmlCall", "MYPROGRAM");
		roundTrip(node, "libraryList", "LIB1,LIB2");
		assertArrayEquals(new String[] { "LIB1", "LIB2" }, node.getLibraryListAsArray());
		for (String name : new String[] { "toPcml", "toXPcml", "fromXPcml" }) {
			roundTrip(node, name, name + ".xsl");
		}
		write(node, "isXPcml", "TRUE");
		assertEquals("true", read(node, "isXPcml"));
		for (String invalid : new String[] { null, "", "yes", " false " }) {
			rejects(node, "isXPcml", invalid);
		}
		roundTrip(node, "isXPcml", "false");
		node.setConfigurableService("{Project}:policy");
		node.setToPcml(null);
		node.setFromXPcml(null);
		assertThrows(MbException.class, node::onPreSetupValidation);
		node.setToPcml("toPcml.xsl");
		assertThrows(MbException.class, node::onPreSetupValidation);
		node.setFromXPcml("fromXPcml.xsl");
		node.onPreSetupValidation();
	}
}
