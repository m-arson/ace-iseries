package com.ibm.swg.iseries;

import com.ibm.broker.plugin.MbException;
import com.ibm.swg.iseries.communication.BaseDataQueueFactory;

public abstract class DataQueueInNode extends InputNode {
	protected int waitInterval = 1;
	protected String library;
	protected String queueName;
	protected String type = "DTAQ";
	protected boolean keyed;
	protected String key;
	protected String searchType = "EQ";
	protected int keylength;

	public DataQueueInNode() throws MbException {
		super();
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
		this.keyed = DataQueueFlowNode.parseKeyed(keyed);
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

	public String getWaitInterval() {
		return Integer.toString(waitInterval);
	}

	public void setWaitInterval(String waitInterval) {
		int value = Integer.parseInt(waitInterval);
		BaseDataQueueFactory.validateTimeout(value);
		this.waitInterval = value;
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
