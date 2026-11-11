package com.ibm.broker.plugin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ibm.as400.access.AS400;
import com.ibm.swg.iseries.RuntimeFailure;
import com.ibm.swg.iseries.cache.TemplateCache;
import com.ibm.swg.iseries.communication.ISeriesCall;

class ISeriesCallDiagnosticsTest {
	@TempDir
	Path directory;

	private static class Input extends MbElement {
		private final String xml;

		Input(String xml) {
			super(0L);
			this.xml = xml;
		}

		@Override
		public byte[] toBitstream(String messageType, String messageSet, String messageFormat,
				int encoding, int ccsid, int options) throws MbException {
			return xml.getBytes(StandardCharsets.UTF_8);
		}
	}

	@Test
	void serializationFailureExcludesExceptionText() {
		Input input = new Input("") {
			@Override
			public byte[] toBitstream(String messageType, String messageSet, String messageFormat,
					int encoding, int ccsid, int options) {
				throw new IllegalStateException("password=secret payload=private");
			}
		};
		MbUserException failure = assertThrows(MbUserException.class, () -> ISeriesCall.callIseriesProgram(
				new AS400(), "test", input, new TemplateCache(), null, null, false, 1208));
		assertTrue(failure.getTraceText().startsWith("PCML input serialization failed."));
		assertFalse(failure.getTraceText().contains("secret"));
		assertFalse(failure.getTraceText().contains("private"));
	}

	@Test
	void preservesMbUserExceptionIdentity() {
		MbUserException expected = RuntimeFailure.create("Test", "serialize", "Serialization failed.", null);
		Input input = new Input("") {
			@Override
			public byte[] toBitstream(String messageType, String messageSet, String messageFormat,
					int encoding, int ccsid, int options) throws MbException {
				throw expected;
			}
		};
		assertSame(expected, assertThrows(MbUserException.class, () -> ISeriesCall.callIseriesProgram(
				new AS400(), "test", input, new TemplateCache(), null, null, false, 1208)));
	}

	@Test
	void distinguishesInputTransformationParsingAndTemplateFailures() throws Exception {
		Path stylesheet = Files.writeString(directory.resolve("identity.xsl"), """
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				<xsl:template match="@*|node()"><xsl:copy><xsl:apply-templates select="@*|node()"/></xsl:copy></xsl:template>
				</xsl:stylesheet>
				""");
		TemplateCache cache = new TemplateCache();
		MbUserException transform = assertThrows(MbUserException.class, () -> ISeriesCall.callIseriesProgram(
				new AS400(), "test", new Input("<secret>private"), cache, stylesheet.toString(), null, false, 1208));
		assertTrue(transform.getTraceText().startsWith("PCML input transformation failed."));
		assertFalse(transform.getTraceText().contains("secret"));
		assertFalse(transform.getTraceText().contains("private"));
		String unsafe = "<pcml xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
				+ "xsi:noNamespaceSchemaLocation='file:///secret-private'/>";
		MbUserException parse = assertThrows(MbUserException.class, () -> ISeriesCall.callIseriesProgram(
				new AS400(), "test", new Input(unsafe), cache, stylesheet.toString(), null, false, 1208));
		assertTrue(parse.getTraceText().startsWith("PCML parsing failed."));
		assertFalse(parse.getTraceText().contains("secret"));
		assertFalse(parse.getTraceText().contains("private"));
		MbUserException load = assertThrows(MbUserException.class, () -> ISeriesCall.callIseriesProgram(
				new AS400(), "test", new Input("<value/>"), cache, "secret-missing.xsl", null, false, 1208));
		assertTrue(load.getTraceText().startsWith("Template load failed."));
		assertFalse(load.getTraceText().contains("secret"));
	}
}
