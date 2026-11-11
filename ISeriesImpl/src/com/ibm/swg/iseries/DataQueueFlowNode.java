package com.ibm.swg.iseries;

import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;

public abstract class DataQueueFlowNode extends FlowNode {
	protected String library;
	protected String queueName;
	protected String type = "DTAQ";
	protected boolean keyed;
	protected String key;
	protected int keylength;
	protected String searchType = "EQ";

	public DataQueueFlowNode() {
		super();
	}

	protected static String override(MbElement environment, String name, String fallback) throws MbException {
		MbElement value = environment == null ? null : environment.getFirstElementByPath(name);
		if (value == null) {
			return fallback;
		}
		Object raw = value.getValue();
		if (raw == null) {
			throw RuntimeFailure.create("DataQueue", "override", "Null override: " + name, null);
		}
		return raw.toString();
	}

	protected static boolean parseKeyed(String value) {
		if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
			throw new IllegalArgumentException("keyed must be true or false");
		}
		return Boolean.parseBoolean(value);
	}

	public String getLibrary() {
		return library;
	}

	public void setLibrary(String library) {
		this.library = library;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getKeyed() {
		return Boolean.toString(keyed);
	}

	public void setKeyed(String keyed) {
		this.keyed = parseKeyed(keyed);
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getKeylength() {
		return Integer.toString(keylength);
	}

	public void setKeylength(String keylength) {
		int value = Integer.parseInt(keylength);
		if (value < 0 || value > 256) {
			throw new IllegalArgumentException("keylength must be between 0 and 256");
		}
		this.keylength = value;
	}
}
