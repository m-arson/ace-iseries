package com.ibm.swg.iseries.dataqueue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class DataQueueInputNodeTest {
	@Test
	void sourceContractKeepsRecoveryBeforeCleanupAndCommitsInsteadOfRethrowing() throws Exception {
		String source = Files.readString(Path.of("src/com/ibm/swg/iseries/dataqueue/DataQueueInputNode.java"));
		int caught = source.indexOf("} catch (MbException e) {");
		int routed = source.indexOf("failureTerminal.propagate(outAssembly);", caught);
		int recoveryCatch = source.indexOf("} catch (MbException | RuntimeException recoveryFailure)", caught);
		int cleared = source.indexOf("outMessage.clearMessage();");
		assertTrue(caught >= 0 && routed > caught && recoveryCatch > routed && cleared > recoveryCatch);
		String recovery = source.substring(caught, recoveryCatch);
		assertTrue(recovery.contains("if (!failureTerminal.isAttached()) {\n\t\t\t\t\tthrow e;"));
		assertTrue(recovery.contains("outMessage = createMessage(message);"));
		assertTrue(recovery.contains("outAssembly = new MbMessageAssembly(inAssembly, outMessage);"));
		assertTrue(source.substring(routed, recoveryCatch).contains("failure = null;"));
		assertTrue(source.substring(routed, recoveryCatch).contains("return SUCCESS_RETURN;"));
		assertFalse(source.substring(routed, recoveryCatch).contains("throw"));
		String failedRecovery = source.substring(recoveryCatch, source.indexOf("} catch (RuntimeException e)", recoveryCatch));
		assertTrue(failedRecovery.contains("e.addSuppressed(recoveryFailure);"));
		assertTrue(failedRecovery.contains("throw e;"));
		assertFalse(source.contains("| Error"));
		assertFalse(source.contains("catch (Error"));
	}
}
