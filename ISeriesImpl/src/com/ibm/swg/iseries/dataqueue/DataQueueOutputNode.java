package com.ibm.swg.iseries.dataqueue;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.BaseDataQueue;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputTerminal;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbNodeInterface;
import com.ibm.swg.iseries.RuntimeFailure;
import com.ibm.swg.iseries.DataQueueFlowNode;
import com.ibm.swg.iseries.cache.ConfigurationData;
import com.ibm.swg.iseries.cache.ConnectionCache;
import com.ibm.swg.iseries.communication.BaseDataQueueFactory;
import com.ibm.swg.iseries.communication.ISeriesCall;

public class DataQueueOutputNode extends DataQueueFlowNode implements MbNodeInterface {
	@SuppressWarnings("this-escape")
	public DataQueueOutputNode() throws MbException {
		createInputTerminal("in");
		createOutputTerminal("out");
		createOutputTerminal("failure");
	}

	@Override
	public void evaluate(MbMessageAssembly inAssembly, MbInputTerminal inTerm) throws MbException {
		loadPolicy();
		MbElement environment = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("DataQueue");
		String policy = getPolicyCacheKey();
		AS400 system = null;
		try {
			String requestLibrary = override(environment, "library", library);
			String requestQueue = override(environment, "queueName", queueName);
			String requestType = override(environment, "type", type);
			String requestKey = override(environment, "key", key);
			boolean requestKeyed = parseKeyed(override(environment, "keyed", getKeyed()));
			if (enable) {
				ISeriesCall.enableTrace(buildFileName());
			}
			MbElement body = inAssembly.getMessage().getRootElement().getLastChild();
			if (body == null || "Properties".equals(body.getName())) {
				throw new IllegalArgumentException("Message body is required");
			}
			ConfigurationData config = ConnectionCache.getConfigurationData("UserDefined", policy);
			if (config == null) {
				throw nodeException("evaluate", "Policy configuration not found: " + policy);
			}
			String ccsid = config.get_CCSID();
			system = ConnectionCache.getConnection("UserDefined", policy);
			int targetCcsid = Integer.parseInt(ccsid);
			if (targetCcsid == 0 && system != null) {
				targetCcsid = system.getCcsid();
			}
			byte[] message = body.toBitstream("", "", "", 0, targetCcsid, 0);
			BaseDataQueueFactory factory = BaseDataQueueFactory.getFactory(requestKeyed, null);
			BaseDataQueue queue = factory.getDataQueue(system, requestLibrary, requestQueue, requestType);
			factory.writeMessage(queue, requestKey, message, ccsid);
		} catch (IllegalArgumentException e) {
			throw RuntimeFailure.create("DataQueueOutputNode", "evaluate", "Invalid queue properties", e);
		} finally {
			ConnectionCache.releaseConnection("UserDefined", policy, system);
		}
		getOutputTerminal("out").propagate(inAssembly);
	}

	public static String getNodeName() {
		return "com_ibm_swg_iseries_dataqueue_DataQueueOutputNode";
	}
}
