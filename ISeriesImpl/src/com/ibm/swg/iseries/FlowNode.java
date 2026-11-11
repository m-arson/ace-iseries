package com.ibm.swg.iseries;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ListResourceBundle;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbNode;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.cache.ConnectionCache;

public abstract class FlowNode extends MbNode {
	protected boolean enable = false;
	protected String directory = "";
	protected String policyName = "myISeries";
	protected String policyProject = "IAM7";
	private boolean qualifiedPolicy;
	private String acquiredPolicyKey;
	private boolean tornDown;

	public FlowNode() {
		super();
	}

	public synchronized String getPolicyProject() {
		return policyProject;
	}

	public synchronized void setPolicyProject(String policyProject) {
		String project = policyProject == null || policyProject.isBlank() ? "IAM7" : policyProject;
		checkPolicyChange(effectivePolicyProject(), project);
		this.policyProject = policyProject;
	}

	public synchronized String getPolicy() {
		return policyName;
	}

	public synchronized void setPolicy(String policyName) {
		checkPolicyChange(this.policyName, policyName);
		this.policyName = policyName;
		qualifiedPolicy = false;
	}

	public synchronized String getConfigurableService() {
		return qualifiedPolicy ? getPolicyCacheKey() : policyName;
	}

	public synchronized void setConfigurableService(String configurableService) {
		if (configurableService != null && configurableService.matches("[^{}:\\s]+")) {
			setPolicy(configurableService);
			return;
		}
		if (configurableService == null || !configurableService.matches("\\{[^{}:\\s]+}:[^{}:\\s]+")) {
			throw new IllegalArgumentException("configurableService must be a policy name or {project}:policy");
		}
		int separator = configurableService.indexOf("}:");
		String project = configurableService.substring(1, separator);
		String name = configurableService.substring(separator + 2);
		checkPolicyChange(effectivePolicyProject(), project);
		checkPolicyChange(policyName, name);
		this.policyProject = project;
		this.policyName = name;
		qualifiedPolicy = true;
	}

	private void checkPolicyChange(String current, String replacement) {
		if ((acquiredPolicyKey != null || tornDown) && !Objects.equals(current, replacement)) {
			throw new IllegalStateException("Policy cannot change after initialization");
		}
		if (replacement == null || !replacement.matches("[^{}:\\s]+")) {
			throw new IllegalArgumentException("Invalid policy name or project");
		}
	}

	public synchronized String getPolicyCacheKey() {
		return "{" + effectivePolicyProject() + "}:" + policyName;
	}

	private String effectivePolicyProject() {
		return policyProject == null || policyProject.isBlank() ? "IAM7" : policyProject;
	}

	public String getEnable() {
		return Boolean.toString(enable);
	}

	public void setEnable(String enable) {
		if (!"true".equalsIgnoreCase(enable) && !"false".equalsIgnoreCase(enable)) {
			throw new IllegalArgumentException("enable must be true or false");
		}
		this.enable = Boolean.parseBoolean(enable);
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String traceDirectory) {
		this.directory = traceDirectory;
	}

	@Override
	public void onPreSetupValidation() throws MbException {
		super.onPreSetupValidation();
		getPolicyCacheKey();
	}

	public synchronized void loadPolicy() throws MbException {
		if (tornDown) {
			throw new IllegalStateException("Node has been torn down");
		}
		if (acquiredPolicyKey == null) {
			String key = getPolicyCacheKey();
			ConnectionCache.acquirePolicy("UserDefined", key, readPolicy(key));
			acquiredPolicyKey = key;
		}
	}

	@Override
	public synchronized void onTearDown() throws MbException {
		tornDown = true;
		String key = acquiredPolicyKey;
		acquiredPolicyKey = null;
		try {
			if (key != null) {
				ConnectionCache.releasePolicy("UserDefined", key);
			}
		} finally {
			super.onTearDown();
		}
	}

	protected String buildFileName() {
		String traceDirectory = directory;
		if (traceDirectory == null || traceDirectory.isEmpty()) {
			traceDirectory = System.getProperty("java.io.tmpdir");
		}
		return Path.of(traceDirectory, "Trace_PCML_" + UUID.randomUUID()).toString();
	}

	protected String buildPathToXsl(String fileName) {
		Path path = Path.of(fileName);
		return (path.isAbsolute() ? path : xslDirectory().resolve(path)).normalize().toString();
	}

	protected String buildPathToXsl(String fileName, boolean requestOverride) throws MbUserException {
		try {
			if (fileName == null || fileName.isBlank()) {
				throw new IllegalArgumentException("Stylesheet path is required");
			}
			if (!requestOverride) {
				return buildPathToXsl(fileName);
			}
			Path relative = Path.of(fileName);
			if (relative.isAbsolute() || fileName.indexOf('\\') >= 0 || fileName.indexOf(':') >= 0) {
				throw new IllegalArgumentException("Stylesheet override must be a relative local path");
			}
			for (Path part : relative) {
				if (part.toString().equals("..")) {
					throw new IllegalArgumentException("Stylesheet override cannot traverse directories");
				}
			}
			Path root = xslDirectory().toRealPath();
			Path resolved = root.resolve(relative).toRealPath();
			if (!resolved.startsWith(root)) {
				throw new IllegalArgumentException("Stylesheet override escapes trusted directory");
			}
			return resolved.toString();
		} catch (IOException | IllegalArgumentException e) {
			MbUserException failure = nodeException("buildPathToXsl", "Invalid stylesheet path");
			failure.initCause(e);
			throw failure;
		}
	}

	protected MbUserException nodeException(String method, String text) {
		return RuntimeFailure.create(getClass().getName(), method, text, null);
	}

	private Path xslDirectory() {
		String workPath = System.getProperty("broker.localWorkpath");
		return workPath == null || workPath.isBlank() ? Path.of("").toAbsolutePath()
				: Path.of(workPath, "XSL");
	}

	private Properties readPolicy(String key) throws MbException {
		MbPolicy policy = getPolicy("UserDefined", key);
		if (policy == null) {
			throw nodeException("readPolicy", "Policy not found: " + key);
		}
		Properties properties = new Properties();
		Set<String> names = policy.getPropertyNames();
		for (String name : new String[] { "iSeriesAddress", "iSeriesUser", "iSeriesPassword", "CCSID",
				"maxConnections", "minConnections", "maxUseTimeout" }) {
			String value = names.contains(name) ? policy.getPropertyValueAsString(name) : null;
			if (value != null) {
				properties.setProperty(name, value);
			}
		}
		return properties;
	}

	protected void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage) throws MbException {
		MbElement outRoot = outMessage.getRootElement();
		MbElement header = inMessage.getRootElement().getFirstChild();
		while (header != null && header.getNextSibling() != null) {
			outRoot.addAsLastChild(header.copy());
			header = header.getNextSibling();
		}
	}

	public static class FlowNodeMessages extends ListResourceBundle {
		public static final String MESSAGE_SOURCE = FlowNodeMessages.class.getName();
		public static final String IOEX = "IOEX";
		public static final String Error = "Error";
		private final Object[][] messages = {
				{ IOEX, "An IOException has been thrown by RoutingFileNode: message: %1 stackTrace: %2" },
				{ Error, "%1" } };

		@Override
		public Object[][] getContents() {
			return messages;
		}
	}
}
