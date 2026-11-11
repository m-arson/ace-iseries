package com.ibm.swg.iseries.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ibm.broker.plugin.MbUserException;

class TemplateCacheTest {
	@TempDir
	Path directory;

	private static final String STYLESHEET = """
			<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
			<xsl:output method="text"/>
			<xsl:template match="/"><xsl:value-of select="/value"/></xsl:template>
			</xsl:stylesheet>
			""";

	@Test
	void concurrentCallersShareOnlyTemplates() throws Exception {
		Path stylesheet = Files.writeString(directory.resolve("shared.xsl"), STYLESHEET);
		TemplateCache cache = new TemplateCache();
		Templates expected = cache.getTemplate(stylesheet.toString());
		ExecutorService workers = Executors.newFixedThreadPool(16);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Void>> results = new ArrayList<>();
		try {
			for (int index = 0; index < 128; index++) {
				String value = "request-" + index;
				results.add(workers.submit(() -> {
					assertTrue(start.await(10, TimeUnit.SECONDS));
					Templates template = cache.getTemplate(stylesheet.toString());
					assertSame(expected, template);
					StringWriter output = new StringWriter();
					template.newTransformer().transform(new StreamSource(new StringReader("<value>" + value + "</value>")),
							new StreamResult(output));
					assertEquals(value, output.toString());
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
		cache.clear();
		assertNotSame(expected, cache.getTemplate(stylesheet.toString()));
	}

	@Test
	void evictsOldTemplatesAndKeepsHashCollisionsSeparate() throws Exception {
		TemplateCache cache = new TemplateCache();
		Path first = Files.writeString(directory.resolve("Aa.xsl"), STYLESHEET);
		Path second = Files.writeString(directory.resolve("BB.xsl"), STYLESHEET);
		assertEquals(first.toString().hashCode(), second.toString().hashCode());
		Templates original = cache.getTemplate(first.toString());
		assertNotSame(original, cache.getTemplate(second.toString()));
		for (int index = 0; index < 64; index++) {
			Path stylesheet = Files.writeString(directory.resolve(index + ".xsl"), STYLESHEET);
			cache.getTemplate(stylesheet.toString());
		}
		assertNotSame(original, cache.getTemplate(first.toString()));
	}

	@Test
	void failureStagesExcludePathsAndStylesheetDetails() throws Exception {
		TemplateCache cache = new TemplateCache();
		MbUserException load = assertThrows(MbUserException.class,
				() -> cache.getTemplate(directory.resolve("secret-missing.xsl").toString()));
		assertTrue(load.getTraceText().startsWith("Template load failed."));
		assertFalse(load.getTraceText().contains("secret"));
		assertFalse(load.getTraceText().contains(directory.toString()));
		Path invalid = Files.writeString(directory.resolve("secret-invalid.xsl"), "<secret/>");
		MbUserException compile = assertThrows(MbUserException.class, () -> cache.getTemplate(invalid.toString()));
		assertTrue(compile.getTraceText().startsWith("Template compilation failed."));
		assertFalse(compile.getTraceText().contains("secret"));
		assertFalse(compile.getTraceText().contains(directory.toString()));
	}

	@Test
	void rejectsExternalStylesheetsAndDocumentReads() throws Exception {
		TemplateCache cache = new TemplateCache();
		Path secret = Files.writeString(directory.resolve("secret.xml"), "<secret>private</secret>");
		Path include = Files.writeString(directory.resolve("include.xsl"), """
				<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				<xsl:include href="file:///must-not-read.xsl"/>
				</xsl:stylesheet>
				""");
		assertThrows(MbUserException.class, () -> cache.getTemplate(include.toString()));
		Path document = Files.writeString(directory.resolve("document.xsl"), STYLESHEET.replace("/value",
				"document('" + secret.toUri() + "')/secret"));
		Templates template = cache.getTemplate(document.toString());
		assertThrows(TransformerException.class, () -> template.newTransformer().transform(
				new StreamSource(new StringReader("<value/>")), new StreamResult(new StringWriter())));
		assertThrows(MbUserException.class, () -> cache.getTemplate(""));
	}
}
