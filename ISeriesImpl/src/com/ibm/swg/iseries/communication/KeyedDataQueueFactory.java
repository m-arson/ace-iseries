package com.ibm.swg.iseries.communication;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;
import java.util.Set;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.IllegalObjectTypeException;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.cache.DataQueueCache;
import com.ibm.swg.iseries.RuntimeFailure;

public final class KeyedDataQueueFactory extends BaseDataQueueFactory {
	private static final Set<String> SEARCH_TYPES = Set.of("EQ", "NE", "LT", "LE", "GT", "GE");

	public KeyedDataQueueFactory(DataQueueCache cache) { super(cache); }

	@Override
	public BaseDataQueue getDataQueue(AS400 system, String library, String queue, String type) {
		String path = queuePath(system, library, queue, type);
		if (cache != null) {
			BaseDataQueue cached = cache.getDataQueue(system, path);
			if (cached != null) {
				return cached;
			}
		}
		BaseDataQueue dataQueue = new KeyedDataQueue(system, path);
		if (cache != null) {
			cache.setDataQueue(system, path, dataQueue);
		}
		return dataQueue;
	}

	static byte[] keyBytes(String key, int length, String ccsid, AS400 system) throws CharacterCodingException {
		Objects.requireNonNull(key, "Queue key is required.");
		if (length < 1 || length > 256) {
			throw RuntimeFailure.invalidArgument("Queue key length must be between 1 and 256 bytes.");
		}
		int value = parseCcsid(ccsid);
		if (value == 0) {
			value = Objects.requireNonNull(system, "Connection is required.").getCcsid();
			if (value < 1 || value > 65535) {
				throw RuntimeFailure.invalidArgument("Connection CCSID is unresolved.");
			}
		}
		Charset charset = Charset.forName(new AS400Text(1, value).getEncoding());
		ByteBuffer encoded = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT).encode(java.nio.CharBuffer.wrap(key));
		byte[] space = " ".getBytes(charset);
		if (encoded.remaining() > length || (length - encoded.remaining()) % space.length != 0) {
			throw RuntimeFailure.invalidArgument("Queue key does not fit: resolvedCCSID=" + value
					+ "; encodedBytes=" + encoded.remaining() + "; queueKeyBytes=" + length
					+ "; paddingUnitBytes=" + space.length + ".");
		}
		byte[] result = new byte[length];
		int used = encoded.remaining();
		encoded.get(result, 0, used);
		for (int offset = used; offset < length; offset += space.length) {
			System.arraycopy(space, 0, result, offset, space.length);
		}
		return result;
	}

	@Override
	public byte[] getMessage(BaseDataQueue queue, String key, String search, int timeout, String ccsid) throws MbUserException {
		String stage = "timeout/search validation";
		try {
			timeout = boundedTimeout(timeout);
			if (search == null || !SEARCH_TYPES.contains(search)) {
				throw RuntimeFailure.invalidArgument("Invalid keyed queue search type.");
			}
			configure(queue, ccsid);
			KeyedDataQueue keyed = (KeyedDataQueue) queue;
			stage = "key-length attribute lookup";
			int keyLength = keyed.getKeyLength();
			stage = "key encoding; configuredCCSID=" + parseCcsid(ccsid) + "; queueKeyBytes=" + keyLength;
			byte[] encodedKey = keyBytes(key, keyLength, ccsid, keyed.getSystem());
			stage = "keyed read; waitSeconds=" + timeout;
			DataQueueEntry entry = keyed.read(encodedKey, timeout, search);
			return entry == null ? null : entry.getData();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RuntimeFailure.create("KeyedDataQueueFactory", "getMessage", "Data queue " + stage + " failed.", e);
		} catch (AS400SecurityException | ErrorCompletingRequestException | IOException
				| IllegalObjectTypeException | ObjectDoesNotExistException | RuntimeException e) {
			throw RuntimeFailure.create("KeyedDataQueueFactory", "getMessage", "Data queue " + stage + " failed.", e);
		}
	}

	@Override
	public void writeMessage(BaseDataQueue queue, String key, byte[] message, String ccsid) throws MbUserException {
		String stage = "write validation";
		try {
			Objects.requireNonNull(message, "Queue message is required.");
			configure(queue, ccsid);
			KeyedDataQueue keyed = (KeyedDataQueue) queue;
			stage = "key-length attribute lookup";
			int keyLength = keyed.getKeyLength();
			stage = "key encoding; configuredCCSID=" + parseCcsid(ccsid) + "; queueKeyBytes=" + keyLength;
			byte[] encodedKey = keyBytes(key, keyLength, ccsid, keyed.getSystem());
			stage = "keyed write; messageBytes=" + message.length;
			keyed.write(encodedKey, message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RuntimeFailure.create("KeyedDataQueueFactory", "writeMessage", "Data queue " + stage + " failed.", e);
		} catch (AS400SecurityException | ErrorCompletingRequestException | IOException
				| IllegalObjectTypeException | ObjectDoesNotExistException | RuntimeException e) {
			throw RuntimeFailure.create("KeyedDataQueueFactory", "writeMessage", "Data queue " + stage + " failed.", e);
		}
	}
}
