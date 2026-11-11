package com.ibm.swg.iseries.dataqueue;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputNodeInterface;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.swg.iseries.DataQueueInNode;
import com.ibm.swg.iseries.cache.ConfigurationData;
import com.ibm.swg.iseries.cache.ConnectionCache;
import com.ibm.swg.iseries.communication.BaseDataQueueFactory;
import com.ibm.swg.iseries.communication.ISeriesCall;

public class DataQueueInputNode extends DataQueueInNode implements MbInputNodeInterface {
	@SuppressWarnings("this-escape")
	public DataQueueInputNode() throws MbException {
		createOutputTerminal("out");
		createOutputTerminal("failure");
		setAttribute("firstParserClassName", "BLOB");
	}

	public static String getNodeName() {
		return "com_ibm_swg_iseries_dataqueue_DataQueueInputNode";
	}

	@Override
	public int run(MbMessageAssembly inAssembly) throws MbException {
		loadPolicy();
		MbOutputTerminal out = getOutputTerminal("out");
		if (!out.isAttached()) {
			return FAILURE_RETURN;
		}
		String policy = getPolicyCacheKey();
		AS400 system = null;
		byte[] message;
		int ccsid;
		try {
			if (enable) {
				ISeriesCall.enableTrace(buildFileName());
			}
			system = ConnectionCache.getConnection("UserDefined", policy);
			BaseDataQueueFactory factory = BaseDataQueueFactory.getFactory(keyed, null);
			BaseDataQueue queue = factory.getDataQueue(system, library, queueName, type);
			ConfigurationData config = ConnectionCache.getConfigurationData("UserDefined", policy);
			if (config == null) {
				throw nodeException("run", "Policy configuration not found: " + policy);
			}
			String configuredCcsid = config.get_CCSID();
			ccsid = Integer.parseInt(configuredCcsid);
			if (ccsid == 0) {
				ccsid = system.getCcsid();
			}
			message = factory.getMessage(queue, key, searchType, waitInterval, configuredCcsid);
		} finally {
			ConnectionCache.releaseConnection("UserDefined", policy, system);
		}
		if (message == null) {
			return TIMEOUT;
		}
		MbMessage outMessage = null;
		MbMessageAssembly outAssembly = null;
		Throwable failure = null;
		try {
			outMessage = createMessage(message);
			MbElement properties = outMessage.getRootElement().getFirstChild();
			MbElement codedCharSetId = properties.getFirstElementByPath("CodedCharSetId");
			MbElement encoding = properties.getFirstElementByPath("Encoding");
			if (codedCharSetId != null) {
				codedCharSetId.setValue(ccsid);
			}
			if (encoding != null) {
				encoding.setValue(0);
			}
			outMessage.finalizeMessage(MbMessage.FINALIZE_VALIDATE);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			dispatchThread();
			out.propagate(outAssembly);
			return SUCCESS_RETURN;
		} catch (MbException e) {
			failure = e;
			try {
				MbOutputTerminal failureTerminal = getOutputTerminal("failure");
				if (!failureTerminal.isAttached()) {
					throw e;
				}
				if (outMessage == null) {
					outMessage = createMessage(message);
				}
				if (outAssembly == null) {
					outAssembly = new MbMessageAssembly(inAssembly, outMessage);
				}
				failureTerminal.propagate(outAssembly);
				failure = null;
				return SUCCESS_RETURN;
			} catch (MbException | RuntimeException recoveryFailure) {
				if (recoveryFailure != e) {
					e.addSuppressed(recoveryFailure);
				}
				throw e;
			}
		} catch (RuntimeException e) {
			failure = e;
			throw e;
		} finally {
			if (outMessage != null) {
				try {
					outMessage.clearMessage();
				} catch (MbException | RuntimeException cleanupFailure) {
					if (failure == null) {
						throw cleanupFailure;
					}
					if (cleanupFailure != failure) {
						failure.addSuppressed(cleanupFailure);
					}
				}
			}
		}
	}
}
