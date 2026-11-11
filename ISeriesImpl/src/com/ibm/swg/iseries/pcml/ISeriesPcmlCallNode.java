package com.ibm.swg.iseries.pcml;

import com.ibm.as400.access.AS400;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbInputTerminal;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbNodeInterface;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbXMLNSC;
import com.ibm.swg.iseries.FlowNode;
import com.ibm.swg.iseries.cache.ConnectionCache;
import com.ibm.swg.iseries.cache.TemplateCache;
import com.ibm.swg.iseries.communication.ISeriesCall;

public class ISeriesPcmlCallNode extends FlowNode implements MbNodeInterface {
	private String pcmlCall;
	private boolean isXPcml;
	private String libraryList;
	private String toPcml = "transformToPcml.xsl";
	private String toXPcml = "transformToXPcml.xsl";
	private String fromXPcml = "transformFromXPcml.xsl";
	private int ccsid = 1208;
	private final TemplateCache templateCache = new TemplateCache();

	@SuppressWarnings("this-escape")
	public ISeriesPcmlCallNode() throws MbException {
		createInputTerminal("in");
		createOutputTerminal("out");
		createOutputTerminal("failure");
	}

	@Override
	public void evaluate(MbMessageAssembly inAssembly, MbInputTerminal inTerm) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbMessage inMessage = inAssembly.getMessage();
		MbMessage outMessage = new MbMessage();
		Throwable failure = null;
		try {
			copyMessageHeaders(inMessage, outMessage);
			MbMessage localEnvironment = inAssembly.getLocalEnvironment();
			MbElement overrides = localEnvironment == null ? null
					: localEnvironment.getRootElement().getFirstElementByPath("ISeriesPcmlCall");
			String requestCall = property(overrides, "pcmlCall", pcmlCall);
			String requestXPcmlValue = property(overrides, "isXPcml", Boolean.toString(isXPcml));
			if (requestCall == null || requestCall.isBlank()) {
				throw nodeException("evaluate", "pcmlCall is required");
			}
			if (!requestXPcmlValue.equalsIgnoreCase("true") && !requestXPcmlValue.equalsIgnoreCase("false")) {
				throw nodeException("evaluate", "isXPcml must be true or false");
			}
			boolean requestXPcml = Boolean.parseBoolean(requestXPcmlValue);
			String requestLibraries = property(overrides, "libraryList", libraryList);
			int requestCcsid = requestCcsid(property(overrides, "ccsid", Integer.toString(ccsid)));
			String toStyleSheet = stylesheet(overrides, requestXPcml ? "toXPcml" : "toPcml",
					requestXPcml ? toXPcml : toPcml);
			String fromStyleSheet = stylesheet(overrides, "fromXPcml", fromXPcml);
			MbElement body = inMessage.getRootElement().getLastChild();
			if (body == null) {
				throw nodeException("evaluate", "Message body is required");
			}
			if (enable) {
				throw nodeException("evaluate", "Per-request JTOpen tracing is not supported");
			}
			loadPolicy();
			String key = getPolicyCacheKey();
			AS400 system = ConnectionCache.getConnection("UserDefined", key);
			byte[] result;
			try {
				if (system == null) {
					throw nodeException("evaluate", "Connection unavailable");
				}
				ISeriesCall.setProgramCallLibraryList(system, splitLibraries(requestLibraries));
				result = ISeriesCall.callIseriesProgram(system, requestCall, body, templateCache,
						toStyleSheet, fromStyleSheet, requestXPcml, requestCcsid);
			} finally {
				try {
					if (system != null) {
						system.disconnectService(AS400.COMMAND);
					}
				} finally {
					ConnectionCache.releaseConnection("UserDefined", key, system);
				}
			}
			MbElement outRoot = outMessage.getRootElement();
			MbElement properties = outRoot.getFirstElementByPath("Properties");
			if (properties == null) {
				properties = outRoot.createElementAsFirstChild("Properties");
			}
			MbElement outputCcsid = properties.getFirstElementByPath("CodedCharSetId");
			if (outputCcsid == null) {
				properties.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "CodedCharSetId", 1208);
			} else {
				outputCcsid.setValue(1208);
			}
			outRoot.createElementAsLastChildFromBitstream(result,
					MbXMLNSC.PARSER_NAME, "", "", "", 0, 1208, 0);
			MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			out.propagate(outAssembly);
		} catch (MbException | RuntimeException | Error e) {
			failure = e;
			throw e;
		} finally {
			try {
				outMessage.clearMessage();
			} catch (MbException | RuntimeException | Error cleanupFailure) {
				if (failure == null) {
					throw cleanupFailure;
				}
				failure.addSuppressed(cleanupFailure);
			}
		}
	}

	private int requestCcsid(String value) throws MbException {
		try {
			return value == null ? ccsid : validatedCcsid(Integer.parseInt(value));
		} catch (IllegalArgumentException e) {
			throw nodeException("requestCcsid", "ccsid must be an integer between 0 and 65535");
		}
	}

	private static int validatedCcsid(int value) {
		if (value < 0 || value > 65535) {
			throw new IllegalArgumentException("ccsid must be between 0 and 65535");
		}
		return value;
	}

	public int getCcsid() {
		return ccsid;
	}

	public void setCcsid(int ccsid) {
		this.ccsid = validatedCcsid(ccsid);
	}

	private String property(MbElement overrides, String name, String fallback) throws MbException {
		MbElement value = overrides == null ? null : overrides.getFirstElementByPath(name);
		if (value == null) {
			return fallback;
		}
		String result = value.getValueAsString();
		if (result == null) {
			throw nodeException("property", "Null override: " + name);
		}
		return result;
	}

	private String stylesheet(MbElement overrides, String name, String configured) throws MbException {
		MbElement value = overrides == null ? null : overrides.getFirstElementByPath(name);
		return buildPathToXsl(value == null ? configured : value.getValueAsString(), value != null);
	}

	private String[] splitLibraries(String libraries) {
		return libraries == null ? null : libraries.split(",");
	}

	@Override
	public void onPreSetupValidation() throws MbException {
		super.onPreSetupValidation();
		String toSheet = isXPcml ? toXPcml : toPcml;
		if (toSheet == null || toSheet.isBlank()) {
			throw nodeException("onPreSetupValidation", (isXPcml ? "toXPcml" : "toPcml") + " is required");
		}
		if (fromXPcml == null || fromXPcml.isBlank()) {
			throw nodeException("onPreSetupValidation", "fromXPcml is required");
		}
	}

	@Override
	public void onTearDown() throws MbException {
		try {
			templateCache.clear();
		} finally {
			super.onTearDown();
		}
	}

	public static String getNodeName() {
		return "com_ibm_swg_iseries_pcml_ISeriesPcmlCallNode";
	}

	public String getPcmlCall() {
		return pcmlCall;
	}

	public void setPcmlCall(String pcmlCall) {
		this.pcmlCall = pcmlCall;
	}

	public String getIsXPcml() {
		return Boolean.toString(isXPcml);
	}

	public void setIsXPcml(String isXPcml) {
		if (!"true".equalsIgnoreCase(isXPcml) && !"false".equalsIgnoreCase(isXPcml)) {
			throw new IllegalArgumentException("isXPcml must be true or false");
		}
		this.isXPcml = Boolean.parseBoolean(isXPcml);
	}

	public String getLibraryList() {
		return libraryList;
	}

	public String[] getLibraryListAsArray() {
		return splitLibraries(libraryList);
	}

	public void setLibraryList(String libraryList) {
		this.libraryList = libraryList;
	}

	public String getToPcml() {
		return toPcml;
	}

	public void setToPcml(String toPcml) {
		this.toPcml = toPcml;
	}

	public String getToXPcml() {
		return toXPcml;
	}

	public void setToXPcml(String toXPcml) {
		this.toXPcml = toXPcml;
	}

	public String getFromXPcml() {
		return fromXPcml;
	}

	public void setFromXPcml(String fromXPcml) {
		this.fromXPcml = fromXPcml;
	}

}
