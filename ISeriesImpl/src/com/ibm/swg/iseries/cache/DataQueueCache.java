package com.ibm.swg.iseries.cache;

import java.util.Objects;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;

public final class DataQueueCache {
	public BaseDataQueue getDataQueue(AS400 system, String path) {
		Objects.requireNonNull(system, "System connection is required");
		Objects.requireNonNull(path, "Queue path is required");
		return null;
	}

	public void setDataQueue(AS400 system, String path, BaseDataQueue dataQueue) {
		Objects.requireNonNull(system, "System connection is required");
		Objects.requireNonNull(path, "Queue path is required");
		Objects.requireNonNull(dataQueue, "Data queue is required");
	}
}
