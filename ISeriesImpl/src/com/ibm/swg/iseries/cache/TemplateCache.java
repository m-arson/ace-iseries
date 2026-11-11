package com.ibm.swg.iseries.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.RuntimeFailure;

public final class TemplateCache {
	private final LinkedHashMap<Path, Templates> templates = new LinkedHashMap<>(16, 0.75f, true);

	public synchronized Templates getTemplate(String templatePath) throws MbUserException {
		String stage = "Template load failed.";
		try {
			if (templatePath == null || templatePath.isBlank()) {
				throw new IllegalArgumentException("Template path is required");
			}
			Path path = Path.of(templatePath).toRealPath();
			Templates template = templates.get(path);
			if (template == null) {
				stage = "Template compilation failed.";
				TransformerFactory factory = TransformerFactory.newDefaultInstance();
				factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
				factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
				factory.setURIResolver((href, base) -> {
					throw new TransformerException("External stylesheet resources are disabled");
				});
				stage = "Template load failed.";
				try (InputStream input = Files.newInputStream(path)) {
					stage = "Template compilation failed.";
					template = factory.newTemplates(new StreamSource(input, path.toUri().toString()));
					stage = "Template load failed.";
				}
				if (templates.size() == 64) {
					templates.remove(templates.keySet().iterator().next());
				}
				templates.put(path, template);
			}
			return template;
		} catch (IOException | TransformerConfigurationException | IllegalArgumentException e) {
			throw RuntimeFailure.create("TemplateCache", "getTemplate",
					stage + " Use a local stylesheet without external resources.", e);
		}
	}

	public synchronized void clear() {
		templates.clear();
	}
}
