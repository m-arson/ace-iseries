package com.ibm.swg.iseries;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ReturnCodeException;
import com.ibm.as400.access.ConnectionPoolException;
import com.ibm.as400.data.XmlException;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.swg.iseries.FlowNode.FlowNodeMessages;

public final class RuntimeFailure {
	private RuntimeFailure() { }

	public static IllegalArgumentException invalidArgument(String detail) {
		return new DiagnosticArgumentException(detail);
	}

	private static final class DiagnosticArgumentException extends IllegalArgumentException {
		private static final long serialVersionUID = 1L;

		private DiagnosticArgumentException(String detail) { super(detail); }
	}

	public static MbUserException create(String source, String method, String operation, Exception cause) {
		return create(source, method, operation, cause, null);
	}

	public static MbUserException create(String source, String method, String operation, Exception cause,
			AS400Message[] messages) {
		Set<String> details = new LinkedHashSet<>();
		addMessageIds(details, messages);
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		for (Throwable current = cause; current != null && visited.add(current);) {
			details.add(current.getClass().getSimpleName());
			if (current instanceof DiagnosticArgumentException) {
				details.add(current.getMessage());
			}
			if (current instanceof SocketTimeoutException) {
				details.add("Socket timeout; check host latency and configured socket timeout");
			} else if (current instanceof ConnectException) {
				details.add("Socket connection failed; check host service and firewall");
			} else if (current instanceof UnknownHostException) {
				details.add("Host name resolution failed; check DNS");
			} else if (current instanceof CharacterCodingException) {
				details.add("Text cannot be encoded with selected CCSID");
			} else if (current instanceof UnsupportedCharsetException) {
				details.add("Character encoding unavailable; check CCSID and JVM charset support");
			}
			if (current instanceof ReturnCodeException exception) {
				details.add("returnCode=" + exception.getReturnCode());
			}
			if (current instanceof AS400Exception exception) {
				addMessageIds(details, exception.getAS400MessageList());
			}
			if (current instanceof AS400SecurityException exception) {
				addMessageIds(details, exception.getMessageList());
			}
			if (current instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			Throwable nested = current.getCause();
			if (nested == null && current instanceof ConnectionPoolException exception) {
				nested = exception.getException();
			} else if (nested == null && current instanceof XmlException exception) {
				nested = exception.getException();
			}
			current = nested;
		}
		String text = operation + (details.isEmpty() ? "" : " [" + String.join(", ", details) + "]");
		MbUserException failure = new MbUserException(source, method, FlowNodeMessages.MESSAGE_SOURCE,
				FlowNodeMessages.Error, text, new Object[] { text });
		if (cause != null) {
			failure.initCause(cause);
		}
		return failure;
	}

	private static void addMessageIds(Set<String> details, AS400Message[] messages) {
		if (messages != null) {
			for (AS400Message message : messages) {
				if (message != null && message.getID() != null && message.getID().matches("[A-Z0-9]{7}")) {
					details.add(message.getID());
				}
			}
		}
	}
}
