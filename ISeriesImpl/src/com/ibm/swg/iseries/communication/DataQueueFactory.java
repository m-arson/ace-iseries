package com.ibm.swg.iseries.communication;

import java.io.IOException;
import java.util.Objects;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalObjectTypeException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.cache.DataQueueCache;
import com.ibm.swg.iseries.RuntimeFailure;

public final class DataQueueFactory extends BaseDataQueueFactory {
	public DataQueueFactory(DataQueueCache cache) { super(cache); }

	@Override
	public BaseDataQueue getDataQueue(AS400 system, String library, String queue, String type) {
		String path = queuePath(system, library, queue, type);
		if (cache != null) {
			BaseDataQueue cached = cache.getDataQueue(system, path);
			if (cached != null) {
				return cached;
			}
		}
		BaseDataQueue dataQueue = new DataQueue(system, path);
		if (cache != null) {
			cache.setDataQueue(system, path, dataQueue);
		}
		return dataQueue;
	}

	@Override
	public byte[] getMessage(BaseDataQueue queue, String key, String search, int timeout, String ccsid) throws MbUserException {
		String stage = "timeout validation";
		try {
			timeout = boundedTimeout(timeout);
			configure(queue, ccsid);
			stage = "FIFO/LIFO read; waitSeconds=" + timeout;
			DataQueueEntry entry = ((DataQueue) queue).read(timeout);
			return entry == null ? null : entry.getData();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RuntimeFailure.create("DataQueueFactory", "getMessage", "Data queue " + stage + " failed.", e);
		} catch (AS400SecurityException | ErrorCompletingRequestException | IOException
				| IllegalObjectTypeException | ObjectDoesNotExistException | RuntimeException e) {
			throw RuntimeFailure.create("DataQueueFactory", "getMessage", "Data queue " + stage + " failed.", e);
		}
	}

	@Override
	public void writeMessage(BaseDataQueue queue, String key, byte[] message, String ccsid) throws MbUserException {
		String stage = "write validation";
		try {
			Objects.requireNonNull(message, "Queue message is required.");
			configure(queue, ccsid);
			stage = "FIFO/LIFO write; messageBytes=" + message.length;
			((DataQueue) queue).write(message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RuntimeFailure.create("DataQueueFactory", "writeMessage", "Data queue " + stage + " failed.", e);
		} catch (AS400SecurityException | ErrorCompletingRequestException | IOException
				| IllegalObjectTypeException | ObjectDoesNotExistException | RuntimeException e) {
			throw RuntimeFailure.create("DataQueueFactory", "writeMessage", "Data queue " + stage + " failed.", e);
		}
	}
}
