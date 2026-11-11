package com.ibm.swg.iseries;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputNode;
import com.ibm.broker.plugin.MbPolicy;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.cache.ConnectionCache;

public abstract class InputNode extends MbInputNode {
	protected boolean enable = false;
	protected String directory = "";
	protected String policyName = "myISeries";
	protected String policyProject = "IAM7";
	private boolean qualifiedPolicy;
	private String acquiredPolicyKey;
	private boolean tornDown;

	public InputNode() throws MbException {
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

	protected MbUserException nodeException(String method, String text) {
		return RuntimeFailure.create(getClass().getName(), method, text, null);
	}
}
