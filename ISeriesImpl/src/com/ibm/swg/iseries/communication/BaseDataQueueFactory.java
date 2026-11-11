package com.ibm.swg.iseries.communication;

import java.beans.PropertyVetoException;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.as400.access.QSYSObjectPathName;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.RuntimeFailure;
import com.ibm.swg.iseries.cache.DataQueueCache;

public abstract class BaseDataQueueFactory {
	protected final DataQueueCache cache;

	protected BaseDataQueueFactory(DataQueueCache cache) {
		this.cache = cache;
	}

	public static BaseDataQueueFactory getFactory(boolean keyed, DataQueueCache cache) {
		return keyed ? new KeyedDataQueueFactory(cache) : new DataQueueFactory(cache);
	}

	protected static String queuePath(AS400 system, String library, String queue, String type) {
		if (system == null || library == null || library.isBlank() || queue == null || queue.isBlank()
				|| !"DTAQ".equalsIgnoreCase(type)) {
			throw RuntimeFailure.invalidArgument("Connection, library, queue name and DTAQ type are required.");
		}
		return new QSYSObjectPathName(library, queue, "DTAQ").getPath();
	}

	protected static int parseCcsid(String value) {
		if (value == null || value.isBlank()) {
			throw RuntimeFailure.invalidArgument("CCSID is required.");
		}
		int ccsid;
		try {
			ccsid = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw RuntimeFailure.invalidArgument("CCSID must be an integer between 0 and 65535.");
		}
		if (ccsid < 0 || ccsid > 65535) {
			throw RuntimeFailure.invalidArgument("CCSID is out of range: CCSID=" + ccsid + ".");
		}
		return ccsid;
	}

	protected static void configure(BaseDataQueue queue, String ccsid) throws MbUserException {
		int value = parseCcsid(ccsid);
		try {
			if (value != 0) {
				java.nio.charset.Charset.forName(new com.ibm.as400.access.AS400Text(1, value).getEncoding());
				queue.setCcsid(value);
			}
		} catch (PropertyVetoException | RuntimeException e) {
			throw RuntimeFailure.create("BaseDataQueueFactory", "configure",
					"Data queue CCSID configuration failed: configuredCCSID=" + value + ".", e);
		}
	}

	public static void validateTimeout(int timeout) {
		if (timeout < -1 || timeout > 30) {
			throw RuntimeFailure.invalidArgument("Queue timeout must be -1 or between 0 and 30 seconds.");
		}
	}

	public static int boundedTimeout(int timeout) {
		validateTimeout(timeout);
		return timeout == -1 ? 30 : timeout;
	}

	protected static MbUserException failure(String method, Exception cause) {
		return RuntimeFailure.create("BaseDataQueueFactory", method, "Data queue " + method + " failed.", cause);
	}

	public abstract BaseDataQueue getDataQueue(AS400 system, String library, String queue, String type);
	public abstract void writeMessage(BaseDataQueue queue, String key, byte[] message, String ccsid) throws MbUserException;
	public abstract byte[] getMessage(BaseDataQueue queue, String key, String search, int timeout, String ccsid) throws MbUserException;
}
