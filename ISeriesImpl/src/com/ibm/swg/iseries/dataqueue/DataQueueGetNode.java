package com.ibm.swg.iseries.dataqueue;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.broker.plugin.MbBLOB;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputTerminal;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbNodeInterface;
import com.ibm.swg.iseries.RuntimeFailure;
import com.ibm.swg.iseries.DataQueueFlowNode;
import com.ibm.swg.iseries.cache.ConfigurationData;
import com.ibm.swg.iseries.cache.ConnectionCache;
import com.ibm.swg.iseries.communication.BaseDataQueueFactory;
import com.ibm.swg.iseries.communication.ISeriesCall;

public class DataQueueGetNode extends DataQueueFlowNode implements MbNodeInterface {
	private int waitInterval = -1;

	@SuppressWarnings("this-escape")
	public DataQueueGetNode() throws MbException {
		createInputTerminal("in");
		createOutputTerminal("out");
		createOutputTerminal("noMessage");
		createOutputTerminal("failure");
	}

	@Override
	public void evaluate(MbMessageAssembly inAssembly, MbInputTerminal inTerm) throws MbException {
		loadPolicy();
		MbElement environment = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("DataQueue");
		AS400 system = null;
		byte[] message;
		String policy = getPolicyCacheKey();
		try {
			String requestLibrary = override(environment, "library", library);
			String requestQueue = override(environment, "queueName", queueName);
			String requestType = override(environment, "type", type);
			String requestKey = override(environment, "key", key);
			String requestSearch = override(environment, "searchType", searchType);
			boolean requestKeyed = parseKeyed(override(environment, "keyed", getKeyed()));
			int timeout = BaseDataQueueFactory.boundedTimeout(
					Integer.parseInt(override(environment, "waitInterval", getWaitInterval())));
			if (enable) {
				ISeriesCall.enableTrace(buildFileName());
			}
			system = ConnectionCache.getConnection("UserDefined", policy);
			BaseDataQueueFactory factory = BaseDataQueueFactory.getFactory(requestKeyed, null);
			BaseDataQueue queue = factory.getDataQueue(system, requestLibrary, requestQueue, requestType);
			ConfigurationData config = ConnectionCache.getConfigurationData("UserDefined", policy);
			if (config == null) {
				throw nodeException("evaluate", "Policy configuration not found: " + policy);
			}
			String ccsid = config.get_CCSID();
			message = factory.getMessage(queue, requestKey, requestSearch, timeout, ccsid);
		} catch (IllegalArgumentException e) {
			throw RuntimeFailure.create("DataQueueGetNode", "evaluate", "Invalid queue properties", e);
		} finally {
			ConnectionCache.releaseConnection("UserDefined", policy, system);
		}
		MbMessage localEnvironment = new MbMessage(inAssembly.getLocalEnvironment());
		Throwable failure = null;
		try {
			replaceResult(localEnvironment.getRootElement(), message);
			MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, localEnvironment,
					inAssembly.getExceptionList(), inAssembly.getMessage());
			getOutputTerminal(message == null ? "noMessage" : "out").propagate(outAssembly);
		} catch (MbException | RuntimeException | Error e) {
			failure = e;
			throw e;
		} finally {
			try {
				localEnvironment.clearMessage();
			} catch (MbException | RuntimeException | Error cleanupFailure) {
				if (failure == null) {
					throw cleanupFailure;
				}
				failure.addSuppressed(cleanupFailure);
			}
		}
	}

	static void replaceResult(MbElement root, byte[] message) throws MbException {
		MbElement element = root.getFirstChild();
		while (element != null) {
			MbElement next = element.getNextSibling();
			if (MbBLOB.ROOT_ELEMENT_NAME.equals(element.getName())) {
				element.delete();
			}
			element = next;
		}
		if (message != null) {
			MbElement parser = root.createElementAsLastChild(MbBLOB.PARSER_NAME);
			parser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "BLOB", message);
		}
	}

	public static String getNodeName() {
		return "com_ibm_swg_iseries_dataqueue_DataQueueGetNode";
	}

	public String getWaitInterval() {
		return Integer.toString(waitInterval);
	}

	public void setWaitInterval(String waitInterval) {
		int value = Integer.parseInt(waitInterval);
		BaseDataQueueFactory.validateTimeout(value);
		this.waitInterval = value;
	}
}
