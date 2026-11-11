package com.ibm.swg.iseries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class ToolkitCompatibilityTest {
	private static Map<String, String> queueProperties() {
		Map<String, String> queue = new LinkedHashMap<>();
		queue.put("configurableService", "0:STRING:myISeries");
		queue.put("queueName", "0:STRING:null");
		queue.put("waitInterval", "0:INTEGER:0");
		queue.put("library", "0:STRING:null");
		queue.put("type", "0:STRING:DTAQ");
		queue.put("keyed", "0:BOOLEAN:false");
		queue.put("key", "0:STRING:null");
		queue.put("keylength", "0:INTEGER:null");
		queue.put("searchType", "0:ENUMERATION:EQ");
		queue.put("enable", "0:BOOLEAN:false");
		queue.put("directory", "0:STRING:null");
		return queue;
	}

	@Test
	@SuppressWarnings("HttpUrlsUsage")
	void toolkitFeaturesAndGeneratedMetadataKeepLegacyContract() throws Exception {
		Path toolkit = Path.of("../ISeries");
		Map<String, String> queue = queueProperties();
		for (String node : new String[] { "DataQueueGet", "DataQueueInput", "DataQueueOutput", "ISeriesPcmlCall" }) {
			Map<String, String> expected = new LinkedHashMap<>(queue);
			String category = "dataqueue";
			switch (node) {
				case "DataQueueInput" -> {
					expected.put("queueName", "1:STRING:null");
					expected.put("library", "1:STRING:null");
				}
				case "DataQueueOutput" -> {
					expected.remove("waitInterval");
					expected.remove("searchType");
					expected.put("directory", "0:STRING:");
				}
				case "ISeriesPcmlCall" -> {
					category = "pcml";
					expected.clear();
					expected.put("configurableService", "1:STRING:myISeries");
					expected.put("isXPcml", "0:BOOLEAN:false");
					expected.put("pcmlCall", "0:STRING:null");
					expected.put("libraryList", "0:STRING:null");
					expected.put("ccsid", "0:INTEGER:1208");
					expected.put("toPcml", "0:STRING:transformToPcml.xsl");
					expected.put("toXPcml", "0:STRING:transformToXPcml.xsl");
					expected.put("fromXPcml", "0:STRING:transformFromXPcml.xsl");
					expected.put("enable", "0:BOOLEAN:false");
					expected.put("directory", "0:STRING:null");
				}
				default -> { }
			}
			String relative = "com/ibm/swg/iseries/" + category + "/" + node;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			var features = factory.newDocumentBuilder().parse(toolkit.resolve(relative + ".msgnode").toFile())
					.getElementsByTagName("eStructuralFeatures");
			Map<String, String> actual = new LinkedHashMap<>();
			for (int i = 0; i < features.getLength(); i++) {
				Element feature = (Element) features.item(i);
				Element type = (Element) feature.getElementsByTagName("eType").item(0);
				String kind = type == null ? "ENUMERATION" : switch (type.getAttribute("href")) {
					case "http://www.eclipse.org/emf/2002/Ecore#//EInt" -> "INTEGER";
					case "http://www.eclipse.org/emf/2002/Ecore#//EBoolean" -> "BOOLEAN";
					default -> "STRING";
				};
				actual.put(feature.getAttribute("name"),
						(feature.hasAttribute("lowerBound") ? feature.getAttribute("lowerBound") : "0") + ":"
						+ kind + ":" + (feature.hasAttribute("defaultValueLiteral")
								? feature.getAttribute("defaultValueLiteral") : "null"));
			}
			assertEquals(expected, actual, node);
			String source = Files.readString(toolkit.resolve("src/" + relative + "NodeUDN.java"));
			var properties = Pattern.compile("new NodeProperty\\([^,]+\\.PROPERTY_([A-Z]+),\\s*NodeProperty\\.Usage\\.(MANDATORY|OPTIONAL),\\s*false,\\s*NodeProperty\\.Type\\.([A-Z]+),\\s*(null|\"[^\"]*\")").matcher(source);
			Map<String, String> generated = new LinkedHashMap<>();
			while (properties.find()) {
				generated.put(properties.group(1), (properties.group(2).equals("MANDATORY") ? "1" : "0")
						+ ":" + properties.group(3) + ":" + properties.group(4).replace("\"", ""));
			}
			Map<String, String> constants = new LinkedHashMap<>();
			expected.forEach((name, value) -> constants.put(name.toUpperCase(java.util.Locale.ROOT), value));
			assertEquals(constants, generated, node);
			var labels = new java.util.Properties();
			try (var input = Files.newInputStream(toolkit.resolve(relative + ".properties"))) {
				labels.load(input);
			}
			assertEquals("Connection Policy", labels.getProperty("Property.configurableService"));
		}
	}
}
