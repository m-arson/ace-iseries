package com.ibm.swg.iseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.ResourceBundle;
import com.ibm.as400.access.AS400Exception;
import com.ibm.as400.access.AS400Message;
import com.ibm.broker.plugin.MbUserException;
import org.junit.jupiter.api.Test;

class RuntimeFailureTest {
	private static AS400Message hostMessage(String id) throws ReflectiveOperationException {
		var constructor = AS400Message.class.getDeclaredConstructor(String.class, String.class);
		constructor.setAccessible(true);
		return constructor.newInstance(id, "password=secret payload=private");
	}

	@Test
	void bundleResolvesStableParameterizedKeyWithoutLeakingExceptionText() throws Exception {
		AS400Exception host = new AS400Exception(new AS400Message[] { hostMessage("CPF9801") });
		IOException cause = new IOException("password=secret payload=private", host);
		MbUserException failure = RuntimeFailure.create("Test", "read", "Data queue read failed.", cause);
		assertSame(cause, failure.getCause());
		assertEquals(FlowNode.FlowNodeMessages.MESSAGE_SOURCE, failure.getMessageSource());
		assertEquals(FlowNode.FlowNodeMessages.Error, failure.getMessageKey());
		assertEquals("%1", ResourceBundle.getBundle(failure.getMessageSource()).getString(failure.getMessageKey()));
		assertEquals(failure.getTraceText(), failure.getInserts()[0]);
		assertTrue(failure.getTraceText().contains("IOException"));
		assertTrue(failure.getTraceText().contains("AS400Exception"));
		assertTrue(failure.getTraceText().contains("CPF9801"));
		assertFalse(failure.getTraceText().contains("secret"));
		assertFalse(failure.getTraceText().contains("private"));
	}

	@Test
	void returnedProgramMessagesIncludeOnlyValidatedIds() throws Exception {
		MbUserException failure = RuntimeFailure.create("ISeriesCall", "callIseriesProgram", "Program call failed.",
				null, new AS400Message[] { hostMessage("CPF9801"), hostMessage("CPF9801"),
						hostMessage("password=secret"), null });
		assertEquals("Program call failed. [CPF9801]", failure.getTraceText());
	}

	@Test
	void returnCodesAndNetworkHintsDoNotExposeArbitraryMessages() {
		var cause = new com.ibm.as400.access.ExtendedIllegalArgumentException("password=secret",
				com.ibm.as400.access.ExtendedIllegalArgumentException.PARAMETER_VALUE_NOT_VALID);
		String text = RuntimeFailure.create("Test", "configure", "Configuration failed.", cause).getTraceText();
		assertTrue(text.contains("returnCode=" + cause.getReturnCode()), text);
		assertFalse(text.contains("secret"), text);
		String timeout = RuntimeFailure.create("Test", "read", "Read failed.",
				new java.net.SocketTimeoutException("payload=private")).getTraceText();
		assertTrue(timeout.contains("Socket timeout"), timeout);
		assertFalse(timeout.contains("private"), timeout);
	}

	@Test
	void interruptionSurvivesWrapping() {
		try {
			RuntimeFailure.create("Test", "read", "Read failed.", new IOException(new InterruptedException()));
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted();
		}
	}
}
