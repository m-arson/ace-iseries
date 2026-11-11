package com.ibm.swg.iseries.communication;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.data.ProgramCallDocument;
import com.ibm.as400.data.XmlException;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.RuntimeFailure;
import com.ibm.swg.iseries.cache.TemplateCache;

public final class ISeriesCall {
	private ISeriesCall() { }

	public static void enableTrace(String fileName) throws MbUserException {
		Objects.requireNonNull(fileName, "Trace file name is required");
		throw failure("enableTrace", "Per-request Toolbox tracing is disabled because trace state is JVM-global.", null);
	}

	public static byte[] callIseriesProgram(AS400 system, String programName, MbElement message,
			TemplateCache cache, String toPCML, String fromPCML, boolean xpcml, int ccsid) throws MbUserException {
		String stage = "Program input validation failed.";
		try {
			Objects.requireNonNull(system, "Connection is required.");
			Objects.requireNonNull(message, "Program input is required.");
			Objects.requireNonNull(cache, "Template cache is required.");
			if (programName == null || programName.isBlank() || ccsid < 0 || ccsid > 65535) {
				throw new IllegalArgumentException("Program name and valid CCSID are required.");
			}
			stage = "PCML input serialization failed.";
			byte[] input = message.toBitstream("", "", "", 0, ccsid, 0);
			stage = "PCML input transformation failed.";
			byte[] transformed = transform(input, cache.getTemplate(toPCML));
			stage = "PCML parsing failed.";
			validateXml(transformed, true);
			ProgramCallDocument document = new ProgramCallDocument(system, programName,
					new ByteArrayInputStream(transformed), null, null,
					xpcml ? ProgramCallDocument.SOURCE_XPCML : ProgramCallDocument.SOURCE_PCML);
			stage = "Program invocation failed.";
			if (!document.callProgram(programName)) {
				throw RuntimeFailure.create("ISeriesCall", "callIseriesProgram", stage,
						null, document.getMessageList(programName));
			}
			stage = "PCML output transformation failed.";
			return transformProgramOutput(document, cache.getTemplate(fromPCML));
		} catch (MbUserException e) {
			throw e;
		} catch (MbException | XmlException | IOException | XMLStreamException
				| TransformerException | RuntimeException e) {
			throw failure("callIseriesProgram", stage, e);
		}
	}

	public static void setProgramCallLibraryList(AS400 system, String[] libraries) throws MbUserException {
		if (libraries == null || libraries.length == 0) {
			return;
		}
		String[] validated = new String[libraries.length];
		for (int i = 0; i < libraries.length; i++) {
			String library = libraries[i];
			if (library == null) {
				throw failure("setProgramCallLibraryList", "Library name must not be null.", null);
			}
			library = library.trim().toUpperCase(Locale.ROOT);
			if (!library.isEmpty() && !library.matches("[A-Z#$@][A-Z0-9_#$@]{0,9}")) {
				throw failure("setProgramCallLibraryList", "Invalid IBM i library name.", null);
			}
			validated[i] = library;
		}
		try {
			CommandCall command = new CommandCall(Objects.requireNonNull(system, "Connection is required."));
			for (String library : validated) {
				if (!library.isEmpty() && !command.run("ADDLIBLE LIB(" + library + ")")) {
					AS400Message[] messages = command.getMessageList();
					boolean duplicate = false;
					for (AS400Message message : messages) {
						if ("CPF2103".equals(message.getID())) {
							duplicate = true;
						} else if (message.getSeverity() >= 20) {
							throw RuntimeFailure.create("ISeriesCall", "setProgramCallLibraryList",
									"Adding library failed.", null, messages);
						}
					}
					if (!duplicate) {
						throw RuntimeFailure.create("ISeriesCall", "setProgramCallLibraryList",
									"Adding library failed.", null, messages);
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw failure("setProgramCallLibraryList", "Library-list operation interrupted.", e);
		} catch (ErrorCompletingRequestException | IOException | PropertyVetoException | AS400SecurityException | RuntimeException e) {
			throw failure("setProgramCallLibraryList", "Library-list operation failed.", e);
		}
	}

	private static XMLInputFactory inputFactory() {
		XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
		factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
			throw new XMLStreamException("External XML resources are forbidden.");
		});
		return factory;
	}

	static void validateXml(byte[] xml, boolean pcml) throws XMLStreamException {
		XMLStreamReader reader = inputFactory().createXMLStreamReader(new ByteArrayInputStream(xml));
		try {
			while (reader.hasNext()) {
				int event = reader.next();
				if (event == XMLStreamConstants.DTD || event == XMLStreamConstants.ENTITY_REFERENCE) {
					throw new XMLStreamException("DTD and entity references are forbidden.");
				}
				if (pcml && event == XMLStreamConstants.START_ELEMENT) {
					for (int i = 0; i < reader.getAttributeCount(); i++) {
						String name = reader.getAttributeLocalName(i);
						if (("schemaLocation".equals(name) || "noNamespaceSchemaLocation".equals(name))
								&& XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(reader.getAttributeNamespace(i))) {
							throw new XMLStreamException("PCML schema locations are forbidden.");
						}
					}
				}
			}
		} finally {
			reader.close();
		}
	}

	static byte[] transformProgramOutput(ProgramCallDocument document, Templates template)
			throws IOException, XmlException, XMLStreamException, TransformerException {
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		document.generateXPCML(generated);
		return transform(generated.toString(Charset.defaultCharset()).getBytes(StandardCharsets.UTF_8), template);
	}

	static byte[] transform(byte[] xml, Templates template) throws XMLStreamException, TransformerException {
		Objects.requireNonNull(template, "XML transformation template is required.");
		validateXml(xml, false);
		XMLStreamReader reader = inputFactory().createXMLStreamReader(new ByteArrayInputStream(xml));
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Transformer transformer = template.newTransformer();
			transformer.setURIResolver((href, base) -> {
				throw new TransformerException("External transformation resources are forbidden.");
			});
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.transform(new StAXSource(reader), new StreamResult(output));
			return output.toByteArray();
		} finally {
			reader.close();
		}
	}

	private static MbUserException failure(String method, String message, Exception cause) {
		return RuntimeFailure.create("ISeriesCall", method, message, cause);
	}
}
