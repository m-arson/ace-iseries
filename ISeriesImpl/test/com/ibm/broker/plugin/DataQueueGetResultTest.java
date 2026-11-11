package com.ibm.broker.plugin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import com.ibm.swg.iseries.dataqueue.DataQueueGetNode;
import org.junit.jupiter.api.Test;

class DataQueueGetResultTest {
	private static final class Element extends MbElement {
		final String name;
		final Object value;
		final List<Element> children = new ArrayList<>();
		Element parent;

		Element(String name, Object value) {
			super(0L);
			this.name = name;
			this.value = value;
		}

		@Override public String getName() { return name; }
		@Override public MbElement getFirstChild() { return children.isEmpty() ? null : children.get(0); }
		@Override public MbElement getNextSibling() {
			int index = parent.children.indexOf(this) + 1;
			return index < parent.children.size() ? parent.children.get(index) : null;
		}
		@Override public void delete() { parent.children.remove(this); }
		@Override public MbElement createElementAsLastChild(String parser) {
			return createElementAsLastChild(MbElement.TYPE_NAME,
					MbBLOB.PARSER_NAME.equals(parser) ? MbBLOB.ROOT_ELEMENT_NAME : parser, null);
		}
		@Override public MbElement createElementAsLastChild(int type, String childName, Object childValue) {
			Element child = new Element(childName, childValue);
			child.parent = this;
			children.add(child);
			return child;
		}

		Element copyTree() {
			Element copy = new Element(name, value);
			for (Element child : children) {
				Element copiedChild = child.copyTree();
				copiedChild.parent = copy;
				copy.children.add(copiedChild);
			}
			return copy;
		}
	}

	private static void replace(Element root, byte[] message) throws ReflectiveOperationException {
		var method = DataQueueGetNode.class.getDeclaredMethod("replaceResult", MbElement.class, byte[].class);
		method.setAccessible(true);
		method.invoke(null, root, message);
	}

	@Test
	void replacesAllStaleResultsOnCopyAndPreservesOtherLocalEnvironmentFields() throws Exception {
		Element input = new Element("Root", null);
		input.createElementAsLastChild("BLOB").createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "BLOB", new byte[] { 1 });
		input.createElementAsLastChild("DataQueue");
		input.createElementAsLastChild("BLOB");
		input.createElementAsLastChild("Variables");
		Element output = input.copyTree();
		replace(output, new byte[] { 2, 3 });
		assertEquals(List.of("DataQueue", "Variables", "BLOB"), output.children.stream().map(e -> e.name).toList());
		assertEquals(1, output.children.get(2).children.size());
		assertArrayEquals(new byte[] { 2, 3 }, (byte[]) output.children.get(2).children.get(0).value);
		assertEquals(List.of("BLOB", "DataQueue", "BLOB", "Variables"), input.children.stream().map(e -> e.name).toList());
		assertArrayEquals(new byte[] { 1 }, (byte[]) input.children.get(0).children.get(0).value);
	}

	@Test
	void noMessageClearsPriorResultAndEmptyMessageStillProducesResult() throws Exception {
		Element root = new Element("Root", null);
		root.createElementAsLastChild("DataQueue");
		replace(root, new byte[0]);
		assertEquals(List.of("DataQueue", "BLOB"), root.children.stream().map(e -> e.name).toList());
		assertArrayEquals(new byte[0], (byte[]) root.children.get(1).children.get(0).value);
		replace(root, null);
		assertEquals(List.of("DataQueue"), root.children.stream().map(e -> e.name).toList());
		replace(root, null);
		assertEquals(1, root.children.size());
	}
}
