package com.ibm.swg.iseries.communication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import com.ibm.as400.access.AS400;
import com.ibm.as400.data.ProgramCallDocument;
import com.ibm.broker.plugin.MbUserException;

class XmlSecurityTest {
	@Test
	void rejectsDtdEntitiesAndRemoteSchemasBeforeToolboxParsing() {
		String[] unsafe = {
				"<!DOCTYPE pcml [<!ENTITY secret SYSTEM 'file:///etc/passwd'>]><pcml>&secret;</pcml>",
				"<!DOCTYPE pcml SYSTEM 'https://invalid.example/pcml.dtd'><pcml/>",
				"<pcml xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:noNamespaceSchemaLocation='file:///secret'/>",
				"<pcml xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='urn:test https://invalid.example/schema'/>"
		};
		for (String xml : unsafe) {
			assertThrows(XMLStreamException.class, () -> ISeriesCall.validateXml(xml.getBytes(StandardCharsets.UTF_8), true));
		}
	}

	@Test
	void transformsUnicodeWithoutPlatformDefaultEncoding() throws Exception {
		Templates template = TransformerFactory.newDefaultInstance().newTemplates(new StreamSource(new StringReader("""
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				<xsl:output method="text"/><xsl:template match="/"><xsl:value-of select="/value"/></xsl:template>
				</xsl:stylesheet>
				""")));
		String value = "Tiếng Việt 日本語";
		byte[] output = ISeriesCall.transform(("<value>" + value + "</value>").getBytes(StandardCharsets.UTF_8), template);
		assertEquals(value, new String(output, StandardCharsets.UTF_8));
	}

	@Test
	void transformsToolboxOutputWithNonUtf8DefaultCharset() throws Exception {
		Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
				"-Dfile.encoding=windows-1252", "-cp", System.getProperty("java.class.path"),
				XmlSecurityTest.class.getName()).redirectErrorStream(true).start();
		try {
			assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Encoding regression JVM timed out");
			assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
		} finally {
			process.destroyForcibly();
		}
	}

	public static void main(String[] args) throws Exception {
		Charset charset = Charset.defaultCharset();
		assertEquals(Charset.forName("windows-1252"), charset);
		String pcml = "<pcml version='4.0'><program name='test' path='/QSYS.LIB/TEST.PGM'>"
				+ "<data name='value' type='char' length='32' usage='inputoutput'/></program></pcml>";
		ProgramCallDocument document = new ProgramCallDocument(new AS400(), "test",
				new ByteArrayInputStream(pcml.getBytes(StandardCharsets.UTF_8)), null, null,
				ProgramCallDocument.SOURCE_PCML);
		String value = "café €";
		document.setValue("test.value", value);
		Templates template = TransformerFactory.newDefaultInstance().newTemplates(new StreamSource(new StringReader("""
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				<xsl:output method="text"/><xsl:template match="/"><xsl:value-of select="/xpcml/program/parameterList/stringParm"/></xsl:template>
				</xsl:stylesheet>
				""")));
		ByteArrayOutputStream generated = new ByteArrayOutputStream();
		document.generateXPCML(generated);
		assertThrows(XMLStreamException.class, () -> ISeriesCall.transform(generated.toByteArray(), template));
		assertEquals(value, new String(ISeriesCall.transformProgramOutput(document, template), StandardCharsets.UTF_8));
		assertEquals(charset, Charset.defaultCharset());
	}

	@Test
	void rejectsCommandInjectionBeforeAccessingConnection() {
		assertThrows(MbUserException.class,
				() -> ISeriesCall.setProgramCallLibraryList(null, new String[] { "LIB) CMD(DLTLIB LIB(X))" }));
		assertThrows(MbUserException.class, () -> ISeriesCall.enableTrace("unused"));
	}
}
